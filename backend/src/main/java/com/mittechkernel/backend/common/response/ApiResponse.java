package com.mittechkernel.backend.common.response;

import jakarta.servlet.http.HttpServletRequest;

import java.time.Instant;

public record ApiResponse<T>(
        Instant timestamp,
        int status,
        boolean success,
        T data,
        String error,
        String message,
        String path
) {
    public static <T> ApiResponse<T> success(T data, HttpServletRequest request) {
        return new ApiResponse<>(Instant.now(), 200, true, data, null, null, request.getRequestURI());
    }

    public static <T> ApiResponse<T> success(T data, HttpServletRequest request, int status) {
        return new ApiResponse<>(Instant.now(), status, true, data, null, null, request.getRequestURI());
    }

    public static <T> ApiResponse<T> error(int status, String error, String message, String path) {
        return new ApiResponse<>(Instant.now(), status, false, null, error, message, path);
    }
}
