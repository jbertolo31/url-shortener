package com.jasonbertolo.urlshortener.api.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jasonbertolo.urlshortener.api.model.ShortUrl;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ShortUrlResponseDto {

    @JsonIgnore
    private final ShortUrl shortUrl;

    public ShortUrlResponseDto(ShortUrl shortUrl) {
        this.shortUrl = shortUrl;
    }

    public String getId() {
        return shortUrl.getId();
    }

    public String getKey() {
        return shortUrl.getKey();
    }

    public String getUrl() {
        return shortUrl.getUrl();
    }

    public String getDescription() {
        return shortUrl.getDescription();
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    public Instant getCreatedAt() {
        return shortUrl.getCreatedAt();
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    public Instant getLastUpdatedAt() {
        return shortUrl.getLastUpdatedAt();
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    public Instant getExpiresAt() {
        return shortUrl.getExpiresAt();
    }
}
