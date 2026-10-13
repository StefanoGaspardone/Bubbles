package com.backend.models.entities

import jakarta.persistence.*
import org.hibernate.Hibernate
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "refresh_tokens")
class RefreshToken(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Column(name = "token_hash", nullable = false, unique = true)
    var tokenHash: String,

    @Column(name = "family_id", nullable = false, updatable = false)
    val familyId: UUID,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant,

    @Column(name = "revoked_at", nullable = true)
    var revokedAt: Instant? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {

    fun isRevoked(): Boolean =
        revokedAt != null

    fun isExpired(): Boolean =
        !Instant.now().isBefore(expiresAt)

    fun isUsable(): Boolean =
        !isRevoked() && !isExpired()

    fun revoke() {
        if(revokedAt == null) revokedAt = Instant.now()
    }

    override fun equals(other: Any?): Boolean {
        if(this === other) return true
        if(other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

        other as RefreshToken
        return id != null && id == other.id
    }

    override fun hashCode(): Int =
        Hibernate.getClass(this).hashCode()

    override fun toString(): String =
        "RefreshToken(id=$id, familyId=$familyId, expiresAt=$expiresAt, revokedAt=$revokedAt)"
}