package com.jasonbertolo.urlshortener.api.config.settings;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "url-shortener")
public class UrlShortenerSettings {

    private Integer keyLength;
    private Integer urlTtlDays;
    private Integer cacheTtlDays;
    private String externalAppUrl;
    private String documentationBaseUrl;
    private ScheduledMaintenance scheduledMaintenance;

    public Integer getKeyLength() {
        return keyLength;
    }

    public void setKeyLength(Integer keyLength) {
        this.keyLength = keyLength;
    }

    public Integer getUrlTtlDays() {
        return urlTtlDays;
    }

    public void setUrlTtlDays(Integer urlTtlDays) {
        this.urlTtlDays = urlTtlDays;
    }

    public Integer getCacheTtlDays() {
        return cacheTtlDays;
    }

    public void setCacheTtlDays(Integer cacheTtlDays) {
        this.cacheTtlDays = cacheTtlDays;
    }

    public String getExternalAppUrl() {
        return externalAppUrl;
    }

    public void setExternalAppUrl(String externalAppUrl) {
        this.externalAppUrl = externalAppUrl;
    }

    public String getDocumentationBaseUrl() {
        return documentationBaseUrl;
    }

    public void setDocumentationBaseUrl(String documentationBaseUrl) {
        this.documentationBaseUrl = documentationBaseUrl;
    }

    public ScheduledMaintenance getScheduledMaintenance() {
        return scheduledMaintenance;
    }

    public void setScheduledMaintenance(ScheduledMaintenance scheduledMaintenance) {
        this.scheduledMaintenance = scheduledMaintenance;
    }

    public static class ScheduledMaintenance {
        private String cronZone;
        private boolean cleanupEnabled;
        private String cleanupCron;

        public String getCronZone() {
            return cronZone;
        }

        public void setCronZone(String cronZone) {
            this.cronZone = cronZone;
        }

        public boolean isCleanupEnabled() {
            return cleanupEnabled;
        }

        public void setCleanupEnabled(boolean cleanupEnabled) {
            this.cleanupEnabled = cleanupEnabled;
        }

        public String getCleanupCron() {
            return cleanupCron;
        }

        public void setCleanupCron(String cleanupCron) {
            this.cleanupCron = cleanupCron;
        }
    }
}
