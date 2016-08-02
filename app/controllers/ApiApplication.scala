package controllers

import models._
import models.db.TokenDb
import org.squeryl.PrimitiveTypeMode
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.{Action, Controller, Result}

object ApiApplication extends Controller with DbHelper {
  val schema = TokenDb

  def withValidApiKey(tokenId: String, apiKey: String, fn: => (Token) => Result): Result = {
    import PrimitiveTypeMode._
    import schema._

    val token = tokens.get(tokenId)

    Logger.debug("api claim " + token.apikey + " " + apiKey)
    Logger.debug("api claim " + token.apikey.equals(apiKey))


    if (token.apikey.equals(apiKey)) {
      fn(token)
    } else {
      JSONApplication.jsonError("invalid api key")
    }
  }

  def claimToken(tokenId: String, apiKey: String) = Action {
    implicit request =>
      val json = request.body.asJson.getOrElse(JsNull)

      val username = (json \ "username").as[String]

      withDbSessionNew(() => {
        withValidApiKey(tokenId, apiKey, (token) => {
          if (JSONApplication.claimTokenDb(tokenId, username)) {
            JSONApplication.jsonOk()
          } else {
            JSONApplication.jsonError("already claimed")
          }
        })
      }

      );
  }

  def releaseToken(tokenId: String, apiKey: String) = Action {
    implicit request =>
      val json = request.body.asJson.getOrElse(JsNull)
      val username = (json \ "username").as[String]

      withDbSessionNew(() => {
        withValidApiKey(tokenId, apiKey, (token) => {
          JSONApplication.releaseTokenDb(tokenId, username) match {
            case 'Ok => JSONApplication.jsonOk()
            case 'ClaimedBySomeOneElse => JSONApplication.jsonError("claimed by someone else")
            case 'NotClaimed => JSONApplication.jsonError("token not claimed")
          }
        })
      });
  }


}