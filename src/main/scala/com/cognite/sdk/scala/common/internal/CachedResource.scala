// Copyright 2020 Gavin Bisesi
// SPDX-License-Identifier: MIT
// From https://gist.github.com/Daenyth/28243952f1fcfac6e8ef838040e8638e/9167a51f41322c53de492186c7bfab609fe78f8d

package com.cognite.sdk.scala.common.internal

import cats.effect.concurrent.{Deferred, Ref}
import cats.effect.implicits._
import cats.effect.{Bracket, Concurrent, ExitCase, Sync}
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
