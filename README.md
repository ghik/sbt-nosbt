# plainsbt

`plainsbt` is a Scala plugin to make your complex, multi-module build definition maintainable like a regular Scala code.

## Overview

`sbt` can be intimidating. This is mostly caused by various abstraction and "magic" layers that it employs. However, deep down, `sbt` build definition is ultimately just plain Scala code. This plugin aims to bring that plain Scala to the surface, removing at least some of the `sbt`'s magic around it.

### `.sbt` files

`sbt` requires your build to be defined in `.sbt` files, which are Scala-like files preprocessed in a special way. Most importantly, that preprocessing includes:

* automatic import of keys and other definitions from `sbt` core and plugins
* extracting all project definitions by looking for all `lazy val`s (and `val`s) typed as `Project`

`.sbt` files may also refer to code in `project/*.scala` files, which are regular Scala files without any special treatment. While this allows you to move a lot of utility functions out of `.sbt` files, you are still forced to enumerate all your projects in `.sbt` files. Typically, this is a single `build.sbt` file.

### Moving to plain Scala

`plainsbt` plugin allows you to move all your project definitions into plain Scala files inside `project` directory. This removes all the special `.sbt` preprocessing and allows you to organize your build definition like a regular Scala code by splitting it into multiple files that refer to each other explicitly. `plainsbt` also establishes a hierarchical convention that makes it easier to define complex, multi-project builds.

## Usage example

The full example is available as an [example project](https://github.com/ghik/plainsbt-example)

### Simple multi-project build

Add the `plainsbt` plugin to your `project/plugins.sbt`:

```scala
addSbtPlugin("com.github.ghik" % "plainsbt" % "<version>")
```

Now create a `project/MyProj.scala` file with definition of a `ProjectGroup`:

```scala
import com.github.ghik.plainsbt.ProjectGroup
import sbt.Keys._
import sbt._

object MyProj extends ProjectGroup("myproj") {
  // the root project of your build; its ID is `myproj` and its base directory is the root directory of the build
  lazy val root: Project = mkRootProject
  
  /* Subprojects of your build */
  
  // ID of this subroject is `myproj-api`, its base directory is `api/`
  lazy val api: Project = mkSubProject
    .settings(/* ... */)
  
  // ID of this subroject is `myproj-impl`, its base directory is `impl/`
  lazy val impl: Project = mkSubProject
    .dependsOn(api)
    .settings(/* ... */)

  // settings in Global scope (optional)
  override def globalSettings: Seq[Def.Setting[_]] = 
    Seq(/* settings that you wish to be Global scope */)

  // settings in ThisBuild scope (optional)
  override def buildSettings: Seq[Def.Setting[_]] = 
    Seq(/* settings that you wish to be in ThisBuild scope */)
    
  // mandatory boilerplate that collects the subprojects
  protected def enumerateSubprojects: Seq[Project] = discoverProjects
}
```

The above file is a complete definition of an `sbt` multi-project build, in plain Scala:

* The root project must be defined as `lazy val root` and implemented with `mkSubProject`. ID of this project will be the same as name of the `ProjectGroup`, i.e. `myproj`
* All subprojects in the project group must be defined as `lazy val`s, just like you would do in an `.sbt` file. However, usage of `mkSubProject` makes sure that subprojects follow hierarchical naming convention. For example `lazy val api: Project = mkSubProject` will define a subproject with ID `myproj-api`.
* The `root` project automatically [aggregates](https://www.scala-sbt.org/1.x/docs/Multi-Project.html#Aggregation) all subprojects.
* Settings in `Global` scope can be set by overriding `globalSettings`
* Settings in `ThisBuild` scope can be set by overriding `buildSettings`.

Because `MyProj.scala` is a regular Scala file, its contents may be split and reorganized as you wish, e.g. be extracting traits, subclasses, etc. into separate files. It becomes maitainable like plain Scala code.

### Bootstrapping

We also need to tell `sbt` that `MyProj.scala` is the entry point of the entire build definition. In order to do that, we need to create a minimal, "bootstrapping" `build.sbt` file:

```
lazy val root = MyProj.root
```

_et voila!_

### Complex, multi-level hierarchies

Let's say your build is more complex. It is split into several "services", each one consisting of multiple subprojects. Let's say you want to achieve a project structure like this:

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
import com.github.ghik.plainsbt.ProjectGroup
import sbt.Keys._
import sbt._

object MyProj extends ProjectGroup("myproj") {
  lazy val root: Project = mkRootProject
  
  lazy val commons: Project = Commons.root
  lazy val fooservice: Project = FooService.root
  lazy val barservice: Project = BarService.root
    
  // mandatory boilerplate that collects the subprojects
  protected def enumerateSubprojects: Seq[Project] = discoverProjects
}

object Commons extends ProjectGroup("commons", MyProj) {
  lazy val root: Project = mkRootProject
  
  lazy val db: Project = mkSubProject
  lazy val api: Project = mkSubProject
  
  // mandatory boilerplate that collects the subprojects
  protected def enumerateSubprojects: Seq[Project] = discoverProjects
}

object FooService extends ProjectGroup("fooservice", MyProj) {
  lazy val root: Project = mkRootProject
  
  lazy val api: Project = mkSubProject.dependsOn(Commons.api)
  lazy val impl: Project = mkSubProject.depensOn(api, Commons.db)
  
  // mandatory boilerplate that collects the subprojects
  protected def enumerateSubprojects: Seq[Project] = discoverProjects
}

object BarService extends ProjectGroup("barservice", MyProj) {
  lazy val root: Project = mkRootProject
  
  lazy val api: Project = mkSubProject.dependsOn(Commons.api)
  lazy val impl: Project = mkSubProject.depensOn(api, Commons.db, FooService.api)
  
  // mandatory boilerplate that collects the subprojects
  protected def enumerateSubprojects: Seq[Project] = discoverProjects
}
```

Note how `Commons`, `FooService` and `BarService` declare `MyProj` as their _parent_ project group. The `MyProj` must also explicitly declare `lazy val`s referring to subgroups' root projects in order for `sbt` to see them.

Finally, the boostrapping `build.sbt` file:

```scala
lazy val root = MyProj.root
```

## Caveats

* Settings defined in `Global` and `ThisBuild` scopes by overriding `globalSettings` and `buildSettings` have lower priority than if they would be defined directly in the `.sbt` file. This means they may get overwritten by settings from other `sbt` plugins in your build. If this is a problem, you can lift their priority back by referring to them explicitly in the `build.sbt` bootstrapping file:

  ```scala
  inScope(Global)(MyProj.globalSettings)
  inThisBuild(MyProj.buildSettings)
  lazy val root = MyProj.root
  ```
