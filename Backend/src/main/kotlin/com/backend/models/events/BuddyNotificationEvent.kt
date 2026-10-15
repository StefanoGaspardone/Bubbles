package com.backend.models.events

import com.backend.services.EmailService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.time.LocalDateTime
import java.util.UUID

enum class BuddyNotificationReason {
    ADDED,
    DATA_CHANGED,
}

data class BuddyNotification(
    val diveId: UUID,
    val toEmail: String,
    val buddyFirstName: String,
    val ownerFullName: String,
    val siteName: String,
    val diveStartLocal: LocalDateTime,
    val reason: BuddyNotificationReason,
)

data class BuddyNotificationsEvent(
    val notifications: List<BuddyNotification>,
)

@Component
class BuddyNotificationListener(
    private val emailService: EmailService,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onBuddyNotifications(event: BuddyNotificationsEvent) {
        event.notifications.forEach { notification ->
            try {
                emailService.sendBuddyNotification(notification)

                logger.info("\n\t[INFO] [buddy_notification_listener][on_buddy_notifications] {} notification sent for dive {}", notification.reason, notification.diveId)
            } catch(e: Exception) {
                logger.error("\n\t[ERROR] [buddy_notification_listener][on_buddy_notifications] Error sending {} notification for dive {}: {}", notification.reason, notification.diveId, e.message)
            }
        }
    }
}