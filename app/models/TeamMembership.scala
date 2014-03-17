package models


import org.squeryl.KeyedEntity
import org.squeryl.dsl.CompositeKey2

case class TeamMembership(val user: String, val team: String)  extends KeyedEntity[CompositeKey2[String, String]] {
  def id = CompositeKey2(user, team)
}