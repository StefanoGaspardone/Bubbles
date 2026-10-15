package com.backend.models.entities

import com.backend.models.enums.BuddyConfirmationStatus
import com.backend.models.enums.BuddyRole
import jakarta.persistence.*
import org.hibernate.Hibernate
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "dive_buddies")
class DiveBuddy(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dive_id", nullable = false, updatable = false)
    var dive: Dive,

    @Column(name = "position", nullable = false)
    var position: Int = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buddy_user_id", nullable = true, updatable = false)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    var buddyUser: User? = null,

    @Column(name = "first_name", nullable = false, length = 50)
    var firstName: String,

    @Column(name = "last_name", nullable = true, length = 50)
    var lastName: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    var role: BuddyRole = BuddyRole.BUDDY,

    @Enumerated(EnumType.STRING)
    @Column(name = "confirmation_status", nullable = false, length = 20)
    var confirmationStatus: BuddyConfirmationStatus = BuddyConfirmationStatus.NOT_REQUIRED,

    @Column(name = "confirmed_at", nullable = true)
    var confirmedAt: Instant? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
) {

    companion object {
        fun forUser(dive: Dive, user: User, role: BuddyRole): DiveBuddy = DiveBuddy(
            dive = dive,
            buddyUser = user,
            firstName = user.firstName,
            lastName = user.lastName,
            role = role,
            confirmationStatus = BuddyConfirmationStatus.PENDING,
        )

        fun custom(dive: Dive, firstName: String, lastName: String?, role: BuddyRole): DiveBuddy = DiveBuddy(
            dive = dive,
            firstName = firstName,
            lastName = lastName,
            role = role,
            confirmationStatus = BuddyConfirmationStatus.NOT_REQUIRED,
        )
    }

    fun isRealUser(): Boolean =
        buddyUser != null

    fun isConfirmed(): Boolean =
        confirmationStatus == BuddyConfirmationStatus.CONFIRMED

    fun confirm(now: Instant = Instant.now()) {
        check(isRealUser()) { "Only real users can confirm a dive" }

        if(confirmationStatus != BuddyConfirmationStatus.CONFIRMED) {
            confirmationStatus = BuddyConfirmationStatus.CONFIRMED
            confirmedAt = now
        }
    }

    fun resetConfirmation(): Boolean {
        if(!isRealUser() || confirmationStatus != BuddyConfirmationStatus.CONFIRMED) return false

        confirmationStatus = BuddyConfirmationStatus.PENDING
        confirmedAt = null
        return true
    }

    override fun equals(other: Any?): Boolean {
        if(this === other) return true
        if(other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

        other as DiveBuddy
        return id != null && id == other.id
    }

    override fun hashCode(): Int =
        Hibernate.getClass(this).hashCode()

    override fun toString(): String =
        "DiveBuddy(id=$id, role=$role, confirmationStatus=$confirmationStatus)"
}