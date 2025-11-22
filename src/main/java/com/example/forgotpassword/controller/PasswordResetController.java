package com.example.forgotpassword.controller;

import com.example.forgotpassword.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @GetMapping("/forgot-password")
    public String showForgotPasswordPage() {
        return "forgot-password";
    }

    @GetMapping("/reset-success")
    public String resetSuccess() {
        return "reset-success";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam("email") String email,
                                        RedirectAttributes redirectAttributes) {
        try {
            boolean userExists = passwordResetService.initiatePasswordReset(email);

            // Always show the same message to prevent user enumeration
            // Whether user exists or not, show the same success message
            if (userExists)
                redirectAttributes.addFlashAttribute("message",
                        "A password reset link has been sent. Please check your inbox.");
            else
                redirectAttributes.addFlashAttribute("message",
                        "The email address you entered doesn't exist");

        } catch (Exception e) {
            // Email sending failed - show actual error to user
            redirectAttributes.addFlashAttribute("error",
                    "Unable to send reset email at this time. Please try again later or contact support.");
            System.err.println("Failed to send password reset email: " + e.getMessage());
            e.printStackTrace();
        }

        return "redirect:/forgot-password";
    }

    @GetMapping("/reset-password")
    public String showResetPasswordPage(@RequestParam("token") String token, Model model) {
        boolean isValid = passwordResetService.validateResetToken(token);

        if (!isValid) {
            model.addAttribute("error", "Invalid or expired reset link.");
            return "reset-password-error";
        }

        model.addAttribute("token", token);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam("token") String token,
                                       @RequestParam("password") String password,
                                       @RequestParam("confirmPassword") String confirmPassword,
                                       RedirectAttributes redirectAttributes) {
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match.");
            redirectAttributes.addAttribute("token", token);
            return "redirect:/reset-password";
        }

        if (password.length() < 8) {
            redirectAttributes.addFlashAttribute("error", "Password must be at least 8 characters.");
            redirectAttributes.addAttribute("token", token);
            return "redirect:/reset-password";
        }

        boolean success = passwordResetService.resetPassword(token, password);

        if (success) {
            redirectAttributes.addFlashAttribute("message",
                    "Your password has been reset successfully. You can now log in.");
            return "redirect:/reset-success";
        } else {
            redirectAttributes.addFlashAttribute("error", "Invalid or expired reset link.");
            return "redirect:/reset-password?token=" + token;
        }
    }
}