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

lazy val root = project.in(file("."))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-nosbt",
    pluginCrossBuild / sbtVersion := {
      scalaBinaryVersion.value match {
        case "2.12" => "1.8.0"
      }
    },

    ideBasePackages := Seq(s"${organization.value}.${name.value}"),
    libraryDependencies ++= Seq(
      "com.avsystem.commons" %% "commons-core" % "2.9.0",
    ),

    publishMavenStyle := true,
    pomIncludeRepository := { _ => false },
    publishTo := sonatypePublishToBundle.value,

    projectInfo := ModuleInfo(
      nameFormal = "PlainSBT",
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
  )
