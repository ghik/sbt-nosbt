package com.github.ghik.plainsbt

import scala.reflect.macros.blackbox

class Macros(val c: blackbox.Context) {

  import c.universe.*

  private def PlainsbtPkg = q"_root_.com.github.ghik.plainsbt"

  def discoverProjectsImpl: Tree = {
    val sbtProjectCls = c.mirror.staticClass("_root_.sbt.Project")

    val rootProjectSym =
      c.mirror.staticClass("_root_.com.github.ghik.plainsbt.ProjectGroup")
        .toType.member(TermName("root"))

    val ptpe = c.prefix.actualType

    val projectRefs =
      ptpe.members.iterator
        .filter { m =>
          m.isTerm && m.asTerm.isGetter &&
            m.typeSignature.finalResultType.typeSymbol == sbtProjectCls &&
            !(m :: m.overrides).contains(rootProjectSym)
        }
        .map(m => q"${c.prefix}.$m")
        .toList

    q"_root_.scala.Seq(..$projectRefs)"
  }

  def mkFreshProject: Tree =
    q"$PlainsbtPkg.FreshProject(_root_.sbt.project)"
}
