package com.cms.service;

import com.cms.exception.ComplaintNotFoundException;
import com.cms.exception.InvalidComplaintStateException;
import com.cms.model.Complaint;
import com.cms.model.ComplaintStatus;
import com.cms.model.Feedback;
import com.cms.model.Priority;
import com.cms.repository.ComplaintRepository;
import com.cms.util.FileHandler;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Core business logic: filing complaints, validated status transitions,
 * stream/lambda-based filtering, feedback capture, and analytics.
 */
public class ComplaintService {

    private static final DateTimeFormatter ID_TS_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String FEEDBACK_HEADER = "id,complaintId,customerUsername,rating,comment,createdAt";

    private final ComplaintRepository complaintRepository;
    private final AsyncLoggerService logger;
    private final Path feedbackFile;
    private final AtomicInteger sequenceWithinSecond = new AtomicInteger(0);

    public ComplaintService(ComplaintRepository complaintRepository, AsyncLoggerService logger, Path feedbackFile) {
        this.complaintRepository = complaintRepository;
        this.logger = logger;
        this.feedbackFile = feedbackFile;
        try {
            FileHandler.ensureFileWithHeader(feedbackFile, FEEDBACK_HEADER);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to initialize feedback.csv", e);
        }
    }

    /** Generates a unique, sortable complaint ID: CMP-<timestamp>-<seq>. */
    private String generateComplaintId() {
        String ts = LocalDateTime.now().format(ID_TS_FMT);
        int seq = sequenceWithinSecond.updateAndGet(v -> (v + 1) % 1000);
        return String.format("CMP-%s-%03d", ts, seq);
    }

    public Complaint fileComplaint(String customerUsername, String category, String description, Priority priority) {
        Complaint complaint = Complaint.newComplaint(generateComplaintId(), customerUsername, category, description, priority);
        complaintRepository.save(complaint);
        logger.log(customerUsername, "FILED complaint " + complaint.getId());
        return complaint;
    }

    public void updateStatus(String complaintId, ComplaintStatus newStatus, String actingAdmin)
            throws ComplaintNotFoundException, InvalidComplaintStateException {
        Complaint complaint = complaintRepository.findById(complaintId);
        ComplaintStatus previous = complaint.getStatus();
        complaint.updateStatus(newStatus);
        complaintRepository.update(complaint);
        logger.log(actingAdmin, "UPDATED " + complaintId + " status " + previous + " -> " + newStatus);
    }

    public void assignToAdmin(String complaintId, String adminUsername) throws ComplaintNotFoundException {
        Complaint complaint = complaintRepository.findById(complaintId);
        complaint.setAssignedAdmin(adminUsername);
        complaintRepository.update(complaint);
        logger.log(adminUsername, "ASSIGNED " + complaintId + " to self");
    }

    public List<Complaint> listByCustomer(String username) {
        return complaintRepository.getByCustomer(username);
    }

    public List<Complaint> listAll() {
        return complaintRepository.filter(c -> true);
    }

    // ---- Stream/lambda-based dynamic filtering ----

    public List<Complaint> filterByCategory(String category) {
        return complaintRepository.filter(c -> c.getCategory().equalsIgnoreCase(category));
    }

    public List<Complaint> filterByPriority(Priority priority) {
        return complaintRepository.filter(c -> c.getPriority() == priority);
    }

    public List<Complaint> filterByStatus(ComplaintStatus status) {
        return complaintRepository.filter(c -> c.getStatus() == status);
    }

    public List<Complaint> filterByDateRange(LocalDate from, LocalDate to) {
        return complaintRepository.filter(c -> {
            LocalDate created = c.getCreatedAt().toLocalDate();
            return !created.isBefore(from) && !created.isAfter(to);
        });
    }

    // ---- Feedback ----

    public Feedback submitFeedback(String complaintId, String customerUsername, int rating, String comment)
            throws ComplaintNotFoundException, InvalidComplaintStateException {
        Complaint complaint = complaintRepository.findById(complaintId);
        if (complaint.getStatus() != ComplaintStatus.RESOLVED && complaint.getStatus() != ComplaintStatus.CLOSED) {
            throw new InvalidComplaintStateException(complaint.getStatus(), ComplaintStatus.CLOSED);
        }
        Feedback feedback = new Feedback(UUID.randomUUID().toString().substring(0, 8),
                complaintId, customerUsername, rating, comment, LocalDateTime.now());
        try {
            FileHandler.appendLine(feedbackFile, FileHandler.toCsvLine(feedback.toCsvRow()));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save feedback", e);
        }
        logger.log(customerUsername, "SUBMITTED feedback for " + complaintId);
        return feedback;
    }

    private List<Feedback> loadAllFeedback() {
        try {
            return FileHandler.readCsv(feedbackFile).stream()
                    .map(Feedback::fromCsvRow)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read feedback.csv", e);
        }
    }

    // ---- Analytics ----

    /** Immutable analytics snapshot (Java record) returned to the admin dashboard. */
    public record AnalyticsSummary(
            long totalComplaints,
            double averageResolutionHours,
            double averageSatisfactionRating,
            Map<String, Long> countByCategory,
            Map<Priority, Long> countByPriority,
            Map<ComplaintStatus, Long> countByStatus
    ) {
    }

    public AnalyticsSummary computeAnalytics() {
        List<Complaint> all = complaintRepository.filter(c -> true);

        double avgResolutionHours = all.stream()
                .filter(c -> c.getResolvedAt() != null)
                .mapToLong(c -> Duration.between(c.getCreatedAt(), c.getResolvedAt()).toMinutes())
                .average()
                .stream().map(m -> m / 60.0)
                .findFirst()
                .orElse(0.0);

        Map<String, Long> byCategory = all.stream()
                .collect(Collectors.groupingBy(Complaint::getCategory, Collectors.counting()));

        Map<Priority, Long> byPriority = all.stream()
                .collect(Collectors.groupingBy(Complaint::getPriority, Collectors.counting()));

        Map<ComplaintStatus, Long> byStatus = all.stream()
                .collect(Collectors.groupingBy(Complaint::getStatus, Collectors.counting()));

        double avgSatisfaction = loadAllFeedback().stream()
                .mapToInt(Feedback::getRating)
                .average()
                .orElse(0.0);

        return new AnalyticsSummary(all.size(), avgResolutionHours, avgSatisfaction, byCategory, byPriority, byStatus);
    }
}
