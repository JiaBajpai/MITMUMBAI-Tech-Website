package com.mittechkernel.backend.common.exception;

public class UnauthorizedException extends ApiException {
    public UnauthorizedException(String message) {
        super("UNAUTHORIZED", 401, message);
    }
}
