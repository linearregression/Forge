package ppl.dsl.forge
package core

import java.io.{File,PrintWriter,FileWriter}
import scala.tools.nsc.io.{Directory,Path}
import scala.reflect.SourceContext
import scala.virtualization.lms.common._
import scala.virtualization.lms.internal.{GenericFatCodegen, GenericCodegen}

trait ForgeApplication extends Forge with ForgeLift {
  def dslName: String
  def lmsAppOps: List[LMSOps] = List()
  def lmsCompOps: List[LMSOps] = List()
  def specification(): Rep[Unit]    
}

/**
 * These are the portions of Scala imported into Forge's scope.
 */
trait ForgeLift extends LiftString with LiftBoolean with LiftNumeric with LiftPrimitives {
  this: Forge =>
}

trait ForgeScalaOpsPkg extends Base
  with ImplicitOps with OrderingOps with StringOps with ArrayOps
  with BooleanOps with PrimitiveOps with TupleOps with CastingOps 

trait ForgeScalaOpsPkgExp extends ForgeScalaOpsPkg 
  with ImplicitOpsExp with OrderingOpsExp with StringOpsExp  with ArrayOpsExp 
  with BooleanOpsExp with PrimitiveOpsExp with TupleOpsExp with CastingOpsExp 

trait ForgeScalaCodeGenPkg extends ScalaGenEffect
  with ScalaGenImplicitOps with ScalaGenOrderingOps with ScalaGenStringOps  with ScalaGenArrayOps 
  with ScalaGenBooleanOps with ScalaGenPrimitiveOps with ScalaGenTupleOps with ScalaGenCastingOps 
  { val IR: ForgeScalaOpsPkgExp  }


/**
 * This the trait that every Forge application must extend.
 */
trait Forge extends ForgeScalaOpsPkg with DerivativeTypes with Definitions with ForgeOps with SpecOps {
  this: ForgeApplication =>
}

/**
 * These are the corresponding IR nodes for Forge.
 */
trait ForgeExp extends Forge with ForgeUtilities with ForgeScalaOpsPkgExp with ForgeOpsExp with FieldOpsExp with SpecOpsExp {
  this: ForgeApplication =>
}

trait ForgeUtilities {  
  def err(s: String)(implicit ctx: SourceContext) = {
    println("[forge error]: " + s)
    println("  at " + (ctx.fileName.split("/").last + ":" + ctx.line)) 
    exit(1)
  }
  def warn(s: String) = println("[forge warning]: " + s)  
  def info(s: String) = println("[forge]: " + s)
}


/**
 * Forge code generators
 */
trait ForgeCodeGenBase extends GenericCodegen with ScalaGenBase {
  val IR: ForgeApplicationRunner with ForgeExp
  import IR._
    
  def buildDir: String
  lazy val dslDir = buildDir + File.separator + "src" + File.separator + dsl.toLowerCase() + File.separator
    
  def makeTpeArgsWithBounds(args: List[Rep[TypeArg]]): String = {
    if (args.length < 1) return ""    
    val args2 = args.map { a => a.name + (if (a.ctxBounds != Nil) ":" + a.ctxBounds.map(_.name).mkString(":") else "") }
    "[" + args2.mkString(",") + "]"
  }
  
  def makeTpeArgs(args: List[Rep[TypeArg]]): String = {
    if (args.length < 1) return ""
    "[" + args.map(_.name).mkString(",") + "]"
  }
  
  def varify(a: Exp[Any]) = "Var[" + quote(a) + "]"
  def repify(a: Exp[Any]) = "Rep[" + quote(a) + "]"
  
  override def quote(x: Exp[Any]) : String = x match {
    case Def(Tpe(s, args)) => s + makeTpeArgs(args)
    case Def(TpeArg(s, ctx)) => s 
    case _ => super.quote(x)
  }  
}

/**
 * This is the interface that all backends must implement to generate an implementation of the DSL
 * from the specification.
 */
trait ForgeCodeGenBackend extends ForgeCodeGenBase with ForgeScalaCodeGenPkg with ScalaGenForgeOps {
  val IR: ForgeApplicationRunner with ForgeExp  
  
  def emitDSLImplementation(): Unit
}
