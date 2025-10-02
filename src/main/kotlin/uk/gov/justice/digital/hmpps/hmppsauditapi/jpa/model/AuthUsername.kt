package uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity(name = "AuthUsername")
@Table(name = "auth_username", schema = "staff")
@Schema(
  description = "Stores all usernames for a given user." +
    "AUDIT_USER_ID is the unique ID given by the audit service to each user." +
    "A user can have multiple records if their username has been changed.",
)
data class AuthUsername(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
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
