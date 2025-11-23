package com.example.forgotpassword.dto;

import com.example.forgotpassword.validation.ValidPassword;

public class ResetPasswordRequest {

    private String token;

    @ValidPassword
    private String password;

    private String confirmPassword;

    // getters & setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}

