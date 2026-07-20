package com.cms.controller;

import com.cms.exception.BaseException;
import com.cms.model.Complaint;
import com.cms.model.Priority;
import com.cms.model.User;
import com.cms.service.ComplaintService;
import com.cms.util.InputValidator;

import java.util.List;
import java.util.Scanner;

/**
 * Console dashboard for customers: file complaints, track their status,
 * and leave feedback once resolved.
 */
public class CustomerController {

    private final ComplaintService complaintService;
    private final Scanner scanner;

    public CustomerController(ComplaintService complaintService, Scanner scanner) {
        this.complaintService = complaintService;
        this.scanner = scanner;
    }

    public void showDashboard(User customer) {
        boolean running = true;
        while (running) {
            System.out.println("\n===== Customer Dashboard (" + customer.getUsername() + ") =====");
            System.out.println("1) File a new complaint");
            System.out.println("2) View my complaints");
            System.out.println("3) Leave feedback on a resolved complaint");
            System.out.println("4) Logout");
            int choice = InputValidator.readInt(scanner, "Choose an option: ", 1, 4);
            switch (choice) {
                case 1 -> fileComplaint(customer);
                case 2 -> viewMyComplaints(customer);
                case 3 -> leaveFeedback(customer);
                case 4 -> running = false;
            }
        }
    }

    private void fileComplaint(User customer) {
        System.out.println("\n--- File a Complaint ---");
        String category = InputValidator.readNonEmptyString(scanner, "Category (e.g. Billing, Delivery, Product): ");
        String description = InputValidator.readNonEmptyString(scanner, "Describe your issue: ");
        Priority priority = InputValidator.readEnum(scanner, "Priority: ", Priority.class);
        Complaint complaint = complaintService.fileComplaint(customer.getUsername(), category, description, priority);
        System.out.println("Complaint filed successfully! Your tracking ID is: " + complaint.getId());
    }

    private void viewMyComplaints(User customer) {
        List<Complaint> complaints = complaintService.listByCustomer(customer.getUsername());
        System.out.println("\n--- My Complaints (" + complaints.size() + ") ---");
        if (complaints.isEmpty()) {
            System.out.println("You haven't filed any complaints yet.");
            return;
        }
        for (Complaint c : complaints) {
            System.out.println(c);
        }
    }

    private void leaveFeedback(User customer) {
        String complaintId = InputValidator.readNonEmptyString(scanner, "Complaint ID to give feedback on: ");
        int rating = InputValidator.readInt(scanner, "Rating (1-5): ", 1, 5);
        String comment = InputValidator.readNonEmptyString(scanner, "Comment: ");
        try {
            complaintService.submitFeedback(complaintId, customer.getUsername(), rating, comment);
            System.out.println("Thank you for your feedback!");
        } catch (BaseException e) {
            System.out.println("Could not submit feedback: " + e.getMessage());
        }
    }
}
