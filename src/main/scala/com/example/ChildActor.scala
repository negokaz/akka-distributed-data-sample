package com.example

import akka.actor._
import akka.cluster.{Member, Cluster}
import akka.cluster.ClusterEvent.{CurrentClusterState, MemberUp, UnreachableMember, MemberEvent}

import scala.concurrent.duration._
import scala.util.Random

import ChildActor._
/**
  * 子どもの Actor
  *
  * 子どもはプレゼントが欲しいので、サンタクロースに自分の名前を書いた手紙を送る。
  */
class ChildActor extends LoggingFSM[State, Data] {

  val cluster = Cluster(context.system)

  // 名前をハッシュ値にする世界ということにしておく
  val myName = Integer.toHexString(hashCode)

  startWith(UnknownSantaClausVillage, SantaClausVillages(Set()))


  when(UnknownSantaClausVillage) {


    case Event(MemberUp(member), previous: SantaClausVillages) if member.hasRole("santa-claus-village") =>

      goto(KnowSantaClausVillage) using previous.copy(members = previous.members + member)
  }


  when(KnowSantaClausVillage) {


    case Event(MemberUp(member), previous: SantaClausVillages) if member.hasRole("santa-claus-village") =>

      stay() using previous.copy(members = previous.members + member)


    case Event(WriteLetter, data: SantaClausVillages) =>

      // サンタクロース村の郵便局
      val santaClausVillagePostOffice =
        context.actorSelection(RootActorPath(data.selectRandomly().address) / "user" / SantaClausActor.routerName)

      santaClausVillagePostOffice ! Letter(myName)

      log.info("I'm {}. I sent a letter to Santa Claus!", myName)

      stay()
  }


  whenUnhandled {


    case Event(gift: SantaClausActor.Gift, _) =>
      log.info("I'm {}. I received a gift! Thanks, Santa Claus! ;>", myName)
      stay()

    case Event(_: CurrentClusterState, _) => // ignore
      stay()

    case Event(_: MemberUp, _) => // ignore
      stay()
  }

  onTransition {

    case UnknownSantaClausVillage -> KnowSantaClausVillage =>
      setTimer("write-letter", WriteLetter, 5 + Random.nextInt(2) * 3 seconds)
  }

  override def preStart() = {

    cluster.subscribe(self, classOf[MemberUp])
  }

  initialize()

}

object ChildActor {

  def props() = Props(new ChildActor)

  /** 手紙を書くきっかけ */
  case object WriteLetter

  /** 手紙 */
  case class Letter(myName: String)

  sealed trait State

  /**
    * サンタクロース村を一つも知らない状態
    * 手紙を送れない
    */
  case object UnknownSantaClausVillage extends State

  /**
    * サンタクロース村を一つ以上知っている状態
    * 手紙を送れる
    */
  case object KnowSantaClausVillage extends State

  sealed trait Data
  case class SantaClausVillages(members: Set[Member]) extends Data {

    /**
      * @return ランダムに選択された Member
      */
    def selectRandomly(): Member = members.toSeq(Random.nextInt(members.size))
  }
}