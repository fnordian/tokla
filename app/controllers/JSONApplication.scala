package controllers

import play.api.mvc.{Action, Controller}
import models.db.TokenDb
import org.squeryl.PrimitiveTypeMode
import play.api.libs.json._
import play.api.Logger
import models.chat._
import models._
import controllers.mail.MailNotification
import play.api.libs.json.JsString
import play.api.libs.json.JsBoolean
import play.api.libs.json.JsNumber
import play.api.libs.json.JsObject
import models.db.TokenDb._
import play.api.libs.json.JsArray
import play.api.libs.json.JsString
import play.api.libs.json.JsBoolean
import play.api.libs.json.JsNumber
import play.api.libs.json.JsObject
import play.api.libs.json.JsArray
import play.api.libs.json.JsString
import play.api.libs.json.JsBoolean
import models.Token
import play.api.libs.json.JsNumber
import models.TokenApplicant
import models.TokenUserReference
import play.api.libs.json.JsObject

object JSONApplication extends Controller with LoggedIn with DbHelper {
  val schema = TokenDb

  def tokenClaimerTeam(team: Option[Team]): JsValue = {

    team match {
      case None => JsNull
      case team: Option[Team] => JsObject(Seq(
        "name" -> JsString(team.get.name),
        "members" -> JsArray(
          team.get.members.map((user: User) => JsString(user.id)).toSeq
        )
      ))
    }
  }

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
              "claimTime" -> JsNumber(token.claimTime),
              "claimedByTeam" -> tokenClaimerTeam(token.claimedByTeam)
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
          } else if (!token.claimedBy.equals(username) && !(token.claimedByTeam.isDefined && token.claimedByTeam.get.members.exists((user) => { user.id.equals(username) }))) {
            jsonError("token claimed by someone else")
          } else {

            val applicants = token.sortedApplicants

            val nextClaimer = if (applicants.size > 0) {
              schema.applicants.delete(applicants(0).id)
              MailNotification.sendTokenNotification(token.copy(claimedBy = applicants(0).applicantName))
              applicants(0).applicantName

            } else {
              null
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


  def getTokenPreferences(id: String) = IsAuthenticated {
    username => implicit request =>
      import schema._
      import PrimitiveTypeMode._
      withDbSession({
        implicit session =>
          val token = tokens.get(id)
          Logger.info("number of associatedUsers: " + token.associatedUsers.size)
          Ok(

            JsObject(Seq("success" -> JsBoolean(true), "preferences" -> JsObject(Seq(
              "picurl" -> JsString(token.picurl),
              "users" -> JsArray(token.associatedUsers.map((u: models.User) =>
                JsObject(Seq(
                  "id" -> JsString(u.id),
                  "team" -> JsObject(Seq(
                    "name" -> JsString({

                      Logger.info("getting teams for user " + u.id)

                      val teams = userToTeams.left(u).filter((t: Team) => {
                        Logger.info("checking if " + u.id + " is in " + t.name)
                        var ret = false;
                        token.teams.foreach((tokenTeam: Team) => {
                          Logger.info("comparing " + tokenTeam.name + " with " + t.name)
                          if (tokenTeam.id.equals(t.id)) {
                            ret = true
                          }
                        });
                        ret;
                      })

                      if (teams.isEmpty) {
                        null
                      } else {
                        teams.head.name
                      }

                    })).toSeq)
                ).toSeq)
              ).toSeq),
              "teams" -> JsArray(token.teams.map((team: models.Team) =>
                JsObject(Seq(
                  "name" -> JsString(team.name)).toSeq)
              ).toSeq)
            ))))
          )
      })
  }

  def userMaySetTokenPicture(token: models.Token, username: String) = {
    !(token.claimedBy == null || token.claimedBy.isEmpty || !token.claimedBy.equals(username))
  }

  def userMaySetTokenUser(token: models.Token, username: String) = {
    token.associatedUsers.exists((u: models.User) => u.id.equals(username))
  }

  def removeUserFromToken(token: models.Token, username: String) {
    import schema._
    import PrimitiveTypeMode._

    val tokenUserRereference = TokenUserReference(token.id, username)
    userReferences.delete(tokenUserRereference.id)
  }

  def addUserToToken(token: models.Token, username: String) {
    import schema._

    val user: models.User = new models.User(username);

    Login.ensureUserDbEntry(user)

    val tokenUserRereference = TokenUserReference(token.id, username)
    userReferences.insert(tokenUserRereference)
  }

  def addTeamToToken(token: Token, teamName: String) {
    import schema._


    val thisTokensTeams = tokenTeams.left(token)

    if (!thisTokensTeams.exists((team) => team.name.equals(teamName))) {
      val newTeam = new models.Team(token = token.id, name = teamName)
      teams.insert(newTeam)
    }
  }

  def removeTeamFromToken(token: Token, teamName: String) {
    val thisTokensTeams = tokenTeams.left(token)
    import PrimitiveTypeMode._

    thisTokensTeams.filter((team) => team.name.equals(teamName)).foreach((team) => {
      team.members.foreach((member) => {
        val membership = new TeamMembership(user = member.id, team = team.id);
        teamMembership.delete(membership.id)
      })
      teams.delete(team.id)
    })
  }

  def clearUserTokenTeam(token: Token, username: String) {
    import PrimitiveTypeMode._

    val thisTokensTeams = tokenTeams.left(token)

    thisTokensTeams.foreach((team) => {
      team.members.find((u: User) => u.id.equals(username)).foreach((u: User) => {
        val membership = new TeamMembership(user = username, team = team.id);
        teamMembership.delete(membership.id)
      })
    })
  }

  def addUserToTokenTeam(token: Token, username: String, teamName: String) {
    val thisTokensTeams = tokenTeams.left(token)

    import PrimitiveTypeMode._

    Logger.info("about to add " + username + " to " + teamName);

    thisTokensTeams.filter((team) => team.name.equals(teamName)).foreach((team) => {
      if (!team.members.exists((u: User) => u.id.equals(username))) {
        val membership = new TeamMembership(user = username, team = team.id);
        teamMembership.insert(membership)
      }
    })
  }

  def setTokenPreferences(id: String) = IsAuthenticated {
    username => implicit request =>
      import schema._
      import PrimitiveTypeMode._
      withDbSession({
        implicit session =>
          val token = tokens.get(id)

          val json = request.body.asJson.getOrElse(JsNull)

          Logger.info("body: " + (request.body))
          Logger.info("json: " + (json))
          Logger.info("json pictureUrl: " + (json \ "pictureUrl"))

          val picurl = (json \ "picurl").as[String]

          Logger.info("setting token picture " + id + " username " + username + " " + token.claimedBy + " " + token.claimedBy.equals(username))

          Logger.info("token about to get a new picture")

          if (!picurl.equals(token.picurl)) {

            if (userMaySetTokenPicture(token, username)) {
              val newToken = token.copy(picurl = picurl)

              Logger.info("token has got a new picture")
              tokens.update(newToken)

              Comet.tokenChatRoom(id).reportTokenUpdate(new TokenUpdate(tokens.get(id)))
            }

          }

          val tokenUsers = (json \ "users").as[Array[JsObject]]
          val tokenTeams = (json \ "teams").as[Array[JsObject]]

          // save user settings
          if (userMaySetTokenUser(token, username)) {
            Logger.info("user may set token user");

            tokenUsers.forall((user) => {

              if ((!(user \ "added").isInstanceOf[JsUndefined]) && ((user \ "removed").isInstanceOf[JsUndefined])) {
                Logger.info("new token user " + user \ "id")
                addUserToToken(token, (user \ "id").as[String])
              } else if (((user \ "added").isInstanceOf[JsUndefined]) && (!(user \ "removed").isInstanceOf[JsUndefined])) {
                if (!username.equals((user \ "id").as[String])) {
                  Logger.info("remove token user " + user \ "id")
                  removeUserFromToken(token, (user \ "id").as[String])
                }
              }




              uniqueTeams(tokenTeams.toList).forall((team) => {
                if ((!(team \ "added").isInstanceOf[JsUndefined]) && ((team \ "removed").isInstanceOf[JsUndefined])) {
                  addTeamToToken(token, (team \ "name").as[String])
                }
                true
              })

              val team = user \ "team"
              if ((team \ "name").isInstanceOf[JsUndefined]) {
                if (!(team \ "new").isInstanceOf[JsUndefined]) {
                  clearUserTokenTeam(token, (user \ "id").as[String])
                }
              } else {
                if (!(team \ "new").isInstanceOf[JsUndefined]) {
                  clearUserTokenTeam(token, (user \ "id").as[String])
                  addUserToTokenTeam(token, (user \ "id").as[String], (team \ "name").as[String])
                }
              }
              true;
            })

            uniqueTeams(tokenTeams.toList).forall((team) => {
              if (((team \ "added").isInstanceOf[JsUndefined]) && (!(team \ "removed").isInstanceOf[JsUndefined])) {
                removeTeamFromToken(token, (team \ "name").as[String])
              }
              true
            })
          }

          jsonOk()

      })

  }

  def uniqueTeams(ls: List[JsObject]) = {
    def loop(map: Map[String, JsObject], ls: List[JsObject]): List[JsObject] = ls match {
      case hd :: tail => hd :: loop(map + ((hd \ "name").as[String] -> hd), tail)
      case Nil => Nil
    }

    loop(Map(), ls).toSeq
  }
}
