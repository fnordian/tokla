package controllers

import play.api.mvc._
import play.api.libs.iteratee.{Enumeratee, Enumerator}
import scala.concurrent._
import ExecutionContext.Implicits.global
import play.api.templates.Html
import scala.actors._
import scala.actors.Actor._
import models.chat.{ChatMessage, ChatRoomListener, ChatRoom}
import java.util.concurrent.{ConcurrentHashMap, ConcurrentMap}
import play.api.http.Writeable
import play.api.data.Form
import play.api.data.Forms.{nonEmptyText, longNumber, tuple}
import models.chat._
import play.api.Logger
import play.api.libs.json._
import play.api.libs.json.JsObject
import scala.actors.!
import play.api.libs.json.JsString
import play.api.libs.json.JsBoolean
import play.api.libs.json.JsNumber


object Comet extends Controller with LoggedIn {

  val toCometMessage = Enumeratee.map[String] {
    data =>
      Html( """console.log('""" + data + """')""")
  }

  val chatForm = Form(
    "message" -> nonEmptyText
  )

  val tokenChatRooms: ConcurrentMap[String, ChatRoom] = new ConcurrentHashMap[String, ChatRoom]()

  def tokenChatRoom(id: String): ChatRoom = {
    tokenChatRooms.synchronized({
      if (!tokenChatRooms.containsKey(id)) {
        val newRoom = new ChatRoom()
        tokenChatRooms.put(id, newRoom)
        newRoom
      } else {
        tokenChatRooms.get(id)
      }
    })
  }

  def chatMessageToCometMessages(messages: Seq[ChatEvent], username: String) = {


    JsObject(Seq("success" -> JsBoolean(true), "messages" -> JsArray(
      messages.map(chatEvent => {
        chatEvent match {
          case chatMessage: ChatMessage =>

            JsObject(Seq(
              "sender" -> JsString(chatMessage.sender),
              "message" -> JsString(chatMessage.message),
              "timeStamp" -> JsNumber(chatMessage.timeStamp)
            ))
          case _ => JsNull
        }
      }
      )

    ), "tokenUpdates" -> JsArray(
      messages.map(chatEvent => {
        chatEvent match {
          case tokenUpdate: TokenUpdate =>

            JsObject(Seq(
              "applicants" -> JsArray(tokenUpdate.applicants.map(a => JsString(a.applicantName)).toSeq),
              "claimedBy" -> JsString(tokenUpdate.claimedBy),
              "claimTime" -> JsNumber(tokenUpdate.claimTime),
              "timeStamp" -> JsNumber(tokenUpdate.timeStamp),
              "picurl" -> JsString(tokenUpdate.picurl),
              "remembered" -> JsBoolean(tokenUpdate.associatedUsers.exists(u => u.id.equals(username))),
              "claimedByTeam" -> JsObject(Seq(
                "members" -> JsArray(
                  tokenUpdate.claimedByTeamMembers.map((username) => JsObject(Seq("id" -> JsString(username))))),
                "name" -> JsString(tokenUpdate.claimedByTeamName)
              ))))
          case _ => JsNull
        }
      }
      )

    )

    )
    )

  }

  def tokenEvents(id: String, lastMessageTimeStamp: Long) = IsAuthenticatedAsync({
    username => implicit request =>

      val room = tokenChatRoom(id)

      val messages = new ChatRoomListener(room, lastMessageTimeStamp).getMessages


      //val messagesHtml = messages.value.get.get.map( message => chatMessageToCometMessge(message))
      val messagesHtml = messages.map {
        value =>
          Ok(chatMessageToCometMessages(value, username))
      }

      messagesHtml

    /*
    val events = Enumerator("kiki", "foo", "bar")

    future {
      Ok.chunked(events >>> Enumerator.eof &> toCometMessage)
    }*/
  }, {
    implicit request => Ok("unauthenticated")
  }
  )

  def chatSay(id: String) = IsAuthenticated {
    username => implicit request =>
      chatForm.bindFromRequest.fold(
        errors => Ok(""),
        message => {

          val room = tokenChatRoom(id)
          room.sayMessage(new models.chat.ChatMessage(username, message))
          Ok("")


        }
      )
  }

}
