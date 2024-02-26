package com.jasonbertolo.urlshortener.web.config.settings;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "ui")
public class UiSettings {

    private List<String> browserRoutingPaths;
    private Map<String, Object> frontendConfig;

    public List<String> getBrowserRoutingPaths() {
        return browserRoutingPaths;
    }

    public void setBrowserRoutingPaths(List<String> browserRoutingPaths) {
        this.browserRoutingPaths = browserRoutingPaths;
    }

    public Map<String, Object> getFrontendConfig() {
        return frontendConfig;
    }

    public void setFrontendConfig(Map<String, Object> frontendConfig) {
        this.frontendConfig = frontendConfig;
    }
}
