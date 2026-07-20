package com.cms.exception;

public class ComplaintNotFoundException extends BaseException {
    public ComplaintNotFoundException(String complaintId) {
        super("No complaint found with ID '" + complaintId + "'");
    }
}
