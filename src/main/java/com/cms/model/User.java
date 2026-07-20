package com.cms.model;

/**
 * Base class for anyone who can log into the system.
 * Concrete subtypes (Customer, Admin) define their own role label.
 */
public abstract class User {

    private final int id;
    private final String username;
    private String passwordHash;
    private String fullName;
    private String email;

    protected User(int id, String username, String passwordHash, String fullName, String email) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.email = email;
    }

    public abstract String getRole();

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    /** Serializes this user to a CSV row: id,username,passwordHash,role,fullName,email */
    public String[] toCsvRow() {
        return new String[]{
                String.valueOf(id), username, passwordHash, getRole(), fullName, email
        };
    }

    @Override
    public String toString() {
        return "[" + getRole() + "] " + username + " (" + fullName + ")";
    }
}
