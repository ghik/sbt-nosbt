package com.github.ghik.sbt.nosbt

import scala.annotation.tailrec
import scala.reflect.macros.blackbox

// copied from sbt.std.KeyMacro
private[nosbt] object SbtMacroUtils {
  def definingValName(c: blackbox.Context, invalidEnclosingTree: String => String): String = {
    import c.universe.{Apply as ApplyTree, *}
    val methodName = c.macroApplication.symbol.name
    def processName(n: Name): String =
      n.decodedName.toString.trim // trim is not strictly correct, but macros don't expose the API necessary
    @tailrec def enclosingVal(trees: List[c.Tree]): String = {
      trees match {
        case ValDef(_, name, _, _) :: _ => processName(name)
        case (_: ApplyTree | _: Select | _: TypeApply) :: xs => enclosingVal(xs)
        // lazy val x: X = <methodName> has this form for some reason (only when the explicit type is present, though)
        case Block(_, _) :: DefDef(mods, name, _, _, _, _) :: _ if mods.hasFlag(Flag.LAZY) =>
          processName(name)
        case _ =>
          c.error(c.enclosingPosition, invalidEnclosingTree(methodName.decodedName.toString))
          "<error>"
      }
    }
    enclosingVal(enclosingTrees(c).toList)
  }

  private def enclosingTrees(c: blackbox.Context): Seq[c.Tree] =
    c.asInstanceOf[reflect.macros.runtime.Context]
      .callsiteTyper
      .context
      .enclosingContextChain
      .map(_.tree.asInstanceOf[c.Tree])
}
