package models

import models.db.DbEntity

case class PremiumPayments (override val id: String = java.util.UUID.randomUUID.toString, val username: String, val payTime: Long = 0, val payAmount: Long, val paymentAddress: String) extends DbEntity {

}
