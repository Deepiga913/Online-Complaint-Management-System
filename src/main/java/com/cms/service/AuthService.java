package com.cms.service;

import com.cms.exception.DuplicateUserException;
import com.cms.exception.UnauthorizedAccessException;
import com.cms.exception.UserNotFoundException;
import com.cms.model.User;
import com.cms.repository.UserRepository;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles registration, credential verification, and lightweight in-memory
 * session tokens (role-based access is derived from the underlying User).
 */
public class AuthService {

    private final UserRepository userRepository;
    private final AsyncLoggerService logger;
    private final Map<String, String> sessions = new ConcurrentHashMap<>(); // token -> username

    public AuthService(UserRepository userRepository, AsyncLoggerService logger) {
        this.userRepository = userRepository;
        this.logger = logger;
    }

    public User register(String username, String rawPassword, String fullName, String email, boolean isAdmin)
            throws DuplicateUserException {
        String hash = hashPassword(rawPassword);
        User user = userRepository.addUser(username, hash, fullName, email, isAdmin);
        logger.log(username, "REGISTERED as " + user.getRole());
        return user;
    }

    public String login(String username, String rawPassword) throws UserNotFoundException, UnauthorizedAccessException {
        User user = userRepository.findByUsername(username);
        if (!user.getPasswordHash().equals(hashPassword(rawPassword))) {
            logger.log(username, "LOGIN_FAILED (bad password)");
            throw new UnauthorizedAccessException("Incorrect password for user '" + username + "'");
        }
        String token = UUID.randomUUID().toString();
        sessions.put(token, username);
        logger.log(username, "LOGIN_SUCCESS");
        return token;
    }

    public User getCurrentUser(String token) throws UnauthorizedAccessException, UserNotFoundException {
        String username = sessions.get(token);
        if (username == null) {
            throw new UnauthorizedAccessException("Session expired or invalid. Please log in again.");
        }
        return userRepository.findByUsername(username);
    }

    public void logout(String token) {
        String username = sessions.remove(token);
        if (username != null) {
            logger.log(username, "LOGOUT");
        }
    }

    public static String hashPassword(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawPassword.getBytes());
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
