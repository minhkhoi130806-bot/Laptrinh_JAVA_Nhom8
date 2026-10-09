
package com.restaurant.backend.exception;

import java.time.LocalDateTime;

public class ApiError {

    // Thoi gian xay ra loi
    private LocalDateTime timestamp;

    // Ma loi HTTP
    private int status;

    // Ten loi HTTP (Bad Request, Not Found, ...)
    private String error;

    // Noi dung loi
    private String message;

    // Duong dan API gay ra loi
    private String path;

    public ApiError() {
    }

    // Giu nguyen constructor cu de code cua cac thanh vien khac van chay
    // Noi dung loi
    private String message;

    public ApiError() {
    }

    public ApiError(int status, String message) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.message = message;
    }

    // Constructor day du thong tin loi
    public ApiError(int status, String error, String message, String path) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
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

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
}
