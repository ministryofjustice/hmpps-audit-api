package uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity(name = "AuditUsername")
@Table(name = "audit_username")
@Schema(
  description = "Stores all usernames for a given user." +
    "AuditUserId is the unique ID given by the audit service to each user." +
    "A user can have multiple records if their username has been changed.",
)
data class AuditUsername (
  @Id
  @GeneratedValue
  val id: Long = 0L,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "audit_user_id")
  val auditUser: AuditUser,

  val username: String,
  val active: Boolean = true,

  @CreationTimestamp
  val creationTime: LocalDateTime = LocalDateTime.now(),

  @UpdateTimestamp
  val lastModifiedTime: LocalDateTime? = LocalDateTime.now(),
)