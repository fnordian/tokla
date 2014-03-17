package models.db

import org.squeryl.{KeyedEntity, Query, Schema}
import models._
import org.squeryl._
import org.squeryl.PrimitiveTypeMode._
import models.Token
import models.User
import models.TokenApplicant
import models.TokenUserReference


object TokenDb extends Schema {

  def tokenToUsers = manyToManyRelation(tokens, users).via((t: Token, u: User, a: TokenUserReference) => (u.id === a.userName, t.id === a.tokenId))
  def userToTokens = manyToManyRelation(users, tokens).via((u: User, t: Token, a: TokenUserReference) => (u.id === a.userName, t.id === a.tokenId))
  def userToTeams = manyToManyRelation(users, teams).via((u: User, t: Team, a: TeamMembership) => (u.id === a.user, t.id === a.team))


  def claimerToTokens = oneToManyRelation(tokens, users ).via((t, u) => u.id === t.claimedBy)

  val tokens = table[Token]
  val applicants = table[TokenApplicant]
  val users = table[User]
  val teams = table[Team]
  val userReferences = table[TokenUserReference]
  val userPremiumPayments = table[PremiumPayments]
  val paymentAddress = table[PaymentAddress]
  val teamMembership = table[TeamMembership]
  

  val tokenToApplicants = oneToManyRelation(tokens, applicants).via((t, a) => t.id === a.tokenId)
  val applicantToUser = oneToManyRelation(users, applicants).via((u, a) => u.id === a.applicantName)
  val userPaymentAddress = oneToManyRelation(users, paymentAddress).via((u, p) => u.id === p.username)
  val tokenTeams = oneToManyRelation(tokens, teams).via((token, team) => token.id === team.token)
}
