package models

import models.db.DbEntity

case class OrganizationMembership(override val id: String = java.util.UUID.randomUUID.toString, val user: String) extends DbEntity {

}
