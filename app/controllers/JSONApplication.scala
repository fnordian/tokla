package controllers

import play.api.mvc.{Action, Controller}
import models.db.TokenDb
import play.api.libs.json.Json._
import org.squeryl.PrimitiveTypeMode
import play.api.libs.json.{JsNumber, JsString, JsBoolean, JsObject}


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

}
