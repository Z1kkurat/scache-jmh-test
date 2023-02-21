package example

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import cats.kernel.CommutativeMonoid
import cats.syntax.all._
import com.evolutiongaming.scache.Cache
import org.openjdk.jmh.annotations._

import scala.util.Random

@State(Scope.Benchmark)
class TestState {
  var cache: (Cache[IO, Int, IO[Option[Int]]], IO[Unit]) = _

  val runtime = IORuntime.global

  @Setup
  def prepare(): Unit = {
    cache = Cache.loading1[IO, Int, IO[Option[Int]]].allocated.unsafeRunSync()(runtime)
    TestData.data.toList.traverse_ { case (i, value) => cache._1.put(i, value) }.unsafeRunSync()(runtime)
  }

  @TearDown
  def tearDown(): Unit = {
    cache._2.unsafeRunSync()(runtime)
  }
}

class FoldTest {

  @Benchmark
  @BenchmarkMode(Array(Mode.SampleTime))
  def measureTraverseFilter(state: TestState): Unit = {
    val cache = state.cache._1

    val io: IO[Option[Int]] = for {
      keys   <- cache.keys
      states <- keys.toVector.traverseFilter(cache.get)
      states <- states.traverseFilter(identity)
      result = states.minOption
    } yield result

    io.unsafeRunSync()(state.runtime)
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.SampleTime))
  def measureFlatMap(state: TestState): Unit = {
    val cache = state.cache._1

    val io: IO[Option[Int]] = cache.keys.flatMap { keys =>
      keys.foldLeft(IO.pure(none[Int])) {
        case (result, key) =>
          cache.get(key).flatMap {
            case Some(value) =>
              value.flatMap {
                case Some(int) =>
                  result.flatMap {
                    case Some(currentMin) => IO.pure(currentMin.min(int).some)
                    case None => IO.pure(int.some)
                  }
                case None => result
              }
            case None => result
          }
      }
    }

    io.unsafeRunSync()(state.runtime)
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.SampleTime))
  def measureFoldMap(state: TestState): Unit = {
    val cache = state.cache._1

    implicit val intCommutativeMonoid: CommutativeMonoid[Option[Int]] = new CommutativeMonoid[Option[Int]] {
      override def empty: Option[Int] = None

      override def combine(x: Option[Int], y: Option[Int]): Option[Int] = x match {
        case Some(xValue) => y match {
          case Some(yValue) => Some(xValue.min(yValue))
          case None => Some(xValue)
        }
        case None => y
      }
    }

    val io: IO[Option[Int]] = cache.foldMap {
      case (_, Left(_)) => IO.pure(none[Int])
      case (_, Right(value)) => value
    }(intCommutativeMonoid)

    io.unsafeRunSync()(state.runtime)
  }
}

object TestData {
  private val rnd = new Random(System.currentTimeMillis())

  val data: Map[Int, IO[Option[Int]]] =
    List.fill(20000)(if (rnd.nextBoolean()) IO.pure(rnd.nextInt(128_000).some) else IO.pure(None))
      .zipWithIndex
      .map(_.swap)
      .toMap
}
