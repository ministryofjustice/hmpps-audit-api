package uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.*
import jakarta.persistence.FetchType.LAZY
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity(name = "AuditUserId")
@Table(name = "audit_user_id")
@Schema(
  description = "Stores all user IDs for a given user." +
    "AuditUserId is the unique ID given by the audit service to each user." +
    "In rare cases, a user may have more than one user_id however most will have just one.",
)
data class AuditUserId(
  @Id
  @GeneratedValue
  val id: Long = 0L,

  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "audit_user_id")
  val auditUser: AuditUser,

  val userId: String,
  val active: Boolean = true,

  @CreationTimestamp
  val creationTime: LocalDateTime = LocalDateTime.now(),

  @UpdateTimestamp
  val lastModifiedTime: LocalDateTime? = LocalDateTime.now(),

  )
