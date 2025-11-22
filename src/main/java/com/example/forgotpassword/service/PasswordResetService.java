package com.example.forgotpassword.service;

import com.example.forgotpassword.entity.User;
import com.example.forgotpassword.repository.UserRepository;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Transactional
    public boolean initiatePasswordReset(String email) throws MessagingException {
        Optional<User> userOpt = userRepository.findByEmail(email);

        // User not found is expected behavior - return false silently
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        String resetToken = UUID.randomUUID().toString();

        user.setResetToken(resetToken);
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        String resetLink = baseUrl + "/reset-password?token=" + resetToken;

        // Let the exception propagate if email fails to send
        emailService.sendPasswordResetEmail(user.getEmail(), resetLink, user.getUsername());
        return true;
    }

    public boolean validateResetToken(String token) {
        Optional<User> userOpt = userRepository.findByResetToken(token);

        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        return user.getResetTokenExpiry() != null &&
                user.getResetTokenExpiry().isAfter(LocalDateTime.now());
    }

    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        Optional<User> userOpt = userRepository.findByResetToken(token);

        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();

        if (user.getResetTokenExpiry() == null ||
                user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            return false;
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);

        return true;
    }
}