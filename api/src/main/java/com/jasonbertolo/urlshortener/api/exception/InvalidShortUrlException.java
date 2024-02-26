package com.jasonbertolo.urlshortener.api.exception;

import org.springframework.http.HttpStatus;

import java.net.URI;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

public class InvalidShortUrlException extends ApiException {

    public static final String TITLE = "Invalid ShortUrl";

    public InvalidShortUrlException(String detail) {
        super(detail);
    }

    public InvalidShortUrlException(String detail, URI instance) {
        super(detail, instance);
    }

    public InvalidShortUrlException(String detail, URI instance, Throwable cause) {
        super(detail, instance, cause);
    }

    @Override
    public URI getType() {
        return URI.create(getDocumentationBaseUrl() + "?problem=InvalidShortUrl");
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
