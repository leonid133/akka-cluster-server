name := "server-node"
version := "0.0.1"
//scalaVersion := "2.10.6"
scalaVersion := "2.11.7"

//val akkaVersion = "2.4-SNAPSHOT"
val akkaVersion = "2.4.0"

libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
    "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
  "com.typesafe.akka" %% "akka-http-core-experimental" % "2.0.1",
  "com.typesafe.akka" %% "akka-http-experimental" % "2.0.1",
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % "2.0.1",
  "org.json4s" %% "json4s-native" % "3.2.10",
  "org.json4s" %% "json4s-jackson" % "3.2.10"

  )

resolvers += "Akka Snapshots" at "http://repo.akka.io/snapshots/"


