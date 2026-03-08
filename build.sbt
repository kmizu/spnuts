import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType, JVMPlatform}
import scalanativecrossproject.ScalaNativeCrossPlugin.autoImport.NativePlatform

ThisBuild / scalaVersion := "3.3.1"
ThisBuild / organization := "org.spnuts"
ThisBuild / version      := "2.0.0-SNAPSHOT"

ThisBuild / scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-language:implicitConversions",
)

// ── Core (shared + JVM platform + Native platform) ──────────────────────────
lazy val core = crossProject(JVMPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .in(file("core"))
  .settings(
    name := "spnuts-core",
    libraryDependencies ++= Seq(
      "org.scalatest"     %%% "scalatest"  % "3.2.19"  % Test,
      "org.scalatestplus" %%% "scalacheck-1-18" % "3.2.19.0" % Test,
    ),
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "org.ow2.asm" % "asm"      % "9.7.1",
      "org.ow2.asm" % "asm-util" % "9.7.1",
    ),
  )
  .nativeSettings(
    nativeConfig ~= { c =>
      c.withLTO(scala.scalanative.build.LTO.none)
       .withMode(scala.scalanative.build.Mode.debug)
       .withGC(scala.scalanative.build.GC.immix)
    },
  )

lazy val coreJVM    = core.jvm
lazy val coreNative = core.native

// ── REPL ─────────────────────────────────────────────────────────────────────
lazy val repl = crossProject(JVMPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .in(file("repl"))
  .dependsOn(core)
  .settings(name := "spnuts-repl")
  .jvmSettings(
    libraryDependencies += "org.jline" % "jline" % "3.27.1",
    Compile / mainClass := Some("spnuts.repl.Main"),
  )
  .nativeSettings(
    Compile / mainClass := Some("spnuts.repl.Main"),
  )

lazy val replJVM    = repl.jvm
lazy val replNative = repl.native

// ── Root ─────────────────────────────────────────────────────────────────────
lazy val root = (project in file("."))
  .aggregate(coreJVM, coreNative, replJVM, replNative)
  .settings(
    name := "spnuts-scala",
    publish / skip := true,
  )
