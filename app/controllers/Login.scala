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
import scala.concurrent._

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

  def IsAuthenticatedAsync(f: => String => Request[AnyContent] => Future[SimpleResult], elseFun: => Request[AnyContent] => SimpleResult) =
    Action.async( request => {

        val username: String = request.session.get("username").getOrElse("")
        if (username.isEmpty) {
          future {
            elseFun(request)
          }
        } else {
          Logger.info("Authorized access to " + request.uri + " , for user " + username)
          f(username)(request)
        }
      }
    )




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

              Logger.info("openid id " + info.id)

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
