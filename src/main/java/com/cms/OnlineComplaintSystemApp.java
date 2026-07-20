package com.cms;

import com.cms.controller.AdminController;
import com.cms.controller.AuthController;
import com.cms.controller.CustomerController;
import com.cms.exception.BaseException;
import com.cms.model.User;
import com.cms.repository.ComplaintRepository;
import com.cms.repository.UserRepository;
import com.cms.service.AsyncLoggerService;
import com.cms.service.AuthService;
import com.cms.service.ComplaintService;
import com.cms.util.InputValidator;

import java.nio.file.Path;
import java.util.Scanner;

/**
 * Entry point. Wires up repositories -> services -> controllers, then
 * drives the Register / Login / Exit welcome menu and routes authenticated
 * users to the correct dashboard based on their role.
 */
public class OnlineComplaintSystemApp {

    public static void main(String[] args) {
        Path dataDir = Path.of("data");
        UserRepository userRepository = new UserRepository(dataDir.resolve("users.csv"));
        ComplaintRepository complaintRepository = new ComplaintRepository(dataDir.resolve("complaints.csv"));
        AsyncLoggerService logger = new AsyncLoggerService(dataDir.resolve("activity.log"));

        AuthService authService = new AuthService(userRepository, logger);
        ComplaintService complaintService = new ComplaintService(complaintRepository, logger, dataDir.resolve("feedback.csv"));

        Scanner scanner = new Scanner(System.in);
        AuthController authController = new AuthController(authService, scanner);
        CustomerController customerController = new CustomerController(complaintService, scanner);
        AdminController adminController = new AdminController(complaintService, scanner);

        // Ensure the background logger flushes and shuts down even on Ctrl+C.
        Runtime.getRuntime().addShutdownHook(new Thread(logger::shutdown));

        printBanner();

        boolean appRunning = true;
        while (appRunning) {
            System.out.println("\n===== Online Complaint Management System =====");
            System.out.println("1) Register");
            System.out.println("2) Login");
            System.out.println("3) Exit");
            int choice = InputValidator.readInt(scanner, "Choose an option: ", 1, 3);

            switch (choice) {
                case 1 -> authController.handleRegister();
                case 2 -> {
                    String token = authController.handleLogin();
                    if (token != null) {
                        routeToDashboard(authService, token, customerController, adminController);
                        authService.logout(token);
                    }
                }
                case 3 -> appRunning = false;
            }
        }

        System.out.println("Goodbye!");
        logger.shutdown();
        scanner.close();
    }

    private static void routeToDashboard(AuthService authService, String token,
                                          CustomerController customerController,
                                          AdminController adminController) {
        try {
            User user = authService.getCurrentUser(token);
            if ("ADMIN".equals(user.getRole())) {
                adminController.showDashboard(user);
            } else {
                customerController.showDashboard(user);
            }
        } catch (BaseException e) {
            System.out.println("Session error: " + e.getMessage());
        }
    }

    private static void printBanner() {
        System.out.println("""
                 ___       _ _              ____                      _       _       _
                / _ \\ _ __ | (_)_ __   ___  / ___|___  _ __ ___  _ __ | | __ _(_)_ __ | |_
               | | | | '_ \\| | | '_ \\ / _ \\| |   / _ \\| '_ ` _ \\| '_ \\| |/ _` | | '_ \\| __|
               | |_| | | | | | | | | |  __/| |__| (_) | | | | | | |_) | | (_| | | | | | |_
                \\___/|_| |_|_|_|_| |_|\\___| \\____\\___/|_| |_| |_| .__/|_|\\__,_|_|_| |_|\\__|
                                                                  |_|
                Online Complaint Management System
                """);
    }
}
