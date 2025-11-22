package com.example.forgotpassword.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String showLoginPage(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            Model model) {

        if (error != null) {
            model.addAttribute("error", "Invalid email or password. Please try again.");
        }

        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully.");
        }

        return "login";
    }
}
