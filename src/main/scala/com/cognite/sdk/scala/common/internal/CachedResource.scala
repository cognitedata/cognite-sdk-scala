// Copyright 2020 Gavin Bisesi
// SPDX-License-Identifier: MIT
// From https://gist.github.com/Daenyth/28243952f1fcfac6e8ef838040e8638e/9167a51f41322c53de492186c7bfab609fe78f8d

package com.cognite.sdk.scala.common.internal

import cats.effect.implicits._
import cats.effect.{Async, Deferred, MonadCancel, Outcome, Ref}
import cats.implicits._

import scala.util.control.NonFatal

trait CachedResource[F[_], R] extends CachedResource.Runner[F, R] {

  /** Invalidates any current instance of `R`, guaranteeing that ??? */
  def invalidate: F[Unit]

  /** Run `f` with an instance of `R`, possibly allocating a new one, or possibly reusing an
    * existing one. Guarantees that `R` will not be invalidated until `f` returns
    */
  def run[A](f: R => F[A]): F[A]

  /** Invalidate if `shouldRefresh` returns true, otherwise do nothing */
  def invalidateIfNeeded(shouldInvalidate: R => Boolean): F[Unit]
}

object CachedResource {

  /** Run `f` with `get`, and if `f` fails and `shouldInvalidate` returns `true`
    *
    * @param shouldInvalidate
    *   If true, invalidate. If false or not defined, do not invalidate. Default: always invalidate
    *   (assuming NonFatal)
    */
  def runAndInvalidateOnError[F[_], R, A](cr: CachedResource[F, R])(
      f: R => F[A],
      shouldInvalidate: PartialFunction[Throwable, Boolean] = { case NonFatal(_) =>
        true
      }
  )(implicit F: MonadCancel[F, Throwable]): F[A] =
    cr.run(f).guaranteeCase {
      case Outcome.Succeeded(_) | Outcome.Canceled() =>
        F.unit
      case Outcome.Errored(e) =>
        val willInvalidate = shouldInvalidate.lift(e).getOrElse(false)
        F.whenA(willInvalidate)(cr.invalidate)
    }

  /** Runner that checks if refresh is needed before each `run` call, and additionally can
    * invalidate on errors
    */
  def runner[F[_], R, A](cr: CachedResource[F, R])(
      shouldRefresh: R => Boolean,
      shouldInvalidate: PartialFunction[Throwable, Boolean] = { case NonFatal(_) =>
        true
      }
  )(
      implicit F: MonadCancel[F, Throwable]
  ): Runner[F, R] = new Runner[F, R] {
    override def run[B](f: R => F[B]): F[B] =
      for {
        _ <- cr.invalidateIfNeeded(shouldRefresh)
        b <- cr.run(f).guaranteeCase {
          case Outcome.Succeeded(_) | Outcome.Canceled() =>
            F.unit
          case Outcome.Errored(e) =>
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

object ConcurrentCachedObject {
  def apply[F[_]: Async, R](acquire: F[R], init: Option[R] = None): F[CachedResource[F, R]] =
    Async[F]
      .delay(new ConcurrentCachedObject[F, R](acquire))
      .flatTap(res => init.map(res.initWith).getOrElse(Async[F].unit))
      .map(x => x: CachedResource[F, R])
}

// private ctor because of Ref.unsafe in class body, `new` needs `F.delay` around it
class ConcurrentCachedObject[F[_], R] private (acquire: F[R])(
    implicit F: Async[F]
) extends CachedResource[F, R] {

  /** Resource state */
  private trait RState
  private type Gate = Deferred[F, Unit]

  private case object Empty extends RState
  private sealed case class Ready(r: R) extends RState
  private sealed case class Pending(gate: Gate) extends RState

  override def invalidate: F[Unit] =
    transition[Unit](_ => Empty -> F.unit)

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  override def run[A](f: R => F[A]): F[A] = transition[A] {
    case s @ Ready(r) =>
      s -> f(r)

    case Empty =>
      val gate = newGate()
      Pending(gate) -> (runAcquire(gate) >> run(f))

    case s @ Pending(gate) =>
      s -> (gate.get >> run(f))

    case _ => throw new RuntimeException("Unexpected state")
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
        (cache.set(Ready(r)) *> gate.complete(()).void).uncancelable
      }
      .onError { case NonFatal(_) =>
        // On allocation error, reset the cache and notify anyone that started waiting while we were in `Allocating`
        setEmpty(gate)
      }

  private def setEmpty(gate: Gate): F[Unit] =
    (cache.set(Empty) >> gate.complete(()).void).uncancelable

  // Using `unsafe` just so that I can have RState be an inner type, to avoid useless type parameters on RState
  private val cache = Ref.unsafe[F, RState](Empty)

  // only meant to be called right after constructor and before any other method
  private def initWith(r: R): F[Unit] =
    cache.set(Ready(r))

  // empty parens to disambiguate the overload
  private def transition[A](f: RState => (RState, F[A])): F[A] =
    cache.modify(f).flatten

  // `unsafe` because the effect being run is just a `new AtomicRef...`, and most cases where we might need it,
  // we don't need it, so don't force `flatMap` to get it ready "just in case"
  private def newGate(): Gate = Deferred.unsafe[F, Unit]
}
