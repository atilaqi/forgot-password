package com.example.forgotpassword.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = EmailConstraintValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEmail {

    String message() default "Invalid email address.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
