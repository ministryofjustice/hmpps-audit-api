package uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime
import java.util.*

@Entity(name = "AuditEmailAddress")
@Table(name = "audit_email_address")
@Schema(
  description = "Stores all email addresses for a given user." +
    "AuditUserId is the unique ID given by the audit service to each user." +
    "A user can have multiple records if their email address has been changed.",
)
data class AuditEmailAddress(
  @Id
  @GeneratedValue
  val id: Long = 0L,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "audit_user_id")
  val auditUser: AuditUser,

  val emailAddress: String,
  val active: Boolean = true,

  @CreationTimestamp
  val creationTime: LocalDateTime = LocalDateTime.now(),

  @UpdateTimestamp
  val lastModifiedTime: LocalDateTime? = LocalDateTime.now(),
)