package com.jasonbertolo.urlshortener.api.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jasonbertolo.urlshortener.api.component.SpringContext;
import com.jasonbertolo.urlshortener.api.config.settings.UrlShortenerSettings;
import org.springframework.validation.Errors;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ErrorsDto {

    @JsonIgnore
    private final Errors errors;

    public ErrorsDto(Errors errors) {
        this.errors = errors;
    }

    public String getObjectName() {
        return errors.getObjectName();
    }

    public Integer getFieldErrorCount() {
        return errors.getFieldErrorCount();
    }

    public List<FieldErrorDto> getFieldErrors() {
        UrlShortenerSettings urlShortenerSettings = SpringContext.getBean(UrlShortenerSettings.class);
        String reference = urlShortenerSettings.getDocumentationBaseUrl() + "#" + getObjectName();
        return errors.getFieldErrors().stream()
                .map(fe -> new FieldErrorDto(fe.getField(), (String)fe.getRejectedValue(), fe.getCode(), reference))
                .toList();
    }

    public record FieldErrorDto(String field, String rejectedValue, String reason, String reference){}
}
