package com.jasonbertolo.urlshortener.api.model;

import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Document("short-url")
public class ShortUrl {

    public static final String KEY_ALLOWED_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    @Id
    private String id;

    @Indexed(unique = true)
    @Field
    private String key;

    @Field
    private String url;

    @Field
    private String description;

    @CreatedDate
    @Field
    private Instant createdAt;

    @CreatedBy
    @Field
    private String createdBy;

    @LastModifiedDate
    @Field
    private Instant lastUpdatedAt;

    @LastModifiedBy
    @Field
    private String lastUpdatedBy;

    @Field
    private Instant expiresAt;

    public ShortUrl() {
    }

    private ShortUrl(Builder builder) {
        setKey(builder.key);
        setUrl(builder.url);
        setDescription(builder.description);
        setExpiresAt(builder.expiresAt);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(Instant lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public String getLastUpdatedBy() {
        return lastUpdatedBy;
    }

    public void setLastUpdatedBy(String lastUpdatedBy) {
        this.lastUpdatedBy = lastUpdatedBy;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    @Override
    public String toString() {
        return "ShortUrl{" +
                "id='" + id + '\'' +
                ", key='" + key + '\'' +
                ", url='" + url + '\'' +
                ", description='" + description + '\'' +
                ", createdAt=" + createdAt +
                ", createdBy='" + createdBy + '\'' +
                ", lastUpdatedAt=" + lastUpdatedAt +
                ", lastUpdatedBy='" + lastUpdatedBy + '\'' +
                ", expiresAt=" + expiresAt +
                '}';
    }

    public static final class Builder {
        private String key;
        private String url;
        private String description;
        private Instant expiresAt;

        public Builder() {
            // No-arg builder
        }

        public Builder key(String val) {
            key = val;
            return this;
        }

        public Builder url(String val) {
            url = val;
            return this;
        }

        public Builder description(String val) {
            description = val;
            return this;
        }

        public Builder expiresAt(Instant val) {
            expiresAt = val;
            return this;
        }

        public ShortUrl build() {
            return new ShortUrl(this);
        }
    }
}
