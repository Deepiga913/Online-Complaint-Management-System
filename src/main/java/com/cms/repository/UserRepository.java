package com.cms.repository;

import com.cms.exception.DuplicateUserException;
import com.cms.exception.UserNotFoundException;
import com.cms.model.Admin;
import com.cms.model.Customer;
import com.cms.model.User;
import com.cms.util.FileHandler;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Loads users.csv into an in-memory HashMap on startup so that authentication
 * lookups are O(1) instead of re-scanning the file on every login.
 * Every mutation is flushed back to disk immediately (atomic rewrite) so the
 * cache and the file on disk never drift apart.
 */
public class UserRepository {

    private static final String HEADER = "id,username,passwordHash,role,fullName,email";
    private final Path filePath;
    private final Map<String, User> usersByUsername = new LinkedHashMap<>();
    private final AtomicInteger idCounter = new AtomicInteger(0);

    public UserRepository(Path filePath) {
        this.filePath = filePath;
        load();
    }

    private void load() {
        try {
            FileHandler.ensureFileWithHeader(filePath, HEADER);
            List<String[]> rows = FileHandler.readCsv(filePath);
            for (String[] r : rows) {
                User user = toUser(r);
                usersByUsername.put(user.getUsername(), user);
                idCounter.updateAndGet(current -> Math.max(current, user.getId()));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load users.csv", e);
        }
    }

    private User toUser(String[] r) {
        int id = Integer.parseInt(r[0]);
        String username = r[1];
        String passwordHash = r[2];
        String role = r[3];
        String fullName = r[4];
        String email = r[5];
        return "ADMIN".equals(role)
                ? new Admin(id, username, passwordHash, fullName, email)
                : new Customer(id, username, passwordHash, fullName, email);
    }

    public synchronized User addUser(String username, String passwordHash, String fullName,
                                      String email, boolean isAdmin) throws DuplicateUserException {
        if (usersByUsername.containsKey(username)) {
            throw new DuplicateUserException(username);
        }
        int newId = idCounter.incrementAndGet();
        User user = isAdmin
                ? new Admin(newId, username, passwordHash, fullName, email)
                : new Customer(newId, username, passwordHash, fullName, email);
        usersByUsername.put(username, user);
        persist();
        return user;
    }

    public User findByUsername(String username) throws UserNotFoundException {
        User user = usersByUsername.get(username);
        if (user == null) {
            throw new UserNotFoundException(username);
        }
        return user;
    }

    public boolean existsByUsername(String username) {
        return usersByUsername.containsKey(username);
    }

    public synchronized void updateUser(User user) {
        usersByUsername.put(user.getUsername(), user);
        persist();
    }

    public Collection<User> getAllUsers() {
        return usersByUsername.values();
    }

    private void persist() {
        try {
            List<String[]> rows = new ArrayList<>();
            for (User u : usersByUsername.values()) {
                rows.add(u.toCsvRow());
            }
            FileHandler.writeCsvAtomic(filePath, HEADER, rows);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to persist users.csv", e);
        }
    }
}
