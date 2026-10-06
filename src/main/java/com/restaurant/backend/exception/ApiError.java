package com.restaurant.backend.exception;

import java.time.LocalDateTime;

public class ApiError {

    // Thoi gian xay ra loi
    private LocalDateTime timestamp;

    // Ma loi HTTP
    private int status;

    // Noi dung loi
    private String message;

    public ApiError() {
    }

    public ApiError(int status, String message) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}