package models

import models.db.DbEntity

case class TeamMembership(override val id: String = java.util.UUID.randomUUID.toString, val user: String) extends DbEntity {

}
