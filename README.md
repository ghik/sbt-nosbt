# sbt-nosbt

`sbt-nosbt` is an `sbt` plugin to make your complex, multi-module build definition more maintainable
by moving build definition from `.sbt` files to plain Scala files and providing a nice convention for
hierarchical organization of subprojects.

## Overview

`sbt` can be intimidating. This is mostly due to various layers of abstraction and "magic" that it uses. However, deep
down,
`sbt` build definition is ultimately just plain Scala code. This plugin aims to bring that plain Scala to the surface,
removing at least some of the `sbt`'s magic.

### `.sbt` files

`sbt` requires your build to be defined in `.sbt` files, which are Scala-like files preprocessed in a special way.
Most importantly, that preprocessing includes:

* automatic import of keys and other definitions from `sbt` core and plugins
* extracting all project definitions by looking for all `lazy val`s (and `val`s) typed as `Project`

`.sbt` files may also refer to definitions in `project/*.scala` files, which are regular Scala files without any special
treatment. While this allows you to move a lot of utility functions out of `.sbt` files, you are still forced to
enumerate all your projects in `.sbt` files. Typically, this is a single `build.sbt` file.

### Moving to plain Scala

The `nosbt` plugin allows you to move all your project definitions into plain Scala files. This removes all the
special `.sbt` preprocessing and allows you to organize your build definition like regular Scala code by splitting
it into multiple files that explicitly refer to each other. `sbt-nosbt` also establishes a convention for project
(and directory) hierarchy that makes it easier to define complex, multi-project builds.

## Usage example

