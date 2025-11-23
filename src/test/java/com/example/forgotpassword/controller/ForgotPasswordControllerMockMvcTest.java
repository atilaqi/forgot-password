package com.example.forgotpassword.controller;

import com.example.forgotpassword.service.PasswordResetService;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(ForgotPasswordController.class)
class ForgotPasswordControllerMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PasswordResetService passwordResetService;

    /**
     * 1) Passwords do NOT match → stay on page, show error in div.
     */
    @Test
    void whenPasswordsDoNotMatch_shouldReturnResetPasswordWithError() throws Exception {
        mockMvc.perform(post("/reset-password")
                        .param("token", "abc123")
                        .param("password", "Password1!")
                        .param("confirmPassword", "Password2!"))
                .andExpect(status().isOk())
                .andExpect(view().name("reset-password"))
                .andExpect(content().string(containsString("Passwords do not match.")));

        // service should NOT be called
        verifyNoInteractions(passwordResetService);
    }

    /**
     * 2) Valid passwords but token invalid → stay on page, show "Invalid or expired token."
     */
    @Test
    void whenTokenInvalid_shouldReturnResetPasswordWithTokenError() throws Exception {
        when(passwordResetService.resetPassword("bad-token", "Password1!"))
                .thenReturn(false);

        mockMvc.perform(post("/reset-password")
                        .param("token", "bad-token")
                        .param("password", "Password1!")
                        .param("confirmPassword", "Password1!"))
                .andExpect(status().isOk())
                .andExpect(view().name("reset-password"))
                .andExpect(content().string(containsString("Invalid or expired reset link.")));

        verify(passwordResetService).resetPassword("bad-token", "Password1!");
    }

    /**
     * 3) Everything OK → redirect to /reset-success with flash message.
     */
    @Test
    void whenEverythingOk_shouldRedirectToSuccessAndSetFlashMessage() throws Exception {
        when(passwordResetService.resetPassword("good-token", "Password1!"))
                .thenReturn(true);

        mockMvc.perform(post("/reset-password")
                        .param("token", "good-token")
                        .param("password", "Password1!")
                        .param("confirmPassword", "Password1!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reset-success"))
                .andExpect(flash().attribute("message",
                        "Your password has been reset successfully. You can now log in."));

        verify(passwordResetService).resetPassword("good-token", "Password1!");
    }

    /**
     * 1) Validation error path: bindingResult.hasErrors() == true
     * e.g. invalid email format -> stay on forgot-password page, with error in model.
     */
    @Test
    void whenValidationFails_shouldReturnForgotPasswordViewWithError() throws Exception {
        // invalid email (should fail @ValidEmail / @Email)
        mockMvc.perform(post("/forgot-password")
                        .param("email", "not-an-email"))
                .andExpect(status().isOk())
                .andExpect(view().name("forgot-password"))
                // error attribute exists in model
                .andExpect(model().attributeExists("error"));
        // Optional: check content contains some error text if you want
        // .andExpect(content().string(containsString("Invalid email")));
    }

    /**
     * 2) User exists: initiatePasswordReset returns true
     * -> redirect, with success flash message.
     */
    @Test
    void whenUserExists_shouldRedirectWithSuccessFlashMessage() throws Exception {
        String email = "user@example.com";
        when(passwordResetService.initiatePasswordReset(email)).thenReturn(true);

        mockMvc.perform(post("/forgot-password")
                        .param("email", email))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/forgot-password"))
                .andExpect(flash().attribute("message",
                        "A password reset link has been sent. Please check your inbox."));

        verify(passwordResetService).initiatePasswordReset(email);
    }

    /**
     * 3) User does NOT exist: initiatePasswordReset returns false
     * -> redirect, with "doesn't exist" flash message.
     */
    @Test
    void whenUserDoesNotExist_shouldRedirectWithNotExistFlashMessage() throws Exception {
        String email = "missing@example.com";
        when(passwordResetService.initiatePasswordReset(email)).thenReturn(false);

        mockMvc.perform(post("/forgot-password")
                        .param("email", email))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/forgot-password"))
                .andExpect(flash().attribute("message",
                        "The email address you entered doesn't exist"));

        verify(passwordResetService).initiatePasswordReset(email);
    }

    /**
     * 4) Exception thrown while sending email
     * -> redirect, with error flash message.
     */
    @Test
    void whenExceptionThrown_shouldRedirectWithErrorFlashMessage() throws Exception {
        String email = "user@example.com";
        when(passwordResetService.initiatePasswordReset(email))
                .thenThrow(new RuntimeException("SMTP failure"));

        mockMvc.perform(post("/forgot-password")
                        .param("email", email))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/forgot-password"))
                .andExpect(flash().attribute("error",
                        "Unable to send reset email at this time. Please try again later or contact support."));

        verify(passwordResetService).initiatePasswordReset(email);
    }

    /**
     * MessagingException thrown during sending email -> redirect + error flash message
     */
    @Test
    void whenMessagingExceptionThrown_shouldRedirectAndSetErrorFlashMessage() throws Exception {
        String email = "user@example.com";

        // Mock the exception from the service
        when(passwordResetService.initiatePasswordReset(email))
                .thenThrow(new MessagingException("SMTP failure"));

        // Perform POST request
        mockMvc.perform(post("/forgot-password")
                        .param("email", email))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/forgot-password"))
                .andExpect(flash().attribute("error",
                        "Unable to send reset email at this time. Please try again later or contact support."));

        // Verify service interaction
        verify(passwordResetService).initiatePasswordReset(email);
    }
}
