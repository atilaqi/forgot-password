package com.example.forgotpassword.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public void sendPasswordResetEmail(String toEmail, String resetLink, String username) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(toEmail);
        helper.setSubject("Password Reset Request");

        Context context = new Context();
        context.setVariable("resetLink", resetLink);
        context.setVariable("username", username);

        String htmlContent = templateEngine.process("email/password-reset-email", context);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }
}