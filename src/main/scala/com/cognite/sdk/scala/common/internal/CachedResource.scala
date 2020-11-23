// Copyright 2020 Gavin Bisesi
// SPDX-License-Identifier: MIT
// From https://gist.github.com/Daenyth/28243952f1fcfac6e8ef838040e8638e/9167a51f41322c53de492186c7bfab609fe78f8d

package com.cognite.sdk.scala.common.internal

import cats.effect.concurrent.{Deferred, Ref}
import cats.effect.implicits._
import cats.effect.{Bracket, Concurrent, ExitCase, Resource, Sync, SyncIO}
import cats.implicits._

import scala.util.control.NonFatal

trait CachedResource[F[_], R] extends CachedResource.Runner[F, R] {

  /** Invalidates any current instance of `R`, guaranteeing that ??? */
  def invalidate: F[Unit]

  /** Run `f` with an instance of `R`, possibly allocating a new one, or possibly reusing an existing one.
    *  Guarantees that `R` will not be invalidated until `f` returns
    */
  def run[A](f: R => F[A]): F[A]

  /** Invalidate if `shouldRefresh` returns true, otherwise do nothing */
  def invalidateIfNeeded(shouldInvalidate: R => Boolean): F[Unit]
}

object CachedResource {

  /** Run `f` with `get`, and if `f` fails and `shouldInvalidate` returns `true`
    *
    * @param shouldInvalidate If true, invalidate. If false or not defined, do not invalidate.
    *                         Default: always invalidate (assuming NonFatal)
    */
  def runAndInvalidateOnError[F[_], R, A](cr: CachedResource[F, R])(
      f: R => F[A],
      shouldInvalidate: PartialFunction[Throwable, Boolean] = { case NonFatal(_) =>
        true
      }
  )(implicit F: Bracket[F, Throwable]): F[A] =
    cr.run(f).guaranteeCase {
      case ExitCase.Completed | ExitCase.Canceled =>
        F.unit
      case ExitCase.Error(e) =>
        val willInvalidate = shouldInvalidate.lift(e).getOrElse(false)
        F.whenA(willInvalidate)(cr.invalidate)
    }

  /** Runner that checks if refresh is needed before each `run` call, and additionally can invalidate on errors */
  def runner[F[_], R, A](cr: CachedResource[F, R])(
      shouldRefresh: R => Boolean,
      shouldInvalidate: PartialFunction[Throwable, Boolean] = { case NonFatal(_) =>
        true
      }
  )(
      implicit F: Bracket[F, Throwable]
  ): Runner[F, R] = new Runner[F, R] {
    override def run[B](f: R => F[B]): F[B] =
      for {
        _ <- cr.invalidateIfNeeded(shouldRefresh)
        b <- cr.run(f).guaranteeCase {
          case ExitCase.Completed | ExitCase.Canceled =>
            F.unit
          case ExitCase.Error(e) =>
            val willInvalidate = shouldInvalidate.lift(e).getOrElse(false)
            F.whenA(willInvalidate)(cr.invalidate)
        }
      } yield b
  }

  // NB Runner is exactly the `Codensity` typeclass from haskell
  trait Runner[F[_], A] {
    def run[B](f: A => F[B]): F[B]
  }
}

object SyncCachedResource {

  def apply[R](resource: Resource[SyncIO, R]): SyncIO[SyncCachedResource[R]] =
    SyncIO(new SyncCachedResource(resource))
}

/** Non-concurrent-safe but simple CachedResource implementation */
class SyncCachedResource[R] private (resource: Resource[SyncIO, R])
    extends CachedResource[SyncIO, R] {
  // Either empty, or allocated
  private type State = Option[(R, SyncIO[Unit])]

  override def invalidate: SyncIO[Unit] = transition[Unit] {
    case None => None -> unit
    case Some((_, release)) =>
      None -> release
  }

  override def run[A](f: R => SyncIO[A]): SyncIO[A] = transition[A] {
    case None =>
      empty -> resource.allocated.bracketCase { case res @ (r, _) =>
        cache.set(Some(res)) >> f(r)
      } {
        case ((_, release), ExitCase.Canceled) => release
        case _ => unit
      }
    case s @ Some((r, _)) => s -> f(r)
  }

  override def invalidateIfNeeded(shouldInvalidate: R => Boolean): SyncIO[Unit] = transition[Unit] {
    case s @ Some((r, release)) =>
      if (shouldInvalidate(r)) { empty -> release }
      else { s -> unit }
    case None => empty -> unit
  }

  private def transition[A](f: State => (State, SyncIO[A])): SyncIO[A] =
    cache.modify(f).flatten

  private val empty = Option.empty[(R, SyncIO[Unit])]

  private val unit = SyncIO.unit
  private val cache: Ref[SyncIO, Option[(R, SyncIO[Unit])]] =
    Ref.unsafe[SyncIO, Option[(R, SyncIO[Unit])]](None)
}

object ConcurrentCachedResource {

  def apply[F[_]: Concurrent, R](resource: Resource[F, R]): F[ConcurrentCachedResource[F, R]] =
    Sync[F].delay(new ConcurrentCachedResource(resource))
}

// private ctor because of Ref.unsafe in class body, `new` needs `F.delay` around it
class ConcurrentCachedResource[F[_], R] private (resource: Resource[F, R])(
    implicit F: Concurrent[F]
) extends CachedResource[F, R] {

  /** Resource state */
  private sealed trait RState
  private type Gate = Deferred[F, Unit]

  private case object Empty extends RState
  private case class Ready(r: R, release: F[Unit], running: Int, pendingInvalidation: Option[Gate])
      extends RState
  private case class Allocating(gate: Gate) extends RState
  private case class Invalidating(gate: Gate) extends RState

  override def invalidate: F[Unit] = transition[Unit] {
    case s @ Ready(_, release, running, pendingInvalidation) =>
      running match {
        case 0 =>
          val gate = pendingInvalidation.getOrElse(newGate())
          Invalidating(gate) -> runRelease(release, gate)
        case _ =>
          // Jobs in flight - they need to clean up
          pendingInvalidation match {
            case Some(_) =>
              s -> F.unit // could getOrElse for shorter code but prefer to avoid the allocation
            case None =>
              s.copy(pendingInvalidation = Some(newGate())) -> F.unit
          }
      }

    case s @ Invalidating(gate) =>
      // We only enter this state when jobs are in-flight - just wait on them to finish us
      s -> gate.get

    case Empty =>
      Empty -> F.unit

    case s @ Allocating(gate) =>
      // Preserve invariant that `run >> invalidate >> run` acquires resource twice
      s -> (gate.get *> invalidate)
  }

  override def run[A](f: R => F[A]): F[A] = transition[A] {
    case s @ Ready(r, _, running, None) =>
      s.copy(running = running + 1) -> f(r).guarantee(runCompleted)

    case s @ Ready(_, _, _, Some(gate)) =>
      s -> (gate.get >> run(f))

    case Empty =>
      val gate = newGate()
      Allocating(gate) -> (runAllocate(gate) >> run(f))

    case s @ Allocating(gate) =>
      s -> (gate.get >> run(f))

    case s @ Invalidating(gate) =>
      s -> (gate.get >> run(f))

  }

  private def runCompleted: F[Unit] = transition[Unit] {
    case s @ Ready(_, _, running, pendingInvalidation) =>
      val stillRunning = running - 1
      val action = F.whenA(stillRunning == 0) {
        // Our job to trigger the cleanup - `uncancelable` because if that final invalidation gets cancelled, then
        // nothing will ever `complete` the `pendingInvalidation` gate, and the whole thing deadlocks
        pendingInvalidation.traverse_(_ => invalidate.uncancelable)
      }
      s.copy(running = stillRunning) -> action

    case other =>
      other -> F.raiseError(
        new IllegalStateException(
          s"Tried to complete run when state was $other. This means there is an implementation error in ${this.getClass.getCanonicalName}"
        )
      )
  }

  // Must only be called at the right time, otherwise we could close a resource currently in-use in a `run` call
  private def runRelease(release: F[Unit], gate: Gate): F[Unit] =
    (release >> cache.set(Empty) >> gate.complete(())).uncancelable

  override def invalidateIfNeeded(shouldInvalidate: R => Boolean): F[Unit] =
    transition[Unit] {
      case s @ Ready(r, _, _, None) =>
        s -> F.whenA(shouldInvalidate(r))(invalidate)
      case other => other -> F.unit
    }

  private def runAllocate(gate: Gate): F[Unit] =
    // bracketCase is needed here; allocated.flatMap isn't safe with cancellation
    resource.allocated
      .bracketCase { case (r, release) =>
        (cache.set(Ready(r, release, 0, None)) *> gate.complete(())).uncancelable
      } {
        case ((_, release), ExitCase.Canceled) =>
          // Cancelled between `allocated` and `bracketCase(use)`
          runRelease(release, gate)
        case _ => F.unit
      }
      .onError { case NonFatal(_) =>
        // On allocation error, reset the cache and notify anyone that started waiting while we were in `Allocating`
        runRelease(F.unit, gate)
      }

  // Using `unsafe` just so that I can have RState be an inner type, to avoid useless type parameters on RState
  private val cache = Ref.unsafe[F, RState](Empty)

  // empty parens to disambiguate the overload
  private def transition[A](f: RState => (RState, F[A])): F[A] =
    cache.modify(f).flatten

  // `unsafe` because the effect being run is just a `new AtomicRef...`, and most cases where we might need it,
  // we don't need it, so don't force `flatMap` to get it ready "just in case"
  private def newGate(): Gate = Deferred.unsafe[F, Unit]
}

