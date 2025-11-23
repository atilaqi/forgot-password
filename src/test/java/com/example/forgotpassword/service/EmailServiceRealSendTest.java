package com.example.forgotpassword.service;

import jakarta.mail.MessagingException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")   // uses application-test.properties
class EmailServiceRealSendTest {

    @Autowired
    private EmailService emailService;

    @Test
    void sendEmail_usingRealGmailSmtp() throws MessagingException {
        String to = "yourReceiverEmail@xxxgmailxxxx";
        String resetLink = "https://example.com/reset?token=abc123";
        String username = "John";

        emailService.sendPasswordResetEmail(to, resetLink, username);

        // no assertions; the real test is receiving the email
        System.out.println("Email sent — check your inbox!");
    }
}

