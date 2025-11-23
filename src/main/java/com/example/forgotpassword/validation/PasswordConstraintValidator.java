package com.example.forgotpassword.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordConstraintValidator implements ConstraintValidator<ValidPassword, String> {

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {

        if (password == null) {
            return false;
        }

        boolean valid = true;

        // We must disable default message
        context.disableDefaultConstraintViolation();

        if (password.length() < 8) {
            context.buildConstraintViolationWithTemplate(
                            "Password must be at least 8 characters long.")
                    .addConstraintViolation();
            valid = false;
        }

        if (!password.matches(".*[A-Z].*")) {
            context.buildConstraintViolationWithTemplate(
                            "Password must contain at least one uppercase letter (A–Z).")
                    .addConstraintViolation();
            valid = false;
        }

        if (!password.matches(".*[a-z].*")) {
            context.buildConstraintViolationWithTemplate(
                            "Password must contain at least one lowercase letter (a–z).")
                    .addConstraintViolation();
            valid = false;
        }

        if (!password.matches(".*\\d.*")) {
            context.buildConstraintViolationWithTemplate(
                            "Password must contain at least one digit (0–9).")
                    .addConstraintViolation();
            valid = false;
        }

        if (!password.matches(".*[@$!%*?&().,;:'\"\\\\|/#^_+=-].*")) {
            context.buildConstraintViolationWithTemplate(
                            "Password must contain at least one special character.")
                    .addConstraintViolation();
            valid = false;
        }

        if (password.contains(" ")) {
            context.buildConstraintViolationWithTemplate(
                            "Password cannot contain spaces.")
                    .addConstraintViolation();
            valid = false;
        }

        return valid;
    }
}
