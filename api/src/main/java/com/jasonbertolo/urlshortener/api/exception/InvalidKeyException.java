package com.jasonbertolo.urlshortener.api.exception;

import org.springframework.http.HttpStatus;

import java.net.URI;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

public class InvalidKeyException extends ApiException {

    public static final String TITLE = "Invalid ShortUrl key";

    public InvalidKeyException(String detail) {
        super(detail);
    }

    public InvalidKeyException(String detail, URI instance) {
        super(detail, instance);
    }

    public InvalidKeyException(String detail, URI instance, Throwable cause) {
        super(detail, instance, cause);
    }

    @Override
    public URI getType() {
        return URI.create(getDocumentationBaseUrl() + "?problem=InvalidShortUrlKey");
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
