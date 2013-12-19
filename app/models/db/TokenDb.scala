package models.db

import org.squeryl.{KeyedEntity, Query, Schema}
import models.{TokenUserReference, TokenApplicant, Token, User}
import org.squeryl._
import org.squeryl.PrimitiveTypeMode._


object TokenDb extends Schema {

  def tokenToUsers = manyToManyRelation(tokens, users).via((t: Token, u: User, a: TokenUserReference) => (u.id === a.userName, t.id === a.tokenId))
  def userToTokens = manyToManyRelation(users, tokens).via((u: User, t: Token, a: TokenUserReference) => (u.id === a.userName, t.id === a.tokenId))


  def claimerToTokens = oneToManyRelation(tokens, users ).via((t, u) => u.id === t.claimedBy)

  val tokens = table[Token]
  val applicants = table[TokenApplicant]
  val users = table[User]
  val userReferences = table[TokenUserReference]

  val tokenToApplicants = oneToManyRelation(tokens, applicants).via((t, a) => t.id === a.tokenId)
  val applicantToUser = oneToManyRelation(users, applicants).via((u, a) => u.id === a.applicantName)
}
