package com.mittechkernel.backend.common.exception;

public class UpstreamServiceException extends ApiException {
    public UpstreamServiceException(String message) {
        super("UPSTREAM_SERVICE_ERROR", 502, message);
    }
}
