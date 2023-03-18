package com.github.ghik.plainsbt

import com.avsystem.commons.*
import sbt.*

import scala.language.experimental.macros

case class FreshProject(project: Project) extends AnyVal
object FreshProject {
  implicit def materialize: FreshProject = macro Macros.mkFreshProject
}

abstract class ProjectGroup(
  val groupName: String,
  parent: OptArg[ProjectGroup] = OptArg.Empty
) extends AutoPlugin {
  private def rootProjectId: String = parent.fold(groupName)(p => s"${p.rootProjectId}-$groupName")
  private def subProjectId(name: String): String = s"$rootProjectId-$name"

  final def baseDir: File = parent.fold(file("."))(p => p.baseDir / groupName)

  /**
   * Settings shared by all the projects defined in this [[ProjectGroup]] and its child [[ProjectGroup]]s
   * (i.e. those that declare this group as their [[parent]]).
   */
  def commonSettings: Seq[Def.Setting[_]] = Seq.empty

  /**
   * Settings shared by all the projects defined in this [[ProjectGroup]] and its child [[ProjectGroup]]s
   * (i.e. those that declare this group as their [[parent]]), excluding the root project of this group.
   */
  def subprojectSettings: Seq[Def.Setting[_]] = Seq.empty

  /**
   * Settings shared by all the projects defined in this [[ProjectGroup]], including its root project
   * (via [[mkRootProject]]) and directly defined subprojects (via [[mkSubProject]]).
   */
  def directCommonSettings: Seq[Def.Setting[_]] = Seq.empty

  /**
   * Settings shared by all the subprojects defined in this [[ProjectGroup]] via [[mkSubProject]].
   * Like [[directCommonSettings]] but excludes the root project of this group.
   */
  def directSubprojectSettings: Seq[Def.Setting[_]] = Seq.empty

  /**
   * A [[ProjectReference]] to the root project of this group. Use this if referring directly
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
   *
   * The root project will automatically aggregate all subprojects in the group
   * (i.e. tasks invoked on the root project will also be invoked on aggregated subprojects).
   *
   * Note how `this` is being added as an sbt plugin of this root project. The purpose of this is to make
   * sbt see all the subprojects in this project group via [[extraProjects]]. Subprojects themselves
   * are listed by the [[enumerateSubprojects]] method (which must always be implemented with [[discoverProjects]]
   * macro).
   */
  protected def mkRootProject(implicit freshProject: FreshProject): Project =
    freshProject.project.in(baseDir)
      .withId(rootProjectId)
      .enablePlugins(this)
      .aggregate(enumerateSubprojects.map(p => p: ProjectReference) *)
      .settings(commonSettings)
      .settings(directCommonSettings)
      .settings(parent.mapOr(Nil, _.commonSettings))
      .settings(parent.mapOr(Nil, _.subprojectSettings))

  /**
   * Creates a subproject in this project group. This method should be used in a similar way that regular
   * `sbt.project` method (macro) is used, i.e. it should be assigned to a `lazy val` public member in the
   * class that extends [[ProjectGroup]].
   *
   * Name of this `lazy val` will be used as the subdirectory name for this project and as a suffix in the
   * ID of the project.
   */
  protected def mkSubProject(implicit freshProject: FreshProject): Project = {
    val project = freshProject.project
    project
      .in(baseDir / project.id)
      .withId(subProjectId(project.id))
      .settings(commonSettings)
      .settings(subprojectSettings)
      .settings(directCommonSettings)
      .settings(directSubprojectSettings)
      .settings(parent.mapOr(Nil, _.commonSettings))
      .settings(parent.mapOr(Nil, _.subprojectSettings))
  }

  /**
   * A macro that lists all the subprojects in this group. Subprojects must be assigned to public `lazy val`s.
   * This macro must be used to implement [[enumerateSubprojects]] in every implementation of this class.
   * The subprojects are then made visible to sbt via [[extraProjects]].
   */
  protected final def discoverProjects: Seq[Project] = macro Macros.discoverProjectsImpl

  /**
   * Implement this method in subclasses using [[discoverProjects]] macro.
   * This is a boilerplate that must be repeated in every implementation.
   */
  protected def enumerateSubprojects: Seq[Project]

  override final def extraProjects: Seq[Project] = enumerateSubprojects
}
