package com.cms.model;

public class Admin extends User {

    public Admin(int id, String username, String passwordHash, String fullName, String email) {
        super(id, username, passwordHash, fullName, email);
    }

    @Override
    public String getRole() {
        return "ADMIN";
    }
}
