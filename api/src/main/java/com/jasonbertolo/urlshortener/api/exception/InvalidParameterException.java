package com.jasonbertolo.urlshortener.api.exception;

import org.springframework.http.HttpStatus;

import java.net.URI;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

public class InvalidParameterException extends ApiException {

    public static final String TITLE = "Invalid parameter";

    public InvalidParameterException(String detail) {
        super(detail);
    }

    public InvalidParameterException(String detail, URI instance) {
        super(detail, instance);
    }

    public InvalidParameterException(String detail, URI instance, Throwable cause) {
        super(detail, instance, cause);
    }

    @Override
    public URI getType() {
        return URI.create(getDocumentationBaseUrl() + "?problem=InvalidParameter");
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    @Override
    public HttpStatus getStatus() {
        return BAD_REQUEST;
    }
}
