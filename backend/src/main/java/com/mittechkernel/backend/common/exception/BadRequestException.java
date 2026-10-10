package com.mittechkernel.backend.common.exception;

public class BadRequestException extends ApiException {
    public BadRequestException(String message) {
        super("BAD_REQUEST", 400, message);
    }
}
