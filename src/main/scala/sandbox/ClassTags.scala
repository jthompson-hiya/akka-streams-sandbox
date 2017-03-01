package sandbox

import scala.reflect.ClassTag
import shapeless.Typeable
import shapeless.TypeCase

object ClassTags extends App {

  sealed trait Inner
  case class Main(value: Double) extends Inner

  sealed trait Secondary extends Inner
  case class Foo(value: String) extends Secondary
  case class Bar(value: Int) extends Secondary

  case class Outer[T](inner: T)

  val outers: List[Outer[Inner]] = List(Outer(Foo("a")),Outer(Main(0.0)),Outer(Bar(4)),Outer(Foo("b")),Outer(Bar(-1)))

  def gatherByType[T <: Inner : Typeable](os: List[Outer[Inner]]): List[Outer[T]] = {
    val caster = implicitly[Typeable[Outer[T]]]
    os.map(o => caster.cast(o)).flatten
  }

  // Sad results with SHapeless 2.0.0, 2.3.0 does the trick
  println(gatherByType[Main](outers))
  println(gatherByType[Foo](outers))
  println(gatherByType[Bar](outers))
  println(gatherByType[Secondary](outers))
}
