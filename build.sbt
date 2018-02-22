import sbt.Keys._

val akkaVersion = "2.5.9"
val akkaHttpVersion = "10.0.11"
val circeVersion = "0.9.1"

lazy val root = (project in file("."))
  .enablePlugins(BuildInfoPlugin)
  .settings(

    organization := "prowse.github.cosm1c",

    name := "job-runner",

    version := "1.0",

    scalaVersion := "2.12.4",

    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
      "com.github.swagger-akka-http" %% "swagger-akka-http" % "0.12.0",
      "ch.megard" %% "akka-http-cors" % "0.2.2",

      "io.projectreactor" % "reactor-core" % "3.1.4.RELEASE",
      "io.projectreactor.addons" % "reactor-adapter" % "3.1.5.RELEASE",

      "de.heikoseeberger" %% "akka-http-circe" % "1.19.0",
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "io.circe" % "circe-java8_2.12" % circeVersion,

      "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",
      "ch.qos.logback" % "logback-classic" % "1.2.3" % Runtime,
      "org.slf4j" % "jul-to-slf4j" % "1.7.25",

      "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
      "org.scalatest" %% "scalatest" % "3.0.4" % Test,
      "org.mockito" % "mockito-all" % "1.10.19" % Test
    ),

    // Inspried by https://tpolecat.github.io/2017/04/25/scalac-flags.html
    scalacOptions ++= Seq(
      "-target:jvm-1.8",
      "-deprecation", // Emit warning and location for usages of deprecated APIs.
      "-encoding", "utf-8", // Specify character encoding used by source files.
      "-explaintypes", // Explain type errors in more detail.
      "-feature", // Emit warning and location for usages of features that should be imported explicitly.
      //"-language:existentials", // Existential types (besides wildcard types) can be written and inferred
      //"-language:experimental.macros", // Allow macro definition (besides implementation and application)
      //"-language:higherKinds", // Allow higher-kinded types
      //"-language:implicitConversions", // Allow definition of implicit functions called views
      "-unchecked", // Enable additional warnings where generated code depends on assumptions.
      //"-Xcheckinit", // Wrap field accessors to throw an exception on uninitialized access.
      "-Xfatal-warnings", // Fail the compilation if there are any warnings.
      //"-Xfuture", // Turn on future language features.
      "-Xlint:adapted-args", // Warn if an argument list is modified to match the receiver.
      "-Xlint:by-name-right-associative", // By-name parameter of right associative operator.
      "-Xlint:constant", // Evaluation of a constant arithmetic expression results in an error.
      "-Xlint:delayedinit-select", // Selecting member of DelayedInit.
      "-Xlint:doc-detached", // A Scaladoc comment appears to be detached from its element.
      "-Xlint:inaccessible", // Warn about inaccessible types in method signatures.
      "-Xlint:infer-any", // Warn when a type argument is inferred to be `Any`.
      "-Xlint:missing-interpolator", // A string literal appears to be missing an interpolator id.
      "-Xlint:nullary-override", // Warn when non-nullary `def f()' overrides nullary `def f'.
      "-Xlint:nullary-unit", // Warn when nullary methods return Unit.
      "-Xlint:option-implicit", // Option.apply used implicit view.
      "-Xlint:package-object-classes", // Class or object defined in package object.
      "-Xlint:poly-implicit-overload", // Parameterized overloaded implicit methods are not visible as view bounds.
      "-Xlint:private-shadow", // A private field (or class parameter) shadows a superclass field.
      "-Xlint:stars-align", // Pattern sequence wildcard must align with sequence component.
      "-Xlint:type-parameter-shadow", // A local type parameter shadows a type already in scope.
      "-Xlint:unsound-match", // Pattern match may not be typesafe.
      "-Yno-adapted-args", // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
      "-Ypartial-unification", // Enable partial unification in type constructor inference
      "-Ywarn-dead-code", // Warn when dead code is identified.
      "-Ywarn-extra-implicit", // Warn when more than one implicit parameter section is defined.
      "-Ywarn-inaccessible", // Warn about inaccessible types in method signatures.
      "-Ywarn-infer-any", // Warn when a type argument is inferred to be `Any`.
      "-Ywarn-nullary-override", // Warn when non-nullary `def f()' overrides nullary `def f'.
      "-Ywarn-nullary-unit", // Warn when nullary methods return Unit.
      "-Ywarn-numeric-widen", // Warn when numerics are widened.
      "-Ywarn-unused:implicits", // Warn if an implicit parameter is unused.
      "-Ywarn-unused:imports", // Warn if an import selector is not referenced.
      "-Ywarn-unused:locals", // Warn if a local definition is unused.
      "-Ywarn-unused:params", // Warn if a value parameter is unused.
      "-Ywarn-unused:patvars", // Warn if a variable bound in a pattern is unused.
      "-Ywarn-unused:privates", // Warn if a private member is unused.
      "-Ywarn-value-discard" // Warn when non-Unit expression results are unused.
    ),

    // REPL can’t cope with these
    scalacOptions in(Compile, console) --= Seq("-Ywarn-unused:imports", "-Xfatal-warnings"),

    javacOptions ++= Seq(
      "-source", "1.8",
      "-target", "1.8"
    ),

    // Build Info
    buildInfoOptions += BuildInfoOption.ToJson,
    buildInfoPackage := "prowse",
    buildInfoKeys := Seq[BuildInfoKey](
      name,
      version,
      scalaVersion,
      sbtVersion,
      BuildInfoKey.action("buildInstant") {
        java.time.format.DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.now())
      },
      "gitChecksum" -> {
        git.gitHeadCommit
      })
  )

//Warts.unsafe only to avoid false positives
wartremoverErrors ++= List(
  // TODO: use Wart.Any when switched to typed Akka
  //  Wart.Any,
  Wart.AsInstanceOf,
  //  Wart.DefaultArguments,
  Wart.EitherProjectionPartial,
  // Wart.IsInstanceOf,
  Wart.TraversableOps,
  //  Wart.NonUnitStatements,
  Wart.Null,
  Wart.OptionPartial,
  Wart.Product,
  Wart.Return,
  Wart.Serializable,
  Wart.StringPlusAny,
  Wart.Throw,
  Wart.TryPartial,
  Wart.Var
)

wartremoverExcluded += sourceManaged.value / "main" / "sbt-buildinfo" / "BuildInfo.scala"
//wartremoverExcluded ++= ((sourceManaged.value / "main") ** "*.scala").get

//scalacOptions ++= scalafixScalacOptions.value
