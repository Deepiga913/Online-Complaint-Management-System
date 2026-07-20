package com.cms.controller;

import com.cms.exception.BaseException;
import com.cms.model.Complaint;
import com.cms.model.ComplaintStatus;
import com.cms.model.Priority;
import com.cms.model.User;
import com.cms.service.ComplaintService;
import com.cms.util.InputValidator;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Console dashboard for admins: tabular ticket views, dynamic filtering,
 * status/assignment updates, and an analytics summary.
 */
public class AdminController {

    private static final String ROW_FORMAT = "%-20s %-14s %-11s %-8s %-15s %-30s%n";

    private final ComplaintService complaintService;
    private final Scanner scanner;

    public AdminController(ComplaintService complaintService, Scanner scanner) {
        this.complaintService = complaintService;
        this.scanner = scanner;
    }

    public void showDashboard(User admin) {
        boolean running = true;
        while (running) {
            System.out.println("\n===== Admin Dashboard (" + admin.getUsername() + ") =====");
            System.out.println("1) View all complaints");
            System.out.println("2) Filter complaints");
            System.out.println("3) Update complaint status");
            System.out.println("4) Assign complaint to myself");
            System.out.println("5) View analytics");
            System.out.println("6) Logout");
            int choice = InputValidator.readInt(scanner, "Choose an option: ", 1, 6);
            switch (choice) {
                case 1 -> printTable(complaintService.listAll());
                case 2 -> filterMenu();
                case 3 -> updateStatus(admin);
                case 4 -> assignToSelf(admin);
                case 5 -> showAnalytics();
                case 6 -> running = false;
            }
        }
    }

    private void filterMenu() {
        System.out.println("\n--- Filter Complaints ---");
        System.out.println("1) By category  2) By priority  3) By status  4) By date range");
        int choice = InputValidator.readInt(scanner, "Choose a filter: ", 1, 4);
        List<Complaint> result = switch (choice) {
            case 1 -> complaintService.filterByCategory(InputValidator.readNonEmptyString(scanner, "Category: "));
            case 2 -> complaintService.filterByPriority(InputValidator.readEnum(scanner, "Priority: ", Priority.class));
            case 3 -> complaintService.filterByStatus(InputValidator.readEnum(scanner, "Status: ", ComplaintStatus.class));
            case 4 -> {
                LocalDate from = InputValidator.readDate(scanner, "From date");
                LocalDate to = InputValidator.readDate(scanner, "To date");
                yield complaintService.filterByDateRange(from, to);
            }
            default -> List.of();
        };
        printTable(result);
    }

    private void updateStatus(User admin) {
        String id = InputValidator.readNonEmptyString(scanner, "Complaint ID: ");
        ComplaintStatus newStatus = InputValidator.readEnum(scanner, "New status: ", ComplaintStatus.class);
        try {
            complaintService.updateStatus(id, newStatus, admin.getUsername());
            System.out.println("Complaint " + id + " updated to " + newStatus + ".");
        } catch (BaseException e) {
            System.out.println("Update failed: " + e.getMessage());
        }
    }

    private void assignToSelf(User admin) {
        String id = InputValidator.readNonEmptyString(scanner, "Complaint ID: ");
        try {
            complaintService.assignToAdmin(id, admin.getUsername());
            System.out.println("Complaint " + id + " assigned to you.");
        } catch (BaseException e) {
            System.out.println("Assignment failed: " + e.getMessage());
        }
    }

    private void showAnalytics() {
        ComplaintService.AnalyticsSummary summary = complaintService.computeAnalytics();
        System.out.println("""

                --- Analytics Summary ---""");
        System.out.printf("Total complaints:              %d%n", summary.totalComplaints());
        System.out.printf("Average resolution time:       %.2f hours%n", summary.averageResolutionHours());
        System.out.printf("Average satisfaction rating:   %.2f / 5%n", summary.averageSatisfactionRating());

        System.out.println("\nBy category:");
        for (Map.Entry<String, Long> e : summary.countByCategory().entrySet()) {
            System.out.printf("  %-20s %d%n", e.getKey(), e.getValue());
        }
        System.out.println("\nBy priority:");
        for (Map.Entry<Priority, Long> e : summary.countByPriority().entrySet()) {
            System.out.printf("  %-20s %d%n", e.getKey(), e.getValue());
        }
        System.out.println("\nBy status:");
        for (Map.Entry<ComplaintStatus, Long> e : summary.countByStatus().entrySet()) {
            System.out.printf("  %-20s %d%n", e.getKey(), e.getValue());
        }
    }

    private void printTable(List<Complaint> complaints) {
        System.out.println();
        System.out.printf(ROW_FORMAT, "ID", "STATUS", "PRIORITY", "ADMIN", "CATEGORY", "DESCRIPTION");
        System.out.println("-".repeat(100));
        if (complaints.isEmpty()) {
            System.out.println("No complaints match.");
            return;
        }
        for (Complaint c : complaints) {
            String truncatedDesc = c.getDescription().length() > 28
                    ? c.getDescription().substring(0, 25) + "..."
                    : c.getDescription();
            System.out.printf(ROW_FORMAT, c.getId(), c.getStatus(), c.getPriority(),
                    c.getAssignedAdmin() == null ? "-" : c.getAssignedAdmin(), c.getCategory(), truncatedDesc);
        }
    }
}
