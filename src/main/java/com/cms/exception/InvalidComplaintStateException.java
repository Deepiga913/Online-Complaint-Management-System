package com.cms.exception;

import com.cms.model.ComplaintStatus;

public class InvalidComplaintStateException extends BaseException {
    public InvalidComplaintStateException(ComplaintStatus from, ComplaintStatus to) {
        super("Cannot transition complaint from " + from + " to " + to);
    }
}
