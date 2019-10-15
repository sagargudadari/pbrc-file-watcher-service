package com.mastercard.mcbs.pbrc.services.exception;

import lombok.Data;

import java.util.Date;

@Data
public class ErrorDetails {
    private String message;
    private String errors;
    private Date timestamp;

    public ErrorDetails(String message, String errors, Date timestamp) {
        super();
        this.timestamp = timestamp;
        this.errors = errors;
        this.message = message;
    }
}
