package com.cms.repository;

import com.cms.exception.ComplaintNotFoundException;
import com.cms.model.Complaint;
import com.cms.util.FileHandler;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * HashMap-cached complaint store. Reads complaints.csv into memory once on
 * startup for instant O(1) status lookups/updates; every write (insert or
 * edit) rewrites the whole file atomically via FileHandler so a crash mid-write
 * can never corrupt neighboring rows.
 */
public class ComplaintRepository {

    private static final String HEADER =
            "id,customerUsername,category,description,priority,status,createdAt,updatedAt,resolvedAt,assignedAdmin";

    private final Path filePath;
    private final Map<String, Complaint> complaintsById = new LinkedHashMap<>();

    public ComplaintRepository(Path filePath) {
        this.filePath = filePath;
        load();
    }

    private void load() {
        try {
            FileHandler.ensureFileWithHeader(filePath, HEADER);
            List<String[]> rows = FileHandler.readCsv(filePath);
            for (String[] r : rows) {
                Complaint c = Complaint.fromCsvRow(r);
                complaintsById.put(c.getId(), c);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load complaints.csv", e);
        }
    }

    public synchronized void save(Complaint complaint) {
        complaintsById.put(complaint.getId(), complaint);
        persist();
    }

    /** Re-persists an already-mutated complaint; named distinctly for readability at call sites. */
    public synchronized void update(Complaint complaint) {
        save(complaint);
    }

    public Complaint findById(String id) throws ComplaintNotFoundException {
        Complaint c = complaintsById.get(id);
        if (c == null) {
            throw new ComplaintNotFoundException(id);
        }
        return c;
    }

    public Collection<Complaint> getAll() {
        return complaintsById.values();
    }

    public List<Complaint> getByCustomer(String username) {
        return complaintsById.values().stream()
                .filter(c -> c.getCustomerUsername().equals(username))
                .collect(Collectors.toList());
    }

    /** Generic stream-based filter so callers can compose category/priority/date-range predicates freely. */
    public List<Complaint> filter(Predicate<Complaint> predicate) {
        return complaintsById.values().stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }

    private void persist() {
        try {
            List<String[]> rows = new ArrayList<>();
            for (Complaint c : complaintsById.values()) {
                rows.add(c.toCsvRow());
            }
            FileHandler.writeCsvAtomic(filePath, HEADER, rows);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to persist complaints.csv", e);
        }
    }
}
