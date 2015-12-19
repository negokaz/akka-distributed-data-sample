name := """akka-replication-sample"""

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.7"

val akkaVersion = "2.4.1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor"                         % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j"                         % akkaVersion,
  "ch.qos.logback"    %  "logback-classic"                    % "1.1.3",
  "com.typesafe.akka" %% "akka-distributed-data-experimental" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit"                       % akkaVersion % "test",
  "org.scalatest"     %% "scalatest"                          % "2.2.4"     % "test"
    exclude("org.scala-lang", "scala-reflect")
)

lazy val runCity = TaskKey[Unit]("run-city", "Run a city node as cluster seed where a lot of children there")

javaOptions in runCity += "-Dakka.remote.netty.tcp.port=2550"

fullRunTask(runCity, Compile, "com.example.ApplicationMain", "city")

fullRunTask(TaskKey[Unit]("add-city", "Add a city node where a lot of children there"), Compile, "com.example.ApplicationMain", "city")

lazy val runSantaClausVillage = TaskKey[Unit]("run-santa-claus-village", "Run a santa claus village node")

javaOptions in runSantaClausVillage += "-Dakka.remote.netty.tcp.port=2551"

fullRunTask(runSantaClausVillage, Compile, "com.example.ApplicationMain", "santa-claus-village")

fullRunTask(TaskKey[Unit]("add-santa-claus-village", "Add a santa claus village node"), Compile, "com.example.ApplicationMain", "santa-claus-village")

fork := true