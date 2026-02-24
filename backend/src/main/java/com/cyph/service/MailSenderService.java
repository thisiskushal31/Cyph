package com.cyph.service;

import com.cyph.config.CyphProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends the "you have a secret message" email with the view link.
 * No-op when SMTP is not configured (e.g. dev).
 */
@Service
public class MailSenderService {

    private final JavaMailSender mailSender;
    private final CyphProperties cyphProperties;

    public MailSenderService(@Autowired(required = false) JavaMailSender mailSender,
                             CyphProperties cyphProperties) {
        this.mailSender = mailSender;
        this.cyphProperties = cyphProperties;
    }

    public void sendSecretMessageNotification(String recipientEmail, String senderDisplayName, String viewUrl, int ttlHours) {
        if (mailSender == null || cyphProperties.getMail().getHost() == null || cyphProperties.getMail().getHost().isBlank()) {
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(cyphProperties.getMail().getFrom());
        message.setTo(recipientEmail);
        message.setSubject(senderDisplayName + " has sent you a secret message!");
        message.setText(
                "Click here to view your secret message: " + viewUrl + "\n\n" +
                "Only you can access this message after signing in.\n\n" +
                "This message will expire in " + ttlHours + " hours."
        );
        mailSender.send(message);
    }
}
