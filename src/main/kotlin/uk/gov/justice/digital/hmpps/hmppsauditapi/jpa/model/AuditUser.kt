package uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime
import java.util.UUID
import kotlin.collections.List

@Entity(name = "AuditUser")
@Table(name = "audit_user")
@Schema(
  description = "Stores all email addresses for a given user. " +
    "AuditUserId is the unique ID given by the audit service to each user. " +
    "A user can have multiple records if their email address has been changed.",
)
data class AuditUser(
  @Id
  @GeneratedValue
  val id: UUID = UUID.randomUUID(),

  @OneToMany(mappedBy = "auditUser", cascade = [CascadeType.ALL], orphanRemoval = true)
  val userIds: List<AuditUserId>? = ArrayList(),

  @OneToMany(mappedBy = "auditUser", cascade = [CascadeType.ALL], orphanRemoval = true)
  val emailAddresses: List<AuditEmailAddress>? = ArrayList(),

  @OneToMany(mappedBy = "auditUser", cascade = [CascadeType.ALL], orphanRemoval = true)
  val usernames: List<AuditUsername>? = ArrayList(),

  @CreationTimestamp
  val creationTime: LocalDateTime = LocalDateTime.now(),

  @UpdateTimestamp
  val lastModifiedTime: LocalDateTime? = LocalDateTime.now(),
)
