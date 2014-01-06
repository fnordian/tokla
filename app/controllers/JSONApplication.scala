package controllers

import play.api.mvc.{SimpleResult, Action, Controller}
import models.db.TokenDb
import play.api.libs.json.Json._
import org.squeryl.PrimitiveTypeMode
import play.api.libs.json._
import play.api.Logger
import models.chat._
import models.{TokenApplicant, TokenUserReference}
import controllers.mail.MailNotification
import play.api.libs.json.JsString
import play.api.libs.json.JsBoolean
import play.api.libs.json.JsNumber
import models.TokenApplicant
import models.TokenUserReference
import play.api.libs.json.JsObject

object JSONApplication extends Controller with LoggedIn with DbHelper {
  val schema = TokenDb

  def showToken(id: String) = Action {
    implicit request =>
      withDbSession({
        implicit session =>
          import schema._
          import PrimitiveTypeMode._

          val token = tokens.get(id)

          Ok(

            JsObject(Seq("success" -> JsBoolean(true), "token" -> JsObject(Seq(

              "id" -> JsString(token.id),
              "claimedBy" -> JsString(token.claimedBy),
              "claimTime" -> JsNumber(token.claimTime)
            ))
            )))

      })

  }

  def jsonError(message: String) = {
    Ok(JsObject(Seq("success" -> JsBoolean(false), "message" -> JsString(message))))
  }

  def jsonOk() = {
    Ok(JsObject(Seq("success" -> JsBoolean(true))))
  }

  def claimToken(id: String) = IsAuthenticated {
    username => implicit request =>

      import schema._
      import PrimitiveTypeMode._

      Logger.info("claiming token " + id + " username " + username)

      withDbSession({
        implicit session =>


          val token = tokens.get(id)

          if (!(token.claimedBy == null || token.claimedBy.isEmpty)) {
            jsonError("token already claimed")
          } else {
            Logger.info("token is now claimed by " + username + " " + token.id)

            val newToken = token.copy(claimedBy = username, claimTime = new java.util.Date().getTime)

            Logger.info("claiming token " + newToken)

            tokens.update(newToken)

            Comet.tokenChatRoom(id).reportTokenUpdate(new TokenUpdate(newToken))

            jsonOk()
          }
      })

  }

  def releaseToken(id: String) = IsAuthenticated {
    username => implicit request =>
      import schema._
      import PrimitiveTypeMode._

      Logger.info("releasing token " + id + " username " + username)

      withDbSession({
        implicit session =>

          val token = tokens.get(id)

          if ((token.claimedBy == null || token.claimedBy.isEmpty)) {
            jsonError("token not claimed")
          } else if (!token.claimedBy.equals(username)) {
            jsonError("token claimed by someone else")
          } else {

            val applicants = token.sortedApplicants

            val nextClaimer = if (applicants.size > 0) {
              schema.applicants.delete(applicants(0).id)
              MailNotification.sendTokenNotification(token.copy(claimedBy = applicants(0).applicantName))
              applicants(0).applicantName

            } else {
              ""
            }

            val newToken = token.copy(claimedBy = nextClaimer)

            Logger.info("token is now released")


            tokens.update(newToken)

            Comet.tokenChatRoom(id).reportTokenUpdate(new TokenUpdate(newToken))
            jsonOk()

          }
      })

  }

  def deenqueueForToken(id: String) = IsAuthenticated {
    username => implicit request =>
      import schema._
      import PrimitiveTypeMode._

      Logger.info("deenqueuing token " + id + " username " + username)

      withDbSession({
        implicit session =>
          val token = tokens.get(id)


          token.applicants.find((a: TokenApplicant) => {
            a.applicantName == username
          }).foreach((a: TokenApplicant) => schema.applicants.delete(a.id))

          Comet.tokenChatRoom(id).reportTokenUpdate(new TokenUpdate(token))

          jsonOk()

      })
  }

  def enqueueForToken(id: String) = IsAuthenticated {
    username => implicit request =>
      import schema._
      import PrimitiveTypeMode._

      Logger.info("enqueuing token " + id + " username " + username)

      withDbSession({
        implicit session =>
          val token = tokens.get(id)

          val applicants = token.applicants
          if (applicants.exists((a: TokenApplicant) => {
            a.applicantName == username
          })) {
            jsonOk()
          } else {

            val applicant = TokenApplicant(tokenId = token.id, enqueueTime = new java.util.Date().getTime, applicantName = username)

            schema.applicants.insert(applicant)

            Comet.tokenChatRoom(id).reportTokenUpdate(new TokenUpdate(token))

            jsonOk()
          }


      })
  }

  def rememberToken(id: String) = IsAuthenticated {
    username => implicit request =>
      import schema._
      import PrimitiveTypeMode._

      Logger.info("remembering token " + id + " username " + username)

      withDbSession({
        implicit session =>
          val tokenUserRereference = TokenUserReference(id, username)
          userReferences.insertOrUpdate(tokenUserRereference)

          Comet.tokenChatRoom(id).reportTokenUpdate(new TokenUpdate(tokens.get(id)))
          jsonOk()
      })

  }

  def forgetToken(id: String) = IsAuthenticated {
    username => implicit request =>
      import schema._
      import PrimitiveTypeMode._

      Logger.info("forgetting token " + id + " username " + username)

      withDbSession({
        implicit session =>

          val tokenUserRereference = TokenUserReference(id, username)
          Logger.info("forgetting token " + tokenUserRereference)
          Logger.info("forgetting token " + tokenUserRereference.id)
          Logger.info("forgetting token " + userReferences)


          userReferences.delete(tokenUserRereference.id)
          Comet.tokenChatRoom(id).reportTokenUpdate(new TokenUpdate(tokens.get(id)))
          jsonOk()
      })

  }



  def setTokenPicture(id: String) = IsAuthenticated {
    username => implicit request =>
      import schema._
      import PrimitiveTypeMode._
      withDbSession({
        implicit session =>
          val token = tokens.get(id)

          val json = request.body.asJson.getOrElse(JsNull)

          Logger.info("body: "+ (request.body))
          Logger.info("json: "+ (json))
          Logger.info("json pictureUrl: "+ (json \ "pictureUrl"))

          val picurl = (json \ "pictureUrl").as[String]

          Logger.info("setting token picture " + id + " username " + username + " " + token.claimedBy + " " + token.claimedBy.equals(username))

              Logger.info("token about to get a new picture")

              if ((token.claimedBy == null || token.claimedBy.isEmpty || !token.claimedBy.equals(username))) {
                jsonError("token not claimed by you")
              } else {
                val newToken = token.copy(picurl = picurl)

                Logger.info("token has got a new picture")
                tokens.update(newToken)

                Comet.tokenChatRoom(id).reportTokenUpdate(new TokenUpdate(tokens.get(id)))

                jsonOk()
              }

      })

  }
}
