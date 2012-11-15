package ppl.dsl.forge
package templates
package compiler

import java.io.PrintWriter
import core._
import shared.BaseGenImports

trait DeliteGenImports extends BaseGenImports {  
  this: ForgeCodeGenDelite =>
  
  val IR: ForgeApplicationRunner with ForgeExp 
  import IR._
    
  def emitDeliteCollectionImport(stream: PrintWriter) {
    stream.println("import ppl.delite.framework.datastruct.scala.DeliteCollection")
  }

  def emitDeliteOpsImports(stream: PrintWriter) {
    emitDeliteCollectionImport(stream)
    stream.println("import ppl.delite.framework.ops.{DeliteOpsExp, DeliteCollectionOpsExp}")
    stream.println("import ppl.delite.framework.Util._")
  }

  def emitDelitePackageImports(stream: PrintWriter) {
    stream.println("import ppl.delite.framework.{Config, DeliteApplication}")
    stream.println("import ppl.delite.framework.codegen.Target")
    stream.println("import ppl.delite.framework.codegen.scala.TargetScala")
    stream.println("import ppl.delite.framework.codegen.cuda.TargetCuda")
    stream.println("import ppl.delite.framework.codegen.c.TargetC")
    stream.println("import ppl.delite.framework.codegen.opencl.TargetOpenCL")
    stream.println("import ppl.delite.framework.ops._")
    stream.println("import ppl.delite.framework.datastructures._")
    stream.println("import ppl.delite.framework.codegen.delite.overrides._")
  }

  def emitDeliteImports(stream: PrintWriter) {
    emitDelitePackageImports(stream)
    emitDeliteOpsImports(stream)
  }
  
  override def emitDSLImports(stream: PrintWriter) {
    super.emitDSLImports(stream)
    stream.println("import " + packageName + "._")
    stream.println("import " + packageName + ".ops._")
  }  
 
  override def emitAllImports(stream: PrintWriter) {
    super.emitAllImports(stream)
    emitLMSImports(stream)
    emitDeliteImports(stream)   
  } 
}