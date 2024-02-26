package com.jasonbertolo.urlshortener.web.config.settings;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "microservices")
public class MicroservicesSettings {

    private String apiBaseUrl;
    private String authBaseUrl;
    private String bffBaseUrl;

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    public String getAuthBaseUrl() {
        return authBaseUrl;
    }

    public void setAuthBaseUrl(String authBaseUrl) {
        this.authBaseUrl = authBaseUrl;
    }

    public String getBffBaseUrl() {
        return bffBaseUrl;
    }

    public void setBffBaseUrl(String bffBaseUrl) {
        this.bffBaseUrl = bffBaseUrl;
    }
}
