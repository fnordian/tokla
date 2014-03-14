package controllers

import play.api.mvc.Controller
import models.db.TokenDb
import org.squeryl.PrimitiveTypeMode
import PrimitiveTypeMode._
import org.megaevil.dogeapi._

object Premium extends Controller with LoggedIn with DbHelper {

  val schema = TokenDb

  def showPremiumStatus = IsAuthenticated({
    username => implicit request =>
      withDbSession({
        implicit session =>
          val user = TokenDb.users.lookup(username).get
          val paymentAddresses = TokenDb.userPaymentAddress.left(user).seq.toList

          Ok(views.html.showPremiumStatus(username, paymentAddresses))
      })

  })


  def getNextUnusedPaymentAddress(user: models.User) = {

  }

}
