package com.cms.exception;

public class DuplicateUserException extends BaseException {
    public DuplicateUserException(String username) {
        super("Username '" + username + "' is already taken");
    }
}
