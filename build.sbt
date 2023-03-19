Global / excludeLintKeys += ideBasePackages

inThisBuild(Seq(
  organization := "com.github.ghik",
  homepage := Some(url("https://github.com/ghik/sbt-nosbt")),

  githubWorkflowTargetTags ++= Seq("v*"),
  githubWorkflowJavaVersions := Seq(JavaSpec.temurin("17")),
  githubWorkflowPublishTargetBranches := Seq(RefPredicate.StartsWith(Ref.Tag("v"))),

  githubWorkflowPublish := Seq(WorkflowStep.Sbt(
    List("ci-release"),
    env = Map(
      "PGP_PASSPHRASE" -> "${{ secrets.PGP_PASSPHRASE }}",
      "PGP_SECRET" -> "${{ secrets.PGP_SECRET }}",
      "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}",
      "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}"
    )
  )),
))

val commonSettings: Seq[Def.Setting[_]] = Seq(
  ideBasePackages := Seq(s"${organization.value}.sbt.nosbt"),

  Compile / scalacOptions ++= Seq(
    "-encoding", "utf-8",
    "-explaintypes",
    "-feature",
    "-deprecation",
    "-unchecked",
    "-language:implicitConversions",
    "-language:existentials",
    "-language:dynamics",
    "-language:experimental.macros",
    "-language:higherKinds",
    "-Werror",
    "-Xlint:-missing-interpolator,-adapted-args,-unused,_",
  ),

  publishMavenStyle := true,
  pomIncludeRepository := { _ => false },
  publishTo := sonatypePublishToBundle.value,

  projectInfo := ModuleInfo(
    nameFormal = "sbt-nosbt",
    description = "SBT plugin for organizing your build into plain Scala files",
    homepage = Some(url("https://github.com/ghik/sbt-nosbt")),
    startYear = Some(2023),
    licenses = Vector(
      "Apache License, Version 2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0")
    ),
    organizationName = "ghik",
    organizationHomepage = Some(url("https://github.com/ghik")),
    scmInfo = Some(ScmInfo(
      browseUrl = url("https://github.com/ghik/sbt-nosbt.git"),
      connection = "scm:git:git@github.com:ghik/sbt-nosbt.git",
      devConnection = Some("scm:git:git@github.com:ghik/sbt-nosbt.git")
    )),
    developers = Vector(
      Developer("ghik", "Roman Janusz", "romeqjanoosh@gmail.com", url("https://github.com/ghik"))
    ),
  ),

  pluginCrossBuild / sbtVersion := {
    scalaBinaryVersion.value match {
      case "2.12" => "1.8.0"
    }
  },
)

lazy val root: Project = project.in(file("."))
  .aggregate(crossproject)
  .enablePlugins(SbtPlugin)
  .settings(
    commonSettings,
    name := "sbt-nosbt",
    libraryDependencies ++= Seq(
      "com.avsystem.commons" %% "commons-core" % "2.9.0",
    ),
  )

lazy val crossproject: Project = project
  .enablePlugins(SbtPlugin)
  .dependsOn(LocalRootProject) // avoid recursion
  .settings(
    commonSettings,
    name := "sbt-nosbt-crossproject",
    addSbtPlugin("org.portable-scala" % "sbt-crossproject" % "1.2.0"),
  )
