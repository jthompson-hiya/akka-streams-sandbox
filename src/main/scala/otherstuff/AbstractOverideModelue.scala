package otherstuff

trait ExtraBits {
  trait ExtraBitsOps {
    def bits: String
  }

  val extra: ExtraBitsOps
}

trait Module extends ExtraBits {
  trait ModuleOps {
    def op: String
  }

  val ops: ModuleOps
}

trait FooModule extends Module {
  object ops extends ModuleOps {
    def op = s"Foo${extra.bits}"
  }
}

trait DefaultExtraBits extends ExtraBits {
  object extra extends ExtraBitsOps {
    def bits = ""
  }
}

trait BarExtraBits extends ExtraBits {
  object extra extends ExtraBitsOps {
    def bits = "Bar"
  }
}

object AbstractOverideModelue extends App {

  val baseModule = new AnyRef with FooModule with DefaultExtraBits

  println(s"baseModule.ops.op = ${baseModule.ops.op}")

  val decoratedModule = new AnyRef with FooModule with BarExtraBits

  println(s"decoratedModule.ops.op = ${decoratedModule.ops.op}")
}
