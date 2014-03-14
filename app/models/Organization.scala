package models

import models.db.DbEntity

case class Organization(override val id: String = java.util.UUID.randomUUID.toString, val name: String) extends DbEntity {

}
