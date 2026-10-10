package com.mittechkernel.backend.common.exception;

public class ExpiredTokenException extends ApiException {
    public ExpiredTokenException(String message) {
        super("TOKEN_EXPIRED", 401, message);
    }
}
