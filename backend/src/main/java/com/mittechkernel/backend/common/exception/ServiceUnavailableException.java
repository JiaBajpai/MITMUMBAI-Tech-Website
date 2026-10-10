package com.mittechkernel.backend.common.exception;

public class ServiceUnavailableException extends ApiException {
    public ServiceUnavailableException(String message) {
        super("SERVICE_UNAVAILABLE", 503, message);
    }
}
