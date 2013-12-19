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

  def chatMessageToCometMessages(messages: Seq[ChatMessage]) = {


    JsObject(Seq("success" -> JsBoolean(true), "messages" -> JsArray(
      messages.map(chatMessage =>
        JsObject(Seq(
          "sender" -> JsString(chatMessage.sender),
          "message" -> JsString(chatMessage.message),
          "timeStamp" -> JsNumber(chatMessage.timeStamp)
        ))
      )

    ))
    )

    /*
        if (messages.length > 0) {
          for (chatMessage <- messages) {
            sb.append( """newChatLine('""" + Html(chatMessage.sender) + """', '""" + Html(chatMessage.message) + """', """ + chatMessage.timeStamp + """);""")
          }
        } else {
          sb.append( """console.log('""" + "no messages" + """')""")
        }

        Html(sb.toString())
    */
  }

  def tokenEvents(id: String, lastMessageTimeStamp: Long) = Action.async {

    val room = tokenChatRoom(id)

    val messages = new ChatRoomListener(room, lastMessageTimeStamp).getMessages


    //val messagesHtml = messages.value.get.get.map( message => chatMessageToCometMessge(message))
    val messagesHtml = messages.map {
      value =>
        Ok(chatMessageToCometMessages(value))
    }

    messagesHtml

    /*
    val events = Enumerator("kiki", "foo", "bar")

    future {
      Ok.chunked(events >>> Enumerator.eof &> toCometMessage)
    }*/
  }

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
