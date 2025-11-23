package com.example.forgotpassword.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @InjectMocks
    private EmailService emailService;

    private MimeMessage mimeMessage;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    void sendPasswordResetEmail_success() throws Exception {
        // Given
        String to = "user@example.com";
        String link = "http://localhost/reset?token=abc123";
        String username = "john";
        String generatedHtml = "<html>reset email</html>";

        // Mock template engine result
        when(templateEngine.process(eq("email/password-reset-email"), any(Context.class)))
                .thenReturn(generatedHtml);

        // When
        emailService.sendPasswordResetEmail(to, link, username);

        // Then
        // Verify template engine was called with correct template
        verify(templateEngine).process(eq("email/password-reset-email"), any(Context.class));

        // Capture context to verify variables
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("email/password-reset-email"), contextCaptor.capture());

        Context contextUsed = contextCaptor.getValue();
        assertThat(contextUsed.getVariable("resetLink")).isEqualTo(link);
        assertThat(contextUsed.getVariable("username")).isEqualTo(username);

        // Verify email was sent
        verify(mailSender).send(mimeMessage);
    }
}
