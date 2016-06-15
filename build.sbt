name := "server-node"
version := "0.0.1"
//scalaVersion := "2.10.6"
scalaVersion := "2.11.7"

//val akkaVersion = "2.4-SNAPSHOT"
val akkaVersion = "2.4.0"

libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
    "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion
  )

resolvers += "Akka Snapshots" at "http://repo.akka.io/snapshots/"


