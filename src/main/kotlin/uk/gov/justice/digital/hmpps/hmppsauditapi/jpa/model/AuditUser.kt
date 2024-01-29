package uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.*
import jakarta.persistence.CascadeType.ALL
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.ArrayList

@Entity(name = "AuditUser")
@Table(name = "audit_user")
@Schema(
    description = "Stores all email addresses for a given user. " +
            "AuditUserId is the unique ID given by the audit service to each user. " +
            "A user can have multiple records if their email address has been changed."
)
data class AuditUser(
    @Id
    @GeneratedValue
    val id: UUID = UUID.randomUUID(),

    @OneToMany(mappedBy = "auditUser", cascade = [ALL], orphanRemoval = true)
    val userIds: List<AuditUserId>? = ArrayList(),

    @OneToMany(mappedBy = "auditUser", cascade = [ALL], orphanRemoval = true)
    val emailAddresses: List<AuditEmailAddress>? = ArrayList(),

    @OneToMany(mappedBy = "auditUser", cascade = [ALL], orphanRemoval = true)
    val usernames: List<AuditUsername>? = ArrayList(),

    @CreationTimestamp
    val creationTime: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    val lastModifiedTime: LocalDateTime? = LocalDateTime.now()
)
