package models.chat

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.actors.Actor._
import scala.actors.{TIMEOUT, Actor}
import scala.concurrent.duration._

import akka.util.Timeout
import play.Logger


case class RequestMessagesSince(sender: Actor, timeStamp: Long)
case class SayMessage(chatMessage: ChatMessage)

class ChatRoom {

  var messages = Seq[ChatMessage]()
  var listeners = Seq[Actor]()

  def messagesSince(timeStamp: Long) = {
    messages.filter(chatMessage => chatMessage.timeStamp > timeStamp)
  }

  val coordinator = actor {
    loop {
      react {
        case RequestMessagesSince(sender, timeStamp) => {
          messagesSince(timeStamp) match {
            case messages: Seq[ChatMessage] if messages.length > 0 =>
              sender ! messages
            case _ =>
              listeners = listeners :+ sender
          }
        }
        case SayMessage(chatMessage: ChatMessage) => {
          messages = messages :+ chatMessage
          Logger.info("informing all listeners")
          for (listener <- listeners) listener ! Seq(chatMessage)
          Logger.info("done informing all listeners")
          listeners = Seq()
        }
      }
    }
  }

  def sayMessage(chatMessage: ChatMessage) {
    coordinator ! SayMessage(chatMessage)
  }
}


class ChatRoomListener(chatRoom: ChatRoom, lastTimeListenTimeStamp: Long) {

  val listenActor = actor {
      react {

        case request: RequestMessagesSince => {
          chatRoom.coordinator ! request
          reply {
            receiveWithin(10000) {
              case response: Seq[ChatMessage] => {

                Logger.info("got response")
                response
              }
              case TIMEOUT =>
                null
            }
          }
        }
    }
  }

  def getMessages : Future[Seq[ChatMessage]] = {

    future {

      implicit val timeout = akka.util.Timeout(1 second)
      System.out.println("about to get messages")

      val messages : Seq[ChatMessage] = listenActor !? RequestMessagesSince(listenActor, lastTimeListenTimeStamp) match {
        case seq: Seq[ChatMessage] => seq
        case _ => Seq[ChatMessage]()
      }

      System.out.println("done getting messages " + messages)
      messages
    }
  }
}
