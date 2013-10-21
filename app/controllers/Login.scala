package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.openid._
import play.api.libs.concurrent.{Redeemed, Thrown}
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global

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

}

object Login extends Controller {

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

  def openIDCallback = Action {
    implicit request =>
      val url: String = request.cookies.get("postLoginUrl").getOrElse(Cookie(name = "", value = routes.Application.index.absoluteURL())).value
      AsyncResult(
        OpenID.verifiedId
          .map(info => Redirect(url).withSession("username" -> info.attributes.get("email").get))
          .recover {
          case t => {
            // Here you should look at the error, and give feedback to the user
            Redirect(routes.Login.login("")).flashing("message" -> "authentication error")
          }
        }
      )

  }


}
