package controllers

import models.User
import models.db.TokenDb
import org.squeryl.PrimitiveTypeMode._
import play.api._
import play.api.data.Forms._
import play.api.data._
import play.api.mvc._


import scala.concurrent.ExecutionContext.Implicits.global
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
    Action.async(request => {

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

  import play.api.Play.current

  val googleClientId = Play.configuration.getString("google.clientId").get
  val googleClientSecret = Play.configuration.getString("google.clientSecret").get

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


  def ensureUserDbEntry(user: User) = {
    Logger.debug("ensureUserDbEntry " + user.id)

    TokenDb.users.lookup(user.id) match {
      case None => TokenDb.users.insert(user)
      case result: Option[User] => {

      }
    }

    user

  }

  def newGoogleAction() = GoogleOAuthAction(googleClientId, googleClientSecret) {
    implicit request => implicit userinfo =>
      userinfo match {
        case Some(userinfo) =>
          withDbSessionNew(() => {

            val url: String = request.cookies.get("postLoginUrl").getOrElse(Cookie(name = "", value = routes.Application.index.absoluteURL())).value
            Logger.info("openid id " + userinfo.id)

            try {
              ensureUserDbEntry(User(id = userinfo.attributes.get("email").get, firstname = userinfo.attributes.get("given_name").getOrElse(""), lastname = userinfo.attributes.get("family_name").getOrElse("")))
              Redirect(url).withSession("username" -> userinfo.attributes.get("email").get)

            } catch {
              case e: Exception => {
                Logger.error("Unauthorized access to " + request.uri + ": " + e)
                throw new Exception;
              };
            }
          })
        case _ => Redirect(routes.Login.login("")).flashing("message" -> "authentication error")
      }
  }
}
