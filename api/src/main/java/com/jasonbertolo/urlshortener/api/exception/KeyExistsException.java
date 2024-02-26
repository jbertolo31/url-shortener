package com.jasonbertolo.urlshortener.api.exception;

import org.springframework.http.HttpStatus;

import java.net.URI;

import static org.springframework.http.HttpStatus.CONFLICT;

public class KeyExistsException extends ApiException {

    public static final String TITLE = "ShortUrl key exists";

    public KeyExistsException(String detail) {
        super(detail);
    }

    public KeyExistsException(String detail, URI instance) {
        super(detail, instance);
    }

    public KeyExistsException(String detail, URI instance, Throwable cause) {
        super(detail, instance, cause);
    }

    @Override
    public URI getType() {
        return URI.create(getDocumentationBaseUrl() + "?problem=KeyExists");
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    @Override
    public HttpStatus getStatus() {
        return CONFLICT;
    }
}
