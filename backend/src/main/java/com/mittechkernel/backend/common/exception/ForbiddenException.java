package com.mittechkernel.backend.common.exception;

public class ForbiddenException extends ApiException {
    public ForbiddenException(String message) {
        super("FORBIDDEN", 403, message);
    }
}
