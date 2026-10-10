package com.mittechkernel.backend.common.exception;

public class DeliveryUnavailableException extends ApiException {
    public DeliveryUnavailableException() {
        super("DELIVERY_UNAVAILABLE", 503, "Password setup email delivery is not configured");
    }
}
