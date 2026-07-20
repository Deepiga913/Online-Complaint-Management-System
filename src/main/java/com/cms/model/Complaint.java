package com.cms.model;

import com.cms.exception.InvalidComplaintStateException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A customer-filed complaint. Holds its own state-transition validation
 * so no controller/service can push it into an illegal status by accident.
 */
public class Complaint {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final String EMPTY = "";

    private final String id;
    private final String customerUsername;
    private String category;
    private String description;
    private Priority priority;
    private ComplaintStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt; // nullable
    private String assignedAdmin;    // nullable

    public Complaint(String id, String customerUsername, String category, String description,
                      Priority priority, ComplaintStatus status, LocalDateTime createdAt,
                      LocalDateTime updatedAt, LocalDateTime resolvedAt, String assignedAdmin) {
        this.id = id;
        this.customerUsername = customerUsername;
        this.category = category;
        this.description = description;
        this.priority = priority;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.resolvedAt = resolvedAt;
        this.assignedAdmin = assignedAdmin;
    }

    /** Factory for freshly-filed complaints (NEW status, no resolution/assignment yet). */
    public static Complaint newComplaint(String id, String customerUsername, String category,
                                          String description, Priority priority) {
        LocalDateTime now = LocalDateTime.now();
        return new Complaint(id, customerUsername, category, description, priority,
                ComplaintStatus.NEW, now, now, null, null);
    }

    /** Applies a validated state transition, stamping resolvedAt/updatedAt as appropriate. */
    public void updateStatus(ComplaintStatus newStatus) throws InvalidComplaintStateException {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new InvalidComplaintStateException(this.status, newStatus);
        }
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
        if (newStatus == ComplaintStatus.RESOLVED) {
            this.resolvedAt = LocalDateTime.now();
        } else if (newStatus == ComplaintStatus.IN_PROGRESS) {
            this.resolvedAt = null; // reopened
        }
    }

    public String getId() {
        return id;
    }

    public String getCustomerUsername() {
        return customerUsername;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
        this.updatedAt = LocalDateTime.now();
    }

    public ComplaintStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public String getAssignedAdmin() {
        return assignedAdmin;
    }

    public void setAssignedAdmin(String assignedAdmin) {
        this.assignedAdmin = assignedAdmin;
        this.updatedAt = LocalDateTime.now();
    }

    /** Row order: id,customerUsername,category,description,priority,status,createdAt,updatedAt,resolvedAt,assignedAdmin */
    public String[] toCsvRow() {
        return new String[]{
                id,
                customerUsername,
                category,
                description,
                priority.name(),
                status.name(),
                createdAt.format(FMT),
                updatedAt.format(FMT),
                resolvedAt == null ? EMPTY : resolvedAt.format(FMT),
                assignedAdmin == null ? EMPTY : assignedAdmin
        };
    }

    public static Complaint fromCsvRow(String[] r) {
        LocalDateTime resolved = (r[8] == null || r[8].isBlank()) ? null : LocalDateTime.parse(r[8], FMT);
        String admin = (r[9] == null || r[9].isBlank()) ? null : r[9];
        return new Complaint(
                r[0], r[1], r[2], r[3],
                Priority.valueOf(r[4]),
                ComplaintStatus.valueOf(r[5]),
                LocalDateTime.parse(r[6], FMT),
                LocalDateTime.parse(r[7], FMT),
                resolved,
                admin
        );
    }

    @Override
    public String toString() {
        return String.format("%-14s [%-11s] (%-6s) %-15s %s",
                id, status, priority, category, description);
    }
}
