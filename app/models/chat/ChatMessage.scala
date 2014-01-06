package models.chat

import models.{TokenApplicant, Token, User}

class ChatEvent() {
  var timeStamp: Long = System.currentTimeMillis
}

class ChatMessage(val sender : String, val message : String) extends ChatEvent {

}

class TokenUpdate(val applicants : Seq[TokenApplicant], val associatedUsers: Seq[User], val claimedBy: String, val claimTime: Long, val picurl: String) extends ChatEvent {
  def this(token: Token) = {
    this(token.sortedApplicants, token.associatedUsers.map(u => u).toSeq, token.claimedBy, token.claimTime, token.picurl)
  }
}