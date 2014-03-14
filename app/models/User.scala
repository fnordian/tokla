package models

import models.db.DbEntity
import org.squeryl.dsl.ManyToMany

case class User (id: String = java.util.UUID.randomUUID.toString, firstname: String = "", lastname: String = "") extends DbEntity {
  lazy val associatedTokens: ManyToMany[Token, TokenUserReference] = db.TokenDb.tokenToUsers.right(this)
}
