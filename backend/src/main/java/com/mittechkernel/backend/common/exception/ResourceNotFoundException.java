package com.mittechkernel.backend.common.exception;

public class ResourceNotFoundException extends ApiException {
    public ResourceNotFoundException(String resource, Object id) {
        super("RESOURCE_NOT_FOUND", 404, resource + " with id " + id + " was not found");
    }
}