object ConcurrentCachedObject {

  def apply[F[_]: Concurrent, R](acquire: F[R]): F[CachedResource[F, R]] =
    Sync[F].delay(new ConcurrentCachedObject[F, R](acquire))
}

// private ctor because of Ref.unsafe in class body, `new` needs `F.delay` around it
class ConcurrentCachedObject[F[_], R] private (acquire: F[R])(
    implicit F: Concurrent[F]
) extends CachedResource[F, R] {

  /** Resource state */
  private sealed trait RState
  private type Gate = Deferred[F, Unit]

  private case object Empty extends RState
  private case class Ready(r: R) extends RState
  private case class Pending(gate: Gate) extends RState

  override def invalidate: F[Unit] =
    transition[Unit](_ => Empty -> F.unit)

  override def run[A](f: R => F[A]): F[A] = transition[A] {
    case s @ Ready(r) =>
      s -> f(r)

    case Empty =>
      val gate = newGate()
      Pending(gate) -> (runAcquire(gate) >> run(f))

    case s @ Pending(gate) =>
      s -> (gate.get >> run(f))

  }

  override def invalidateIfNeeded(shouldInvalidate: R => Boolean): F[Unit] =
    transition[Unit] {
      case s @ Ready(r) =>
        s -> F.whenA(shouldInvalidate(r))(invalidate)
      case other =>
        other -> F.unit
    }

  private def runAcquire(gate: Gate): F[Unit] =
    acquire
      .flatMap { r =>
        (cache.set(Ready(r)) *> gate.complete(())).uncancelable
      }
      .onError { case NonFatal(_) =>
        // On allocation error, reset the cache and notify anyone that started waiting while we were in `Allocating`
        setEmpty(gate)
      }

  private def setEmpty(gate: Gate): F[Unit] =
    (cache.set(Empty) >> gate.complete(())).uncancelable

  // Using `unsafe` just so that I can have RState be an inner type, to avoid useless type parameters on RState
  private val cache = Ref.unsafe[F, RState](Empty)

  // empty parens to disambiguate the overload
  private def transition[A](f: RState => (RState, F[A])): F[A] =
    cache.modify(f).flatten

  // `unsafe` because the effect being run is just a `new AtomicRef...`, and most cases where we might need it,
  // we don't need it, so don't force `flatMap` to get it ready "just in case"
  private def newGate(): Gate = Deferred.unsafe[F, Unit]
}
