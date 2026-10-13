package com.backend.services

import com.backend.properties.EmailProperties
import org.slf4j.LoggerFactory
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class EmailService(
    private val mailSender: JavaMailSender,
    private val templateEngine: TemplateEngine,
    private val emailProperties: EmailProperties
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Async
    fun sendActivationOtp(to: String, otpCode: String, expiresInMinutes: Long) {
        val context = Context().apply {
            setVariable("otpCode", otpCode)
            setVariable("expiresInMinutes", expiresInMinutes)
        }

        send(to = to, subject = "Activation Code - Bubbles", template = "emails/otp", context = context)
    }

    @Async
    fun sendPasswordResetOtp(to: String, otpCode: String, expiresInMinutes: Long) {
        val context = Context().apply {
            setVariable("otpCode", otpCode)
            setVariable("expiresInMinutes", expiresInMinutes)
        }

        send(to = to, subject = "Reset your password - Bubbles", template = "emails/password-reset", context = context)
    }

    private fun send(to: String, subject: String, template: String, context: Context) {
        logger.debug("\n\t[DEBUG] [email_service][send] Sending email\n\tto={}\n\ttemplate={}", to, template)

        try {
            val htmlBody = templateEngine.process(template, context)

            val message = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, false, "UTF-8")

            helper.setTo(to)
            helper.setSubject(subject)
            helper.setFrom(emailProperties.fromAddress, emailProperties.fromName)
            helper.setText(htmlBody, true)

            mailSender.send(message)

            logger.info("\n\t[INFO] [email_service][send] Email sent\n\tto={}\n\ttemplate={}", to, template)
        } catch(e: Exception) {
            logger.error("\n\t[ERROR] [email_service][send] Failed to send email\n\tto={}\n\ttemplate={}\n\terror={}", to, template, e.message)
            throw e
        }
    }
}