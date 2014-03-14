package models

import models.db.DbEntity

case class PaymentAddress(override val id: String, val username: String) extends DbEntity{

}
