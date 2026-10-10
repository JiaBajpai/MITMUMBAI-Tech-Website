package com.mittechkernel.backend.common.exception;

public class InvalidCredentialsException extends ApiException {
    public InvalidCredentialsException() {
        super("INVALID_CREDENTIALS", 401, "Invalid email or password");
    }
}
