package models.chat

import models.{Team, TokenApplicant, Token, User}

class ChatEvent() {
  var timeStamp: Long = System.currentTimeMillis
}

class ChatMessage(val sender: String, val message: String) extends ChatEvent {

}

class TokenUpdate(val applicants: Seq[TokenApplicant], val associatedUsers: Seq[User], val claimedBy: String, val claimedByTeamMembers: Seq[String], val claimedByTeamName: String, val claimTime: Long, val picurl: String) extends ChatEvent {
  def this(token: Token) = {

    this(
      token.sortedApplicants,
      token.associatedUsers.map(u => u).toSeq,
      token.claimedBy,
      token.claimedByTeam match {
        case None => Seq[String]()
        case team: Option[Team] => team.get.members.map((user) => user.id).toSeq
      },

      token.claimedByTeam match {
        case None => ""
        case team: Option[Team] => team.get.name
      },

      token.claimTime,
      token.picurl)
  }
}