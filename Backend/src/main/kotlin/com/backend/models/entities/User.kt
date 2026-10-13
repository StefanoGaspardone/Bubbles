package com.backend.models.entities

import com.backend.exceptions.AccountAlreadyActivatedException
import com.backend.models.enums.AccountStatus
import com.backend.models.enums.UserRole
import jakarta.persistence.*
import org.hibernate.Hibernate
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(name = "first_name", nullable = false, length = 50)
    var firstName: String,

    @Column(name = "last_name", nullable = false, length = 50)
    var lastName: String,

    @Column(name = "username", nullable = false, unique = true, length = 50)
    var username: String,

    @Column(name = "email", nullable = false, unique = true, length = 255)
    var email: String,

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 20)
    var accountStatus: AccountStatus = AccountStatus.PENDING_ACTIVATION,

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    var role: UserRole = UserRole.USER,

    @Column(name = "notifications_enabled", nullable = false)
    var notificationsEnabled: Boolean = true,

    @Column(name = "avatar_id", nullable = true)
    var avatarId: UUID? = null,

    @Column(name = "email_verified_at", nullable = true)
    var emailVerifiedAt: Instant? = null,

    @Column(name = "last_login_at", nullable = true)
    var lastLoginAt: Instant? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {

    fun recordLogin() {
        lastLoginAt = Instant.now()
    }

    fun ensurePendingActivation() {
        if(accountStatus != AccountStatus.PENDING_ACTIVATION) {
            throw AccountAlreadyActivatedException()
        }
    }

    fun activate() {
        ensurePendingActivation()

        accountStatus = AccountStatus.ACTIVE
        emailVerifiedAt = Instant.now()
    }

    fun markDeleted() {
        check(accountStatus != AccountStatus.DELETED) { "Account is already deleted" }
        val userId = checkNotNull(id) { "Cannot delete a user that has not been persisted" }

        username = "deleted_$userId"
        email = "deleted-$userId@deleted.invalid"
        firstName = "Deleted"
        lastName = "User"
        passwordHash = ""
        avatarId = null
        notificationsEnabled = false
        accountStatus = AccountStatus.DELETED
    }

    override fun equals(other: Any?): Boolean {
        if(this === other) return true
        if(other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

        other as User
        return id != null && id == other.id
    }

    override fun hashCode(): Int =
        Hibernate.getClass(this).hashCode()

    override fun toString(): String =
        "User(id=$id, username=$username, email=$email)"
}