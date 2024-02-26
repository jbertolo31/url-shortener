package com.jasonbertolo.urlshortener.api.model.dto;

public class ShortUrlCreateDto {

    private String url;

    private String description;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
