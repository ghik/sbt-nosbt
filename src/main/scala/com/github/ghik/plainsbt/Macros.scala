package com.github.ghik.plainsbt

import scala.reflect.macros.blackbox

class Macros(val c: blackbox.Context) {

  import c.universe.*

  private def PlainsbtPkg = q"_root_.com.github.ghik.plainsbt"

  private def classBeingConstructed: ClassSymbol = {
    val ownerConstr = c.internal.enclosingOwner
    if (!ownerConstr.isConstructor) {
      c.abort(c.enclosingPosition, s"${c.macroApplication.symbol} can only be used as super constructor argument")
    }
    ownerConstr.owner.asClass
  }

  def discoverProjectsImpl: Tree = {
    val sbtProjectCls = c.mirror.staticClass("_root_.sbt.Project")

    val projectGroupTpe =
      c.mirror.staticClass("_root_.com.github.ghik.plainsbt.ProjectGroup").toType

    val rootProjectSym =
      projectGroupTpe.member(TermName("root"))

    val ptpe = classBeingConstructed.toType
    val arg = c.freshName(TermName("pg"))

    val projectRefs =
      ptpe.members.iterator
        .filter { m =>
          m.isTerm && m.asTerm.isGetter &&
            m.typeSignature.finalResultType.typeSymbol == sbtProjectCls &&
            !(m :: m.overrides).contains(rootProjectSym)
        }
        .map(m => q"$arg.asInstanceOf[$ptpe].$m")
        .toList

    q"($arg: $projectGroupTpe) => _root_.scala.Seq(..$projectRefs)"
  }

  def mkFreshProject: Tree =
    q"$PlainsbtPkg.FreshProject(_root_.sbt.project)"
}
