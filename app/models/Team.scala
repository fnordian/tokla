package models

import models.db.DbEntity
import org.squeryl.dsl.ManyToMany

case class Team (override val id: String = java.util.UUID.randomUUID.toString, val token: String, val name: String) extends DbEntity {
  lazy val members: ManyToMany[User, TeamMembership] = db.TokenDb.userToTeams.right(this)
}
