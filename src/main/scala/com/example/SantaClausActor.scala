package com.example

import akka.actor.{Props, ActorLogging, Actor}
import akka.cluster.Cluster
import akka.cluster.ddata.Replicator.WriteLocal
import akka.cluster.ddata._

/**
  * サンタクロースの Actor
  *
  * 子どもたちからサンタクロース村の郵便局経由で手紙を受け取る。
  * 郵便局から手紙は各サンタクロースに均等な量が届くように転送される。
  * サンタクロースは子どもたちが大好きなので、手紙に書いていた子どもたちの名前を全員分覚える。
  */
class SantaClausActor extends Actor with ActorLogging {
  import SantaClausActor._

  val replicator = DistributedData(context.system).replicator

  /** 子どもたちの名簿のキー */
  val childrenNameSetKey = GSetKey[String]("children-name-set")

  // 名前をハッシュ値にする世界ということにしておく
  val myName = Integer.toHexString(hashCode)

  def receive = {

    /** 子どもたちからの手紙を受け取り、手紙に書いてある名前を共有の名簿に追加する */
    case ChildActor.Letter(fromName) =>

      replicator ! Replicator.Update(childrenNameSetKey, GSet.empty[String], WriteLocal)(_ + fromName)

      log.info("Ho! Ho! Ho! I'm {}. I received a letter from {}!", myName, fromName)

      sender() ! Gift()

    /** 子どもたちの名簿の更新を受け取る */
    case c @ Replicator.Changed(key) if key == childrenNameSetKey =>
      val childrenNameSet = c.get(childrenNameSetKey)

      log.info("Ho! Ho! Ho! I'm {}. We received letters from {} children! {}", myName, childrenNameSet.elements.size, childrenNameSet)
  }

  override def preStart() = {

    replicator ! Replicator.Subscribe(childrenNameSetKey, self)
  }
}

object SantaClausActor {

  val routerName = "santa-claus-village-post-office"

  def props() = Props(new SantaClausActor)

  /** プレゼント */
  case class Gift()
}
