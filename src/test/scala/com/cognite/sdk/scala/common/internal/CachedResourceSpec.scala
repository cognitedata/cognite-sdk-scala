// Copyright 2020 Gavin Bisesi
// SPDX-License-Identifier: MIT
// From https://gist.github.com/Daenyth/28243952f1fcfac6e8ef838040e8638e/9167a51f41322c53de492186c7bfab609fe78f8d

// scalastyle:off

package com.cognite.sdk.scala.common.internal

import fs2.Stream
import org.scalactic.source.Position
import org.scalatest._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Random
import scala.util.control.NoStackTrace
import cats.effect.concurrent.{Deferred, Ref}
import cats.effect.{ContextShift, IO, Resource, Sync, Timer}
import cats.{Applicative, ApplicativeError, FlatMap}
import cats.effect.laws.util.TestContext
import cats.instances.all._
import cats.syntax.all._
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.verbs.ResultOfStringPassedToVerb

class ConcurrentCachedObjectSpec extends AsyncFlatSpec with ConcurrentCachedResourceBehavior {

  behavior.of("ConcurrentCachedObject")

  (it should behave).like(cachedResource(create, withCleanup = false))

  (it should behave).like(concurrentCachedResource(create, withCleanup = false))

  def create: IO[(Pool, CachedResource[IO, Obj])] =
    for {
      pool <- Ref[IO].of(Map.empty[Int, Obj])
      ids <- Ref[IO].of(1)
      res = Resources.alloc(ids, pool)
      cr <- ConcurrentCachedObject(res)
    } yield (pool, cr)
}

class Obj(val id: Int) {
  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  private var _alive: Boolean = true

  def alive: Boolean = synchronized(_alive)
  def unsafeRelease(): Unit = synchronized { _alive = false }

  def assertLive[F[_]](implicit F: ApplicativeError[F, Throwable]): F[Unit] =
    Applicative[F].unlessA(alive)(F.raiseError(new Exception(s"Obj ${this.id.toString} is dead")))

  @SuppressWarnings(Array("org.wartremover.warts.Equals"))
  override def equals(obj: Any): Boolean = obj match {
    case that: Obj => this eq that
    case _ => false
  }

  override def hashCode(): Int = id.hashCode()

  override def toString: String = s"Obj(${id.toString} alive=${alive.toString})"
}

trait BaseCachedResourceBehavior[F[_]] extends Matchers with Inspectors {
  this: AsyncFlatSpec =>

  protected val time: FiniteDuration = 1.nano

  protected type Pool = Ref[F, Map[Int, Obj]]

  protected object Resources {

    def alloc(ids: Ref[F, Int], pool: Pool)(implicit F: FlatMap[F]): F[Obj] =
      ids.modify(cur => (cur + 1, cur)).flatMap { id =>
        pool.modify { m =>
          val obj = new Obj(id)
          m.updated(obj.id, obj) -> obj
        }
      }

    def basicRelease(implicit F: Sync[F]): Obj => F[Unit] =
      obj => F.delay(obj.unsafeRelease())

    def basic(implicit F: Sync[F]): F[(Pool, Resource[F, Obj])] =
      for {
        pool <- Ref[F].of(Map.empty[Int, Obj])
        ids <- Ref[F].of(1)
      } yield pool -> Resource.make(alloc(ids, pool))(basicRelease)

  }

  protected def toFuture(fa: F[Assertion])(implicit pos: Position): Future[Assertion]

  protected implicit class ItVerbStringOps(itVerbString: ItVerbString) {

    def inIO(testFun: => F[Assertion])(implicit pos: Position): Unit =
      itVerbString.in(toFuture(testFun))
  }

  protected implicit class ResultOfStringPassedToVerbOps(obj: ResultOfStringPassedToVerb) {

    def inIO(testFun: => F[Assertion])(implicit pos: Position): Unit =
      obj.in(toFuture(testFun))
  }

}

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
trait CachedResourceBehavior[F[_]] extends BaseCachedResourceBehavior[F] with OptionValues {
  this: AsyncFlatSpec =>

  /** should behave like cachedResource(create) */
  protected def cachedResource(
      create: F[(Pool, CachedResource[F, Obj])],
      withCleanup: Boolean =
        true // Whether or not this resource is expected to clean up Obj on invalidate
  )(implicit F: Sync[F]): Unit = {

    (it should "run with no previous state").inIO {
      create.flatMap { case (_, cr) =>
        cr.run(_.assertLive[F]).map(_ => succeed)
      }
    }

    (it should "invalidate with no previous state").inIO {
      create.flatMap { case (_, cr) =>
        cr.invalidate.map(_ => succeed)
      }
    }

    (it should "run and then invalidate").inIO {
      create.flatMap { case (pool, cr) =>
        for {
          id <- cr.run(r => r.assertLive[F].as(r.id))
          _ <- cr.invalidate
          obj <- pool.get.map(_.get(id))
        } yield
          if (withCleanup)
            obj.value.alive shouldEqual false
          else succeed
      }
    }

    (it should "reuse for multiple runs").inIO {
      create.flatMap { case (_, cr) =>
        for {
          id1 <- cr.run(r => r.assertLive[F].as(r.id))
          id2 <- cr.run(r => r.assertLive[F].as(r.id))
        } yield id1 shouldEqual id2
      }
    }

    (it should "get a new resource after invalidating").inIO {
      create.flatMap { case (_, cr) =>
        for {
          id1 <- cr.run(r => r.assertLive[F].as(r.id))
          _ <- cr.invalidate
          id2 <- cr.run(r => r.assertLive[F].as(r.id))
        } yield (id1 should not).equal(id2)
      }
    }

    (it should "allow run to fail and still work after").inIO {
      create.flatMap { case (_, cr) =>
        val oops = new Exception("oops")
        for {
          result <- cr.run(_ => F.raiseError[Int](oops)).attempt
          alive <- cr.run(_.alive.pure[F])
        } yield {
          result shouldEqual Left(oops)
          alive shouldBe true
        }
      }
    }
  }

}

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
trait ConcurrentCachedResourceBehavior extends CachedResourceBehavior[IO] {
  this: AsyncFlatSpec =>

  implicit val ctx: TestContext = TestContext()
  // Explicitly pass Async[IO] because sometimes scalac wants to compile
  // to 'ctx.contextShift(IO.ioConcurrentEffect(this.CS))`, which is recursive, and explodes.
  // And yes, "sometimes", because implicit resolution is slightly nondeterministic
  implicit val contextShift: ContextShift[IO] = ctx.contextShift[IO](IO.ioEffect)
  implicit val timer: Timer[IO] = ctx.timer[IO]

