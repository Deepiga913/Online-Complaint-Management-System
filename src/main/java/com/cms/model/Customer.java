package com.cms.model;

public class Customer extends User {

    public Customer(int id, String username, String passwordHash, String fullName, String email) {
        super(id, username, passwordHash, fullName, email);
    }

    @Override
    public String getRole() {
        return "CUSTOMER";
    }
}
