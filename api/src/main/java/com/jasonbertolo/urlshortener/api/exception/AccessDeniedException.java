package com.jasonbertolo.urlshortener.api.exception;

import org.springframework.http.HttpStatus;

import java.net.URI;

import static org.springframework.http.HttpStatus.FORBIDDEN;

public class AccessDeniedException extends ApiException {

    public static final String TITLE = "Access denied";

    public static final String USER_AUTHENTICATION_REQUIRED = "User authentication is required";

    public AccessDeniedException(String detail) {
        super(detail);
    }

    public AccessDeniedException(String detail, URI instance) {
        super(detail, instance);
    }

    public AccessDeniedException(String detail, URI instance, Throwable cause) {
        super(detail, instance, cause);
    }

    @Override
    public URI getType() {
        return URI.create(getDocumentationBaseUrl() + "?problem=AccessDenied");
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    @Override
    public HttpStatus getStatus() {
        return FORBIDDEN;
    }
}
