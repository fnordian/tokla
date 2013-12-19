package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.openid._
import play.api.libs.concurrent.{Redeemed, Thrown}
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import models.db.TokenDb
import models.User

trait LoggedIn extends Controller {
  def IsAuthenticated(f: => String => Request[AnyContent] => Result): Action[AnyContent] = {

    Action({
      request => {
        val username: String = request.session.get("username").getOrElse("")
        if (username.isEmpty) {
          Logger.info("Unauthorized access to " + request.uri)
          Redirect(routes.Login.login(request.path))
        } else {
          Logger.info("Authorized access to " + request.uri + " , for user " + username)
          f(username)(request)
        }
      }
    })
  }


  def IsAuthenticated(f: => String => Request[AnyContent] => Result, elseFun: => Request[AnyContent] => Result): Action[AnyContent] = {

    Action({
      request => {
        val username: String = request.session.get("username").getOrElse("")
        if (username.isEmpty) {
          elseFun(request)
        } else {
          Logger.info("Authorized access to " + request.uri + " , for user " + username)
          f(username)(request)
        }
      }
    })
  }

}

object Login extends Controller with DbHelper {

  val loginForm = Form(
    "url" -> nonEmptyText
  )

  def logout = Action {
    implicit request =>
      Redirect(routes.Application.index).withNewSession
  }

  def login(postLoginPath: String) = Action {
    implicit request =>
      if (postLoginPath.isEmpty) {
        Ok(views.html.loginform(loginForm))
      } else {
        Ok(views.html.loginform(loginForm)).withCookies(Cookie(name = "postLoginUrl", value = postLoginPath))
      }
  }

  def loginWithGoogle = Action {
    implicit request =>
      val openid = "https://www.google.com/accounts/o8/id";
      AsyncResult(OpenID.redirectURL(openid, routes.Login.openIDCallback.absoluteURL(), Seq("email" -> "http://schema.openid.net/contact/email"))
        .map(url => Redirect(url))
        .recover {
        case error => Redirect(routes.Login.login(""))
      }).flashing("message" -> "welcome")

  }

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


object Comet extends Controller with LoggedIn {

  val toCometMessage = Enumeratee.map[String] { data =>
    Html("""console.log('""" + data + """')""")
  }

  val chatForm = Form(
    "message" -> nonEmptyText
  )


  val tokenChatRooms : ConcurrentMap[String, ChatRoom] = new ConcurrentHashMap[String, ChatRoom]()

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

    val sb: StringBuilder = new StringBuilder()



    if(messages.length > 0) {
      for (chatMessage <- messages) {
        sb.append("""newChatLine('""" + chatMessage.sender + """', '""" + chatMessage.message + """')""")
      }
    } else {
      sb.append("""console.log('""" + "no messages" + """')""")
    }

    Html(sb.toString())

  }

  def tokenEvents(id: String) = Action.async {

    val room = tokenChatRoom(id)

    val messages = new ChatRoomListener(room, 0).getMessages


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
          errors => BadRequest("bad form"),
          message => {
            val room = tokenChatRoom(id)
            room.sayMessage(new ChatMessage("", message))
            Ok("")

          }
        )
  }
}


  def loginPost = Action {
    implicit request =>
      Form(single(
        "openid" -> nonEmptyText
      )).bindFromRequest.fold(
      error => {
        Logger.info("bad request " + error.toString)
        BadRequest(error.toString)
      }, {
        case (openid) => AsyncResult(OpenID.redirectURL(openid, routes.Login.openIDCallback.absoluteURL(), Seq("email" -> "http://schema.openid.net/contact/email"))
          .map(url => Redirect(url))
          .recover {
          case error => Redirect(routes.Login.login(""))
        })
      }
      )

  }

  def ensureUserDbEntry(user: User) = {
    TokenDb.users.insertOrUpdate(user)
    user
  }

  def openIDCallback = Action {
    implicit request =>
      val url: String = request.cookies.get("postLoginUrl").getOrElse(Cookie(name = "", value = routes.Application.index.absoluteURL())).value
      AsyncResult(
        OpenID.verifiedId
          .map(info =>
          withDbSession({
            implicit session =>

              try {
                ensureUserDbEntry(User(id = info.attributes.get("email").get, firstname = info.attributes.get("firstname").getOrElse(""), lastname = info.attributes.get("lastname").getOrElse("")))
                Redirect(url).withSession("username" -> info.attributes.get("email").get)

              } catch {
                case e: Exception => {
                  Logger.error("Unauthorized access to " + request.uri + ": " + e)
                  throw new Exception;
                };
              }
          }))
          .recover {
          case t => {
            // Here you should look at the error, and give feedback to the user
            Redirect(routes.Login.login("")).flashing("message" -> "authentication error")
          }
        }
      )
  }
}
