package com.example.forgotpassword.controller;

import com.example.forgotpassword.dto.ForgotPasswordRequest;
import com.example.forgotpassword.dto.ResetPasswordRequest;
import com.example.forgotpassword.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class ForgotPasswordController {

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
    public String processForgotPassword(@Valid @ModelAttribute("form") ForgotPasswordRequest form,
                                        BindingResult bindingResult,
                                        Model model,
                                        RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            // collect all errors into one string like before, if you want
            String errorMessage = bindingResult.getAllErrors().stream()
                    .map(e -> e.getDefaultMessage())
                    .collect(Collectors.joining(" "));
            model.addAttribute("error", errorMessage);
            return "forgot-password";
        }

        try {
            boolean userExists = passwordResetService.initiatePasswordReset(form.getEmail());

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
    public String processResetPassword(
            @Valid @ModelAttribute("form") ResetPasswordRequest form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {

        // 1) Password & confirm match check
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "Passwords do not match.");
        }

        // 2) If any validation error (including @ValidPassword) → show form again
        if (bindingResult.hasErrors()) {
//            model.addAttribute("token", form.getToken());
            String errorMessage = bindingResult
                    .getAllErrors()
                    .stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .collect(Collectors.joining("\n")); // or "\n" for multiple lines

            model.addAttribute("error", errorMessage);
            model.addAttribute("form", form);
            model.addAttribute("token", form.getToken());
            return "reset-password";   // Thymeleaf template
        }

//        boolean success = passwordResetService.resetPassword(token, password);

        boolean success = passwordResetService.resetPassword(form.getToken(), form.getPassword());
        if (success) {
            redirectAttributes.addFlashAttribute("message",
                    "Your password has been reset successfully. You can now log in.");
            return "redirect:/reset-success";
        } else {
//            redirectAttributes.addFlashAttribute("error", "Invalid or expired reset link.");
            model.addAttribute("error", "Invalid or expired reset link.");
            model.addAttribute("token", form.getToken());
//            return "redirect:/reset-password?token=" + form.getToken();
            return "reset-password";
        }
    }
}