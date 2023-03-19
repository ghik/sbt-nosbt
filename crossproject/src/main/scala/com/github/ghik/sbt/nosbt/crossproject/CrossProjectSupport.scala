package com.github.ghik.sbt.nosbt
package crossproject

import com.avsystem.commons.misc.OptArg
import sbtcrossproject.{CrossProject, CrossType, Platform}

trait CrossProjectSupport { this: ProjectGroup =>
  /**
   * A cross-project version of `mkSubProject`.
   * Use this method in place of `sbtcrossproject.CrossPlugin.autoImport.crossProject`.
   */
  protected def mkCrossSubProject(platforms: Platform*): CrossProjectBuilder = macro Macros.mkCrossSubProjectImpl

  protected def mkCrossSubProject(name: String)(platforms: Platform*): CrossProjectBuilder =
    new CrossProjectBuilder(CrossProject(subProjectId(name), subProjectDir(name))(platforms *))

  /** Imitates `CrossProject.Builder` */
  protected final class CrossProjectBuilder(wrapped: CrossProject.Builder) {
    def withoutSuffixFor(platform: Platform): CrossProjectBuilder =
      new CrossProjectBuilder(wrapped.withoutSuffixFor(platform))

    def crossType(crossType: CrossType): CrossProjectBuilder =
      new CrossProjectBuilder(wrapped.crossType(crossType))

    def build(): CrossProject =
      wrapped.build().settings(allSubProjectSettings)
  }
  protected object CrossProjectBuilder {
    implicit def crossProjectFromBuilder(builder: CrossProjectBuilder): CrossProject =
      builder.build()
  }
}

abstract class CrossProjectGroup(
  groupName: String,
  parent: OptArg[ProjectGroup] = OptArg.Empty,
)(implicit
  discoverProject: DiscoveredProjects
) extends ProjectGroup(groupName, parent) with CrossProjectSupport
