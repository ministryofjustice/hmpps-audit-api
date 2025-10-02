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
@Table(name = "audit_user", schema = "staff")
@Schema(description = "Stores a unique ID for each user from auth")
data class AuditUser(
  @Id
  @GeneratedValue
  val id: UUID = UUID.randomUUID(),

  @OneToMany(mappedBy = "auditUser", cascade = [CascadeType.ALL], orphanRemoval = true)
  val userIds: List<AuthUserId>? = ArrayList(),

  @OneToMany(mappedBy = "auditUser", cascade = [CascadeType.ALL], orphanRemoval = true)
  val emailAddresses: List<AuthEmailAddress>? = ArrayList(),

  @OneToMany(mappedBy = "auditUser", cascade = [CascadeType.ALL], orphanRemoval = true)
  val usernames: List<AuthUsername>? = ArrayList(),

  @CreationTimestamp
  val creationTime: LocalDateTime = LocalDateTime.now(),

  @UpdateTimestamp
  val lastModifiedTime: LocalDateTime? = LocalDateTime.now(),
)
