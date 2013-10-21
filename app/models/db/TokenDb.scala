package models.db

import org.squeryl.{KeyedEntity, Query, Schema}
import models.{TokenApplicant, Token}
import org.squeryl._
import org.squeryl.PrimitiveTypeMode._



object TokenDb extends Schema {
  val tokens = table[Token]
  val applicants = table[TokenApplicant]

  val tokenToApplicants = oneToManyRelation(tokens, applicants).via((t, a) => t.id === a.tokenId)
}

