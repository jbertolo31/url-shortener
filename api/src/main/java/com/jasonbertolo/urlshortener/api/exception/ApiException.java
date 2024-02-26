package com.jasonbertolo.urlshortener.api.exception;

import com.jasonbertolo.urlshortener.api.component.SpringContext;
import com.jasonbertolo.urlshortener.api.config.settings.UrlShortenerSettings;
import com.jasonbertolo.urlshortener.api.model.dto.ErrorsDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

public abstract class ApiException extends RuntimeException {

    private final transient ProblemDetail problemDetail;

    protected ApiException(String detail) {
        this(detail, null);
    }

    protected ApiException(String detail, URI instance) {
        super(detail);
        this.problemDetail = ProblemDetail.forStatusAndDetail(getStatus(), detail);
        this.problemDetail.setType(getType());
        this.problemDetail.setTitle(getTitle());
        this.problemDetail.setInstance(instance);
        enrichProblemDetail();
    }

    protected ApiException(String detail, URI instance, Throwable cause) {
        super(detail, cause);
        this.problemDetail = ProblemDetail.forStatusAndDetail(getStatus(), detail);
        this.problemDetail.setType(getType());
        this.problemDetail.setTitle(getTitle());
        this.problemDetail.setInstance(instance);
        enrichProblemDetail();
    }

    public ProblemDetail getProblemDetail() {
        return problemDetail;
    }

    public void enrichProblemDetail() {
        problemDetail.setProperty("timestamp", Instant.now().toEpochMilli());
        problemDetail.setProperty("traceId", UUID.randomUUID().toString());
    }

    public String getDocumentationBaseUrl() {
        UrlShortenerSettings urlShortenerSettings = SpringContext.getBean(UrlShortenerSettings.class);
        return urlShortenerSettings.getDocumentationBaseUrl();
    }

    public ApiException withErrors(ErrorsDto errors) {
        problemDetail.setProperty("errors", errors);
        return this;
    }

    public abstract URI getType();

    public abstract String getTitle();

    public abstract HttpStatus getStatus();
}
