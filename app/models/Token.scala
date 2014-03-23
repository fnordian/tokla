package models

import models.db.DbEntity
import org.squeryl.dsl.{CompositeKey2, ManyToMany, OneToMany, ManyToOne}
import org.squeryl.KeyedEntity

case class Token(override val id: String = java.util.UUID.randomUUID.toString, name: String, claimedBy: String = "", claimTime: Long = 0, picurl: String = "") extends DbEntity {
  lazy val applicants: OneToMany[TokenApplicant] = db.TokenDb.tokenToApplicants.left(this)

  def sortedApplicants: Array[TokenApplicant] = {
    applicants.toArray.sortWith((a: TokenApplicant, b: TokenApplicant) => {
      a.enqueueTime < b.enqueueTime
    })
  }

  lazy val user: OneToMany[User] = db.TokenDb.claimerToTokens.left(this)
  lazy val associatedUsers: ManyToMany[User, TokenUserReference] = db.TokenDb.tokenToUsers.left(this)
  lazy val teams: OneToMany[Team] = db.TokenDb.tokenTeams.left(this)

  def claimedByTeam = teams.find((team) => {
    team.members.exists((user: User) => user.id.equals(claimedBy))
  })

}

case class TokenApplicant(override val id: String = java.util.UUID.randomUUID().toString, tokenId: String, enqueueTime: Long = 0, applicantName: String) extends DbEntity {
  lazy val token: ManyToOne[Token] = db.TokenDb.tokenToApplicants.right(this)
}

case class TokenUserReference(tokenId: String, userName: String) extends KeyedEntity[CompositeKey2[String, String]] {
  def id = CompositeKey2(tokenId, userName)
}