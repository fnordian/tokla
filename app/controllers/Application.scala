package controllers

import play.api._
import play.api.mvc._
import models._
import models.db.TokenDb


import play.api.data.Forms._
import play.api.data.Form

import org.squeryl.{PrimitiveTypeMode, SessionFactory}
import scala.Predef._
import PrimitiveTypeMode._
import models.Token
import models.TokenApplicant


object Application extends Controller with LoggedIn with DbHelper {

  val tokenForm = Form(
    "name" -> nonEmptyText
  )

  val claimForm = Form(
    "submit" -> nonEmptyText
  )

  val tokenPictureForm = Form(
    "picurl" -> nonEmptyText
  )


  val schema = TokenDb

  def index = IsAuthenticated({

    username => implicit request =>
      withDbSessionNew(() => {
          val user = TokenDb.users.lookup(username) match {
            case Some(user) => user
            case none => Login.ensureUserDbEntry(User(username))
          }
          Ok(views.html.indexLoggedin(user))
      })
  }, {
    implicit request =>
      Ok(views.html.index("Your new application is ready."))
  })

  def learn = Action {
    implicit request =>
      Ok(views.html.index("Your new application is ready.")).flashing("message" -> "Tutorial comming soon. Thank you for your interest.")
  }


  def createTokenForm = IsAuthenticated {
    username => implicit request =>
      Ok(views.html.createTokenForm(tokenForm))
  }

  def createToken = IsAuthenticated {

    username => implicit request =>

      import schema._

      tokenForm.bindFromRequest.fold(
        errors => BadRequest(views.html.createTokenForm(tokenForm)),
        name => {
          withDbSessionNew(() => {
              val token = tokens.insert(Token(name = name, claimedBy = null))

              Redirect(routes.Application.showToken(token.id))
          })
        }
      )
  }

  def showToken(id: String) = IsAuthenticated {
    username => implicit request =>
      import schema._
      import PrimitiveTypeMode._

      Logger.info("show token " + id)

      withDbSessionNew(() => {

          val token = tokens.get(id)

          Logger.info("" + token)
          Logger.info(request.session.get("username").get + " " + (token.claimedBy == request.session.get("username").get))

          Ok(views.html.token(token, tokenPictureForm))
      })
  }

  def deenqueueForToken(id: String) = IsAuthenticated {
    username => implicit request =>
      import schema._
      import PrimitiveTypeMode._

      Logger.info("deenqueuing token " + id + " username " + username)

      withDbSessionNew(() => {
          val token = tokens.get(id)


          token.applicants.find((a: TokenApplicant) => {
            a.applicantName == username
          }).foreach((a: TokenApplicant) => schema.applicants.delete(a.id))


          Redirect(routes.Application.showToken(id))

      })
  }

  def enqueueForToken(id: String) = IsAuthenticated {
    username => implicit request =>
      import schema._
      import PrimitiveTypeMode._

      Logger.info("enqueuing token " + id + " username " + username)

      withDbSessionNew(() => {
          val token = tokens.get(id)

          val applicants = token.applicants
          if (applicants.exists((a: TokenApplicant) => {
            a.applicantName == username
          })) {
            Redirect(routes.Application.showToken(id))
          } else {

            val applicant = TokenApplicant(tokenId = token.id, enqueueTime = new java.util.Date().getTime, applicantName = username)

            schema.applicants.insert(applicant)

            Redirect(routes.Application.showToken(id))
          }

      })
  }

  def claimToken(id: String) = IsAuthenticated {
    username => implicit request =>

      import schema._
      import PrimitiveTypeMode._

      Logger.info("claiming token " + id + " username " + username)

      withDbSessionNew(() => {


        val token = tokens.get(id)

        if (!(token.claimedBy == null || token.claimedBy.isEmpty)) {

          Redirect(routes.Application.showToken(id)).flashing("message" -> "token already claimed")
        } else {


          Logger.info("token is now claimed by " + username + " " + token.id)

          val newToken = token.copy(claimedBy = username, claimTime = new java.util.Date().getTime)

          Logger.info("claiming token " + newToken)

          tokens.update(newToken)

          Redirect(routes.Application.showToken(id))


        }
      })

  }

  def disclaimer = Action {
    implicit request =>
      Ok(views.html.disclaimer())
  }

  def robotstxt = Action {
    implicit request =>
      Ok(views.txt.robots())
  }

  def releaseToken(id: String) = IsAuthenticated {
    username => implicit request =>
      import schema._
      import PrimitiveTypeMode._

      Logger.info("releasing token " + id + " username " + username)

      withDbSessionNew(() => {

          val token = tokens.get(id)

          if ((token.claimedBy == null || token.claimedBy.isEmpty)) {

            Redirect(routes.Application.showToken(id)).flashing("message" -> "token not claimed")
          } else if (!token.claimedBy.equals(username)) {
            Redirect(routes.Application.showToken(id)).flashing("message" -> "token claimed by someone else")
          } else {

            val applicants = token.sortedApplicants

            val nextClaimer = if (applicants.size > 0) {
              schema.applicants.delete(applicants(0).id)
              mail.MailNotification.sendTokenNotification(token.copy(claimedBy = applicants(0).applicantName))
              applicants(0).applicantName

            } else {
              ""
            }

            val newToken = token.copy(claimedBy = nextClaimer)

            Logger.info("token is now released")


            tokens.update(newToken)

            Redirect(routes.Application.showToken(id))

          }
      })

  }


}


trait DbHelper extends Controller {
  def withDbSession(f: => org.squeryl.Session => Result): Result = {

    val session = SessionFactory.newSession


    transaction {
      val result = f(session)


      result
    }


  }

  def withDbSessionNew[A](f: => () => A): A = {

    inTransaction {
      f()
    }
  }
}
