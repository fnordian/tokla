package models

import models.db.DbEntity
import org.squeryl.dsl.ManyToMany

case class User (override val id: String = java.util.UUID.randomUUID.toString, val firstname: String = "", val lastname: String = "") extends DbEntity {
  lazy val associatedTokens: ManyToMany[Token, TokenUserReference] = db.TokenDb.tokenToUsers.right(this)
}
