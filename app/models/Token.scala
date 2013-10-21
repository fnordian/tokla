package models

import models.db.DbEntity
import org.squeryl.dsl.{OneToMany, ManyToOne}

case class Token (override val id: String = java.util.UUID.randomUUID.toString, name: String, claimedBy: String = "", claimTime: Long = 0) extends DbEntity{
  lazy val applicants: OneToMany[TokenApplicant] = db.TokenDb.tokenToApplicants.left(this)
  def sortedApplicants: Array[TokenApplicant] = {
    applicants.toArray.sortWith((a: TokenApplicant, b: TokenApplicant) => {
      a.enqueueTime < b.enqueueTime
    })
  }
}

case class TokenApplicant (override val id: String = java.util.UUID.randomUUID().toString, tokenId: String, enqueueTime: Long = 0, applicantName: String) extends DbEntity {
  lazy val token: ManyToOne[Token] = db.TokenDb.tokenToApplicants.right(this)
}