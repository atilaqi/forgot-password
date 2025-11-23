package com.example.forgotpassword.controller;

import com.example.forgotpassword.dto.ForgotPasswordRequest;
import com.example.forgotpassword.dto.ResetPasswordRequest;
import com.example.forgotpassword.service.PasswordResetService;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ForgotPasswordControllerTest {

    @Mock
    private PasswordResetService passwordResetService;

    @InjectMocks
    private ForgotPasswordController controller;

    @Test
    void whenPasswordValidationFails_shouldReturnResetPasswordAndSetError() {
        // given
        ResetPasswordRequest form = new ResetPasswordRequest();
        form.setToken("token123");
        form.setPassword("short");          // invalid
        form.setConfirmPassword("short");

        // Simulate a validation error on password (as if @ValidPassword failed)
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        bindingResult.addError(new FieldError(
                "form",
                "password",
                "Password must be at least 8 characters long."
        ));

        Model model = new ExtendedModelMap();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        // when
        String viewName = controller.processResetPassword(form, bindingResult,redirectAttributes, model);

        // then
        assertEquals("reset-password", viewName);
        assertTrue(model.containsAttribute("error"));

        String errorMessage = (String) model.getAttribute("error");
        assertNotNull(errorMessage);
        assertTrue(errorMessage.contains("Password must be at least 8 characters long."));

        // no call to service
        verifyNoInteractions(passwordResetService);
    }

    @Test
    void whenPasswordsDoNotMatch_shouldReturnResetPasswordAndSetError() {
        // given
        ResetPasswordRequest form = new ResetPasswordRequest();
        form.setToken("token123");
        form.setPassword("Password1!");
        form.setConfirmPassword("Password2!"); // mismatch

        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        Model model = new ExtendedModelMap();

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        // when
        String viewName = controller.processResetPassword(form, bindingResult,redirectAttributes, model);

        // then
        assertEquals("reset-password", viewName);
        String errorMessage = (String) model.getAttribute("error");
        assertNotNull(errorMessage);
        assertTrue(errorMessage.contains("Passwords do not match."));

        verifyNoInteractions(passwordResetService);
    }

    @Test
    void whenTokenInvalid_shouldReturnResetPasswordAndSetTokenError() {
        // given
        ResetPasswordRequest form = new ResetPasswordRequest();
        form.setToken("bad-token");
        form.setPassword("Password1!");
        form.setConfirmPassword("Password1!");

        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        Model model = new ExtendedModelMap();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        when(passwordResetService.resetPassword("bad-token", "Password1!"))
                .thenReturn(false);

        // when
        String viewName = controller.processResetPassword(form, bindingResult,redirectAttributes, model);

        // then
        assertEquals("reset-password", viewName);
        String errorMessage = (String) model.getAttribute("error");
        assertEquals("Invalid or expired reset link.", errorMessage);

        verify(passwordResetService).resetPassword("bad-token", "Password1!");
    }

    @Test
    void whenEverythingOk_shouldReturnSuccessViewAndCallService() {
        // given
        ResetPasswordRequest form = new ResetPasswordRequest();
        form.setToken("good-token");
        form.setPassword("Password1!");
        form.setConfirmPassword("Password1!");

        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        Model model = new ExtendedModelMap();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        when(passwordResetService.resetPassword("good-token", "Password1!"))
                .thenReturn(true);

        // when
        String viewName = controller.processResetPassword(form, bindingResult, redirectAttributes, model);

        // then
        assertEquals("redirect:/reset-success", viewName);

        // Verify flash message exists
        assertTrue(redirectAttributes.getFlashAttributes().containsKey("message"));
        assertEquals(
                "Your password has been reset successfully. You can now log in.",
                redirectAttributes.getFlashAttributes().get("message")
        );
        verify(passwordResetService).resetPassword("good-token", "Password1!");
    }

    /**
     * 1) Validation error: bindingResult.hasErrors() == true
     */
    @Test
    void whenValidationFails_shouldReturnForgotPasswordAndSetErrorOnModel() {
        // given
        ForgotPasswordRequest form = new ForgotPasswordRequest();
        form.setEmail("invalid-email");

        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        bindingResult.rejectValue("email", "invalid", "Invalid email address.");

        Model model = new ExtendedModelMap();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        // when
        String viewName = controller.processForgotPassword(form, bindingResult, model, redirectAttributes);

        // then
        assertEquals("forgot-password", viewName);
        assertTrue(model.containsAttribute("error"));

        String errorMessage = (String) model.getAttribute("error");
        assertNotNull(errorMessage);
        assertTrue(errorMessage.contains("Invalid email address."));

        // service should not be called at all
        verifyNoInteractions(passwordResetService);
    }

    /**
     * 2) User exists: initiatePasswordReset returns true
     */
    @Test
    void whenUserExists_shouldRedirectAndSetSuccessFlashMessage() throws MessagingException {
        // given
        ForgotPasswordRequest form = new ForgotPasswordRequest();
        form.setEmail("user@example.com");

        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        Model model = new ExtendedModelMap();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        when(passwordResetService.initiatePasswordReset("user@example.com"))
                .thenReturn(true);

        // when
        String viewName = controller.processForgotPassword(form, bindingResult, model, redirectAttributes);

        // then
        assertEquals("redirect:/forgot-password", viewName);

        var flashes = redirectAttributes.getFlashAttributes();
        assertTrue(flashes.containsKey("message"));
        assertEquals("A password reset link has been sent. Please check your inbox.",
                flashes.get("message"));

        verify(passwordResetService).initiatePasswordReset("user@example.com");
    }

    /**
     * 3) User does NOT exist: initiatePasswordReset returns false
     */
    @Test
    void whenUserDoesNotExist_shouldRedirectAndSetDoesNotExistMessage() throws MessagingException {
        // given
        ForgotPasswordRequest form = new ForgotPasswordRequest();
        form.setEmail("missing@example.com");

        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        Model model = new ExtendedModelMap();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        when(passwordResetService.initiatePasswordReset("missing@example.com"))
                .thenReturn(false);

        // when
        String viewName = controller.processForgotPassword(form, bindingResult, model, redirectAttributes);

        // then
        assertEquals("redirect:/forgot-password", viewName);

        var flashes = redirectAttributes.getFlashAttributes();
        assertTrue(flashes.containsKey("message"));
        assertEquals("The email address you entered doesn't exist",
                flashes.get("message"));

        verify(passwordResetService).initiatePasswordReset("missing@example.com");
    }

    /**
     * 4) Exception thrown from service: error flash attribute set
     */
    @Test
    void whenExceptionThrown_shouldRedirectAndSetErrorFlashMessage() throws MessagingException {
        // given
        ForgotPasswordRequest form = new ForgotPasswordRequest();
        form.setEmail("user@example.com");

        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        Model model = new ExtendedModelMap();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        when(passwordResetService.initiatePasswordReset("user@example.com"))
                .thenThrow(new RuntimeException("SMTP failure"));

        // when
        String viewName = controller.processForgotPassword(form, bindingResult, model, redirectAttributes);

        // then
        assertEquals("redirect:/forgot-password", viewName);

        var flashes = redirectAttributes.getFlashAttributes();
        assertTrue(flashes.containsKey("error"));
        assertEquals(
                "Unable to send reset email at this time. Please try again later or contact support.",
                flashes.get("error")
        );

        verify(passwordResetService).initiatePasswordReset("user@example.com");
    }

    @Test
    void whenMessagingExceptionThrown_shouldRedirectAndSetErrorFlashMessage() throws Exception {
        // given
        ForgotPasswordRequest form = new ForgotPasswordRequest();
        form.setEmail("user@example.com");

        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        Model model = new ExtendedModelMap();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        // Simulate MessagingException thrown by the service
        when(passwordResetService.initiatePasswordReset("user@example.com"))
                .thenThrow(new MessagingException("SMTP connection failed"));

        // when
        String viewName = controller.processForgotPassword(
                form, bindingResult, model, redirectAttributes);

        // then
        assertEquals("redirect:/forgot-password", viewName);

        // Validate flash attribute with error message
        var flashes = redirectAttributes.getFlashAttributes();
        assertTrue(flashes.containsKey("error"));
        assertEquals(
                "Unable to send reset email at this time. Please try again later or contact support.",
                flashes.get("error")
        );

        verify(passwordResetService).initiatePasswordReset("user@example.com");
    }

}
