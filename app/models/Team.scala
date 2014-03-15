package models

import models.db.DbEntity

case class Team (override val id: String = java.util.UUID.randomUUID.toString, val token: String, val name: String) extends DbEntity {

}
