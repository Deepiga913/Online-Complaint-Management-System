package com.cms.exception;

public class UserNotFoundException extends BaseException {
    public UserNotFoundException(String username) {
        super("No user found with username '" + username + "'");
    }
}
