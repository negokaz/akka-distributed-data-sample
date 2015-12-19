package com.example

import akka.actor.{Terminated, ActorSystem}
import akka.routing.FromConfig
import com.typesafe.config.ConfigFactory

import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration

object ApplicationMain extends App {

  val systemTerminated =
    if (!args.isEmpty && args.head == "city") {
      startCityNode()
    } else {
      startSantaClausVillageNode()
    }

  Await.result(systemTerminated, Duration.Inf)


  def startCityNode(): Future[Terminated] = {

    val system = ActorSystem("ChristmasSystem", ConfigFactory.load("node-city"))

    (1 to 3).foreach { i =>
      system.actorOf(ChildActor.props(), i.toString)
    }

    system.whenTerminated
  }

  def startSantaClausVillageNode(): Future[Terminated]  = {

    val system = ActorSystem("ChristmasSystem", ConfigFactory.load("node-santa-claus-village"))

    system.actorOf(FromConfig.props(SantaClausActor.props()), SantaClausActor.routerName)
    system.whenTerminated
  }

}
