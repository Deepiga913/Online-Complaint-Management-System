package com.cms.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Customer satisfaction feedback tied directly to a resolved/closed complaint.
 */
public class Feedback {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final String id;
    private final String complaintId;
    private final String customerUsername;
    private final int rating; // 1-5
    private final String comment;
    private final LocalDateTime createdAt;

    public Feedback(String id, String complaintId, String customerUsername, int rating,
                     String comment, LocalDateTime createdAt) {
        this.id = id;
        this.complaintId = complaintId;
        this.customerUsername = customerUsername;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getComplaintId() {
        return complaintId;
    }

    public String getCustomerUsername() {
        return customerUsername;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** Row order: id,complaintId,customerUsername,rating,comment,createdAt */
    public String[] toCsvRow() {
        return new String[]{id, complaintId, customerUsername, String.valueOf(rating), comment, createdAt.format(FMT)};
    }

    public static Feedback fromCsvRow(String[] r) {
        return new Feedback(r[0], r[1], r[2], Integer.parseInt(r[3]), r[4], LocalDateTime.parse(r[5], FMT));
    }

    @Override
    public String toString() {
        return String.format("Feedback[%s] complaint=%s rating=%d/5 : %s", id, complaintId, rating, comment);
    }
}
