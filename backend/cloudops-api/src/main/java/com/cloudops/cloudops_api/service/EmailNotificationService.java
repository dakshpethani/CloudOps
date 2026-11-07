
package com.cloudops.cloudops_api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService {

    private final JavaMailSender mailSender;

    @Value("${cloudops.alert.admin-email}")
    private String adminEmail;

    public EmailNotificationService(JavaMailSender javaMailSender) {
        this.mailSender = javaMailSender;
    }

    // Existing global admin notification method
    public void sendAdminAlertEmail(String subject, String message) {

        sendAlertEmail(adminEmail, subject, message);
    }

    // Send notification to a specific server's configured email
    public void sendAlertEmail(
            String recipient,
            String subject,
            String message
    ) {

        if (recipient == null || recipient.isBlank()) {
            throw new IllegalArgumentException(
                    "Alert recipient email is not configured"
            );
        }

        SimpleMailMessage mailMessage = new SimpleMailMessage();

        mailMessage.setTo(recipient);
        mailMessage.setSubject(subject);
        mailMessage.setText(message);

        mailSender.send(mailMessage);
    }
}