  def concurrentCachedResource(
      create: IO[(Pool, CachedResource[IO, Obj])],
      withCleanup: Boolean = true
  ): Unit = {
    // Alias here so I can move code between the traits easier
    type F[A] = IO[A]

    (it should "get a new resource after invalidating (concurrently)").inIO {
      create.flatMap { case (_, cr) =>
        for {
          id1 <- cr.run(r => timer.sleep(time) *> r.assertLive[F].as(r.id))
          _ <- cr.invalidate
          id2 <- cr.run(r => r.assertLive[F].as(r.id))
        } yield (id1 should not).equal(id2)
      }
    }

    (it should "reuse resource when starting a run while one run is in progress").inIO {
      create.flatMap { case (_, cr) =>
        for {
          gate <- Deferred[F, Unit]
          // use gate so run can't complete until after another concurrent run starts
          run1 <- cr.run(r => gate.get.as(r.id)).start
          id2 <- cr.run(r => gate.complete(()).as(r.id))
          id1 <- run1.join
        } yield id1 shouldEqual id2
      }
    }

    (it should "defer releasing for invalidate until in flight run completes").inIO {
      create.flatMap { case (_, cr) =>
        for {
          run <- cr.run(r => timer.sleep(time) *> r.assertLive[F]).start
          _ <- cr.invalidate
          _ <- run.join
        } yield succeed
      }
    }

    (it should "race run and invalidate without failing or leaking").inIO {
      create.flatMap { case (pool, cr) =>
        for {
          _ <- cr.run(_ => IO.unit) // warmup allocate
          parLimit = 8 // arbitrary
          tasks = 100 // arbitrary
          results <- Stream(
            cr.run(r => timer.sleep(r.id.millis) *> r.assertLive[F]).attempt,
            cr.invalidate.attempt
          ).covary[F]
            .repeat
            .take(tasks.toLong)
            .mapAsyncUnordered(parLimit)(io => io)
            .compile
            .toList
          _ <- cr.invalidate // Make sure the last task is to invalidate
          objects <- pool.get
        } yield {
          all(results) shouldBe Symbol("right")
          if (withCleanup) {
            forAll(objects.values) { obj =>
              obj.alive shouldBe false
            }
          }
          val numAllocated = objects.keySet.foldLeft(0)((x, y) => if (x >= y) x else y)
          val maxAllocated = tasks / 2 // div by 2 because half are run, half invalidate
          numAllocated should be <= maxAllocated
        }
      }
    }

    (it should "not leak or deadlock under aggressive cancellation and concurrency").inIO {
      sealed abstract class Task {
        def id: Int
        def run(cr: CachedResource[F, Obj]): F[Unit]
      }
      final case class Sleep(id: Int, dur: Int) extends Task {
        def run(cr: CachedResource[F, Obj]): F[Unit] =
          cr.run(r => timer.sleep(dur.nanos) *> r.assertLive[F])
      }
      case object Ex extends Exception("ok") with NoStackTrace
      final case class Err(id: Int) extends Task {
        val err: F[Unit] = IO.raiseError(Ex)

        def run(cr: CachedResource[F, Obj]): F[Unit] =
          cr.run(_ => err).recoverWith { case Ex =>
            IO.unit
          }
      }
      final case class Invalidate(id: Int) extends Task {
        def run(cr: CachedResource[F, Obj]): F[Unit] =
          cr.invalidate
      }

      val taskCount = 1000
      val rand = IO(Random.nextInt(5)) // 0 to 4 inclusive
      val bool = IO(Random.nextBoolean())
      val ids = Stream.iterate(0)(_ + 1)

      create.flatMap { case (pool, cr) =>
        val tasks: Stream[F, (String, Either[Throwable, Unit])] = Stream[
          F,
          Int => F[Task]
        ](
          i => rand.map(dur => Sleep(i, dur)),
          i => (Err(i): Task).pure[F],
          i => (Invalidate(i): Task).pure[F]
        ).repeat
          .take(taskCount.toLong)
          .zipWith(ids) { case (mkTask, i) => mkTask(i) }
          .evalMap(identity)
          .mapAsyncUnordered(taskCount) { (t: Task) =>
            for {
              f <- t.run(cr).start
            } yield (t, f)
          } // concurrent .start in non-deterministic order
          .mapAsyncUnordered(taskCount) { case (t, f) =>
            for {
              // Timeout will only fail if we deadlocked
              e <- Sync[F].ifM(bool)(f.cancel.attempt, f.join.timeout(1.hour).attempt)
            } yield t.toString -> e
          } // Cancel/join in non-deterministic order
        for {
          results <- tasks.compile.toVector
          _ <- cr.invalidate
          objects <- pool.get
        } yield {
          if (withCleanup) { forAll(objects.values)(_.alive shouldBe false) }
          results.foreach { case (taskId, result) =>
            withClue(taskId) {
              result shouldBe Right(())
            }
          }
          succeed
        }
      }
    }
  }

  final override protected def toFuture(
      fa: IO[Assertion]
  )(implicit pos: Position): Future[Assertion] = {
    val maxTestDuration = 1.minute
    val test = fa
      .timeoutTo(
        maxTestDuration,
        IO.raiseError(
          new Exception(
            s"Test case did not complete within ${maxTestDuration.toString}. Deadlock is likely"
          )
        )
      )
      .unsafeToFuture() // Begin eager test execution async
    // Resolve `IO` concurrency inside `test` by advancing the clock
    ctx.tick(maxTestDuration)
    ctx.tick(10.seconds) // Definitely past our `timeoutTo`

    val tasksAfterTick = ctx.state.tasks
    if (tasksAfterTick.isEmpty) {
      test // Now that `ctx` has no remaining `IO` to run, return the (completed) `Future[Assertion]`
    } else {
      // timeoutTo wasn't enough, maybe we deadlocked `uncancelable` IO?.
      // `Future` has no ability to cancel, so hopefully it gets GC'd
      throw new IllegalStateException(
        s"""Test probably deadlocked.
           | tasksAfterTick=${tasksAfterTick.toString}
           | pos=${pos.toString}""".stripMargin
      )
    }
  }

}
