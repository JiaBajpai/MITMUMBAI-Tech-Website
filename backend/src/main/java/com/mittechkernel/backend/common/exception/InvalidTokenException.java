package com.mittechkernel.backend.common.exception;

public class InvalidTokenException extends ApiException {
    public InvalidTokenException(String message) {
        super("INVALID_TOKEN", 401, message);
    }
}
