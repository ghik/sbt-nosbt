package com.github.ghik.sbt.nosbt

import com.avsystem.commons.*
import sbt.*

import scala.language.experimental.macros

abstract class ProjectGroup(
  val groupName: String,
  parent: OptArg[ProjectGroup] = OptArg.Empty
)(implicit
  discoveredProjects: DiscoveredProjects,
) extends AutoPlugin {
  protected def rootProjectId: String = parent.fold(groupName)(p => s"${p.rootProjectId}-$groupName")
  protected def subProjectId(name: String): String = s"$rootProjectId-$name"

  final def parentGroup: Option[ProjectGroup] = parent.toOption

  def baseDir: File = parent.fold(file("."))(p => p.baseDir / groupName)
  protected def subProjectDir(name: String): File = baseDir / name

  /**
   * Settings shared by all the projects defined in this [[ProjectGroup]] and its child [[ProjectGroup]]s
   * (i.e. those that declare this group as their [[parentGroup]]).
   */
  def commonSettings: Seq[Def.Setting[?]] = Seq.empty

  /**
   * Settings shared by all the projects defined in this [[ProjectGroup]] and its child [[ProjectGroup]]s
   * (i.e. those that declare this group as their [[parentGroup]]), excluding the root project of this group.
   */
  def subProjectSettings: Seq[Def.Setting[?]] = Seq.empty

  /**
   * Settings shared by all subprojects defined via `mkSubProject` in this [[ProjectGroup]] and all its
   * child [[ProjectGroup]]s. This is like [[commonSettings]] but excludes all the intermediate aggregating projects,
   * i.e. the root projects of each [[ProjectGroup]].
   */
  def leafProjectSettings: Seq[Def.Setting[?]] = Seq.empty

  /**
   * Settings shared by all the projects defined in this [[ProjectGroup]], including its root project
   * (via `mkRootProject` and directly defined subprojects (via `mkSubProject`).
   */
  def directCommonSettings: Seq[Def.Setting[?]] = Seq.empty

  /**
   * Settings shared by all the subprojects defined in this [[ProjectGroup]] via `mkSubProject`.
   * Like [[directCommonSettings]] but excludes the root project of this group.
   */
  def directSubProjectSettings: Seq[Def.Setting[?]] = Seq.empty

  /**
   * A `ProjectReference` to the root project of this group. Use this if referring directly
   * to the root project would create a cycle during project resolution.
   */
  final def rootRef: ProjectReference = LocalProject(rootProjectId)

  /**
   * Implement this method using [[mkRootProject]]
   * (with additional configs and settings for the root project if necessary)
   */
  def root: Project

  /**
   * Creates the sbt root project for this project group. The ID of this project will be set to
   * `<parentGroupPrefix>-<groupName>` or simply `<groupName>` if this project group is the toplevel group.
   *
   * The base directory for this project will be set to:
   * - current directory if this project group is the toplevel group
   * - `<parentGroupDirectory>/<groupName>` otherwise
   */
  protected def mkRootProject: Project =
    Project(rootProjectId, baseDir)
      .enablePlugins(this)
      .settings(allRootProjectSettings)

  protected final def allRootProjectSettings: Seq[Def.Setting[?]] =
    parent.mapOr(Nil, _.commonSettings) ++
      parent.mapOr(Nil, _.subProjectSettings) ++
      commonSettings ++
      directCommonSettings

  /**
   * Creates a subproject in this project group. This method should be used in a similar way that regular
   * `sbt.project` method (macro) is used, i.e. it should be assigned to a `lazy val` public member in an
   * object implementing [[ProjectGroup]].
   *
   * Name of this `lazy val` will be used as the subdirectory name for this project and as a suffix in the
   * ID of the project.
   */
  protected final def mkSubProject: Project = macro Macros.mkSubProjectImpl

  protected def mkSubProject(name: String): Project =
    Project(subProjectId(name), subProjectDir(name))
      .settings(allSubProjectSettings)

  protected final def allSubProjectSettings: Seq[Def.Setting[?]] =
    parent.mapOr(Nil, _.commonSettings) ++
      parent.mapOr(Nil, _.subProjectSettings) ++
      parent.mapOr(Nil, _.leafProjectSettings) ++
      commonSettings ++
      subProjectSettings ++
      leafProjectSettings ++
      directCommonSettings ++
      directSubProjectSettings

  final def subprojects: Seq[Project] = discoveredProjects.get(this)

  override final def extraProjects: Seq[Project] = subprojects

  // make sure the user doesn't mistakenly override these
  override final def trigger: PluginTrigger = noTrigger
  override final def requires: Plugins = Plugins.empty
  override final def projectConfigurations: Seq[Configuration] = Nil
  override final def projectSettings: Seq[Def.Setting[?]] = Nil
}

trait DiscoveredProjects {
  def get(group: ProjectGroup): Seq[Project]
}
object DiscoveredProjects {
  implicit def materialize: DiscoveredProjects = macro Macros.discoverProjectsImpl
}
