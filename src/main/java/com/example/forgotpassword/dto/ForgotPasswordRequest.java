package com.example.forgotpassword.dto;

import com.example.forgotpassword.validation.ValidEmail;

public class ForgotPasswordRequest {

    @ValidEmail
    private String email;

    // getter/setter
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}

