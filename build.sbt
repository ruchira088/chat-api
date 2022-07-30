import Dependencies._
import sbtrelease.Git
import sbtrelease.ReleaseStateTransformations._
import sbtrelease.Utilities.stateW

import java.awt.Desktop
import scala.sys.process.ProcessBuilder

val ReleaseBranch = "dev"
val ProductionBranch = "main"

inThisBuild {
  Seq(
    organization := "com.ruchij",
    scalaVersion := Dependencies.ScalaVersion,
    maintainer := "me@ruchij.com",
    scalacOptions ++= Seq("-Xlint", "-feature", "-Wconf:cat=lint-byname-implicit:s"),
    addCompilerPlugin(kindProjector),
    addCompilerPlugin(betterMonadicFor)
  )
}

lazy val migrationApp =
  (project in file("./migration-app"))
    .enablePlugins(JavaAppPackaging)
    .settings(
      name := "chat-migration-app",
      libraryDependencies ++= Seq(catsEffect, flyway, postgres, h2, pureconfig, scalaLogging, logbackClassic),
      topLevelDirectory := None,
      Universal / javaOptions ++= Seq("-Dlogback.configurationFile=/opt/data/logback.xml"),
    )

lazy val root =
  (project in file("."))
    .enablePlugins(BuildInfoPlugin, JavaAppPackaging)
    .settings(
      name := "chat-api",
      libraryDependencies ++=
        Seq(
          http4sDsl,
          http4sEmberServer,
          http4sCirce,
          http4sEmberClient,
          circeGeneric,
          circeParser,
          circeLiteral,
          jodaTime,
          doobieHikari,
          postgres,
          h2,
          bcrypt,
          enumeratum,
          mongoDriver,
          redis4CatsEffect,
          kafkaClients,
          kafkaAvroSerializer,
          chatAvroSchemas,
          pureconfig,
          embeddedRedis,
          embeddedKafkaSchemaRegistry,
          embeddedMongo,
          logbackClassic,
          scalaLogging
        ) ++
          Seq(scalaTest, scalaMock, pegdown).map(_ % Test),
      resolvers ++=
        Seq(
          "Confluent" at "https://packages.confluent.io/maven/",
          "jitpack" at "https://jitpack.io",
          "JFrog Artifactory" at "https://ruchij.jfrog.io/artifactory/default-maven-virtual/"
        ),
      credentials += {
        val artifactoryCredentials =
            for {
            username <- environmentVariable("ARTIFACTORY_USERNAME")
            password <- environmentVariable("ARTIFACTORY_PASSWORD")
          } yield Credentials("Artifactory Realm", "ruchij.jfrog.io", username, password)

        artifactoryCredentials.getOrElse(Credentials(Path.userHome / ".sbt" / ".credentials"))
      },
      buildInfoKeys := Seq[BuildInfoKey](name, organization, version, scalaVersion, sbtVersion),
      buildInfoPackage := "com.eed3si9n.ruchij",
      topLevelDirectory := None,
      Universal / javaOptions ++= Seq("-Dlogback.configurationFile=/opt/data/logback.xml"),
      Compile / unmanagedResourceDirectories += baseDirectory.value / "nginx" / "files"
    )
    .dependsOn(migrationApp)

lazy val development =
  (project in file("./development"))
      .settings(
        name := "chat-api-development"
      )
      .dependsOn(migrationApp, root)

val verifyReleaseBranch = { state: State =>
  val git = Git.mkVcs(state.extract.get(baseDirectory))
  val branch = git.currentBranch

  if (branch != ReleaseBranch) {
    sys.error {
      s"The release branch is $ReleaseBranch, but the current branch is set to $branch"
    }
  } else state
}

val mergeReleaseToMaster = { state: State =>
  val git = Git.mkVcs(state.extract.get(baseDirectory))

  val (updatedState, releaseTag) = state.extract.runTask(releaseTagName, state)

  updatedState.log.info(s"Merging $releaseTag to $ProductionBranch...")

  val userInput: Option[ProcessBuilder] =
    SimpleReader
      .readLine("Push changes to the remote master branch (y/n)? [y]")
      .map(_.toLowerCase) match {
      case Some("y") | Some("") =>
        updatedState.log.info(s"Pushing changes to remote master ($releaseTag)...")
        Some(git.cmd("push"))

      case _ =>
        updatedState.log.warn("Remember to push changes to remote master")
        None
    }

  val actions: List[ProcessBuilder] =
    List(git.cmd("checkout", ProductionBranch), git.cmd("pull", "--rebase"), git.cmd("merge", releaseTag)) ++
      userInput ++
      List(git.cmd("checkout", ReleaseBranch))

  actions.reduce(_ #&& _) !!

  updatedState.log.info(s"Successfully merged $releaseTag to $ProductionBranch")

  updatedState
}
releaseProcess := Seq(
  ReleaseStep(verifyReleaseBranch),
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  ReleaseStep(mergeReleaseToMaster),
  setNextVersion,
  commitNextVersion,
  pushChanges
)

val viewCoverageResults = taskKey[Unit]("Opens the coverage result in the default browser")

viewCoverageResults := {
  val coverageResults =
    target.value.toPath.resolve(s"scala-${scalaBinaryVersion.value}/scoverage-report/index.html")

  Desktop.getDesktop.browse(coverageResults.toUri)
}

def environmentVariable(envName: String): Either[Exception, String] =
  sys.env.get(envName) match {
    case None => Left(new IllegalStateException(s"$envName is not defined as an environment variable"))
    case Some(envValue) => Right(envValue)
  }

addCommandAlias("cleanCompile", "; clean; compile")
addCommandAlias("cleanTest", "; clean; test")
addCommandAlias("testWithCoverage", "; clean; coverage; test; coverageAggregate; viewCoverageResults")