The full example is available in an [example project repository](https://github.com/ghik/sbt-nosbt-example)

### Setup

Add the `nosbt` plugin to your `project/plugins.sbt`:

```scala
addSbtPlugin("com.github.ghik" % "sbt-nosbt" % "<version>")
```

### Build definition

Now create a `project/MyProj.scala` file with definition of a `ProjectGroup`:

```scala
import com.github.ghik.sbt.nosbt.ProjectGroup
import sbt.Keys._
import sbt._

object MyProj extends ProjectGroup("myproj") {
  // the root project of your build; its ID is `myproj` 
  // and its base directory is the root directory of the build
  lazy val root: Project = mkRootProject

  /* Subprojects of your build */

  // ID of this subroject is `myproj-api`, its base directory is `api/`
  lazy val api: Project = mkSubProject
    .settings(/* ... */)

  // ID of this subroject is `myproj-impl`, its base directory is `impl/`
  lazy val impl: Project = mkSubProject
    .dependsOn(api)
    .settings(/* ... */)

  // settings applied to all projects (optional)
  override def commonSettings: Seq[Def.Setting[_]] = Seq(
    scalaVersion := "3.2.2",
  )

  // settings in ThisBuild scope (optional)
  override def buildSettings: Seq[Def.Setting[_]] =
    Seq(/* settings that you wish to be in ThisBuild scope */)

  // settings in Global scope (optional)
  override def globalSettings: Seq[Def.Setting[_]] =
    Seq(/* settings that you wish to be Global scope */)
}
```

The above file is a complete definition of an `sbt` multi-project build, in plain Scala:

* The root project must be defined as `lazy val root` and implemented with `mkSubProject`. ID of this project will be
  the same as name of the `ProjectGroup`, i.e. `myproj`. Base directory of this project is the build root directory.
* All subprojects in the project group must be defined as `lazy val`s, just like you would do in an `.sbt` file.
  However, usage of `mkSubProject` makes sure that subprojects follow hierarchical naming and directory convention.
  For example `lazy val api: Project = mkSubProject` will define a subproject with ID `myproj-api` and base directory
  `api/`. Note how this is different from the default `sbt` behaviour which would place the project in a directory
  corresponding directly to its ID (i.e. `myproj-api/`).
* Settings shared by all the projects in your build can be defined by overriding `commonSettings`.
  Note how this is **not** the same as defining settings in `Global` or `ThisBuild` scopes - `commonSettings` are
  applied **directly** on each and every project which is more reliable than `Global`/`ThisBuild` and generally
  more recommended. There are also variations of `commonSettings`, e.g. `subprojectSettings`, `leafSubprojectSettings`,
  etc. which allow you to refine the exact set of projects that you want to apply settings on. Refer to `ProjectGroup`s
  API for details.
* Settings in `Global` scope can be set by overriding `globalSettings`
* Settings in `ThisBuild` scope can be set by overriding `buildSettings`.

Because `MyProj.scala` is a regular Scala file, its contents may be split and reorganized as you wish, e.g. be
extracting traits, subclasses, etc. into separate files. It becomes maitainable like plain Scala code.

### Bootstrapping

We also need to tell `sbt` that `MyProj.scala` is the entry point of the entire build definition. In order to do that,
we need to create a minimal, "bootstrapping" `build.sbt` file:

```scala
lazy val root = MyProj.root
```

_et voila!_

## Complex, multi-level hierarchies

Let's say your build is more complex. It is split into several "services", each one consisting of multiple subprojects.
Let's say you want to achieve a project structure like this:

```
myproj
myproj-commons
myproj-commons-db
myproj-commons-api
myproj-fooservice
myproj-fooservice-api
myproj-fooservice-impl
myproj-barservice
myproj-barservice-api
myproj-barservice-impl
```

which corresponds to the following directory structure:

```
myproj/
  commons/
    db/
    api/
  fooservice/
    api/
    impl/
  barservice/
    api/
    impl/
```

You can achieve this with the following set of definitions:

```scala
import com.github.ghik.sbt.nosbt.ProjectGroup
import sbt.Keys._
import sbt._

object MyProj extends ProjectGroup("myproj") {
  // setting shared by all projects in this group and all its child groups
  override def commonSettings: Seq[Def.Setting[_]] = Seq(
    scalaVersion := "3.2.2",
  )

  lazy val root: Project = mkRootProject

  lazy val commons: Project = Commons.root
  lazy val fooservice: Project = FooService.root
  lazy val barservice: Project = BarService.root
}

object Commons extends ProjectGroup("commons", MyProj) {
  lazy val root: Project = mkRootProject

  lazy val db: Project = mkSubProject
  lazy val api: Project = mkSubProject
}

object FooService extends ProjectGroup("fooservice", MyProj) {
  lazy val root: Project = mkRootProject

  lazy val api: Project = mkSubProject.dependsOn(Commons.api)
  lazy val impl: Project = mkSubProject.dependsOn(api, Commons.db)
}

object BarService extends ProjectGroup("barservice", MyProj) {
  lazy val root: Project = mkRootProject

  lazy val api: Project = mkSubProject.dependsOn(Commons.api)
  lazy val impl: Project = mkSubProject.dependsOn(api, Commons.db, FooService.api)
}
```

Note how `Commons`, `FooService` and `BarService` declare `MyProj` as their _parent_ project group. The `MyProj`
must also explicitly declare `lazy val`s referring to subgroups' root projects in order for `sbt` to see them.

Finally, the boostrapping `build.sbt` file:

```scala
lazy val root = MyProj.root
```

## Cross project support (for Scala.js & Scala Native)

If you want to use [`sbt-crossproject`](https://github.com/portable-scala/sbt-crossproject) for defining
projects cross compiled to Scala.js and/or Scala Native, use `sbt-nosbt-crossproject`:

```scala
addSbtPlugin("com.github.ghik" % "sbt-nosbt-crossproject" % "<version>")
```

Then, use `CrossProjectGroup` instead of `ProjectGroup` and use `mkCrossSubProject` in place of
`crossProject` macro from the `sbt-crossproject` plugin:

```scala
import com.github.ghik.sbt.nosbt.crossproject.CrossProjectGroup
import sbt.Keys._
import sbt._
import sbtcrossproject.{CrossProject, JVMPlatform}
import scalajscrossproject.JSPlatform

object MyProj extends CrossProjectGroup("myproj") {
  lazy val root: Project = mkRootProject

  lazy val foo: Project = mkSubProject // not cross compiled
  lazy val utils: CrossProject = mkCrossSubProject(JVMPlatform, JSPlatform) // cross compiled to JVM & JS
}
```

## Caveats

* Settings defined in `Global` and `ThisBuild` scopes by overriding `globalSettings` and `buildSettings` have
  lower priority than if they would be defined directly in the `.sbt` file. This means they may get overwritten by
  settings from other `sbt` plugins in your build. If this is a problem, you can lift their priority back by
  referring to them explicitly in the `build.sbt` bootstrapping file:

  ```scala
  inScope(Global)(MyProj.globalSettings)
  inThisBuild(MyProj.buildSettings)
  lazy val root = MyProj.root
  ```

  In order to avoid these problems altogether, prefer overriding `ProjectGroup.commonSettings`
  rather than using `ThisBuild`.
