package com.jasonbertolo.urlshortener.api.exception;

import org.springframework.http.HttpStatus;

import java.net.URI;

import static org.springframework.http.HttpStatus.NOT_FOUND;

public class ResourceNotFoundException extends ApiException {

    public static final String TITLE = "Resource not found";

    public ResourceNotFoundException(String detail) {
        super(detail);
    }

    public ResourceNotFoundException(String detail, URI instance) {
        super(detail, instance);
    }

    public ResourceNotFoundException(String detail, URI instance, Throwable cause) {
        super(detail, instance, cause);
    }

    @Override
    public URI getType() {
        return URI.create(getDocumentationBaseUrl() + "?problem=ResourceNotFound");
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    @Override
    public HttpStatus getStatus() {
        return NOT_FOUND;
    }
}
