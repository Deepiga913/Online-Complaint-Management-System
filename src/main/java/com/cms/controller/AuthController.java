package com.cms.controller;

import com.cms.exception.BaseException;
import com.cms.model.User;
import com.cms.service.AuthService;
import com.cms.util.InputValidator;

import java.util.Scanner;

/**
 * Handles the Register / Login flows before a user reaches their dashboard.
 */
public class AuthController {

    private final AuthService authService;
    private final Scanner scanner;

    public AuthController(AuthService authService, Scanner scanner) {
        this.authService = authService;
        this.scanner = scanner;
    }

    /** Returns a valid session token, or null if the user chose to go back. */
    public String handleLogin() {
        System.out.println("\n--- Login ---");
        String username = InputValidator.readNonEmptyString(scanner, "Username: ");
        String password = InputValidator.readNonEmptyString(scanner, "Password: ");
        try {
            String token = authService.login(username, password);
            System.out.println("Welcome back, " + username + "!");
            return token;
        } catch (BaseException e) {
            System.out.println("Login failed: " + e.getMessage());
            return null;
        }
    }

    /** Returns true if registration succeeded. */
    public boolean handleRegister() {
        System.out.println("\n--- Register ---");
        String username = InputValidator.readNonEmptyString(scanner, "Choose a username: ");
        String password = InputValidator.readNonEmptyString(scanner, "Choose a password: ");
        String fullName = InputValidator.readNonEmptyString(scanner, "Full name: ");
        String email = InputValidator.readEmail(scanner, "Email: ");
        int roleChoice = InputValidator.readInt(scanner, "Register as (1) Customer  (2) Admin: ", 1, 2);
        boolean isAdmin = roleChoice == 2;
        try {
            User user = authService.register(username, password, fullName, email, isAdmin);
            System.out.println("Registration successful! You can now log in as " + user.getRole().toLowerCase() + ".");
            return true;
        } catch (BaseException e) {
            System.out.println("Registration failed: " + e.getMessage());
            return false;
        }
    }
}
