package com.jasonbertolo.urlshortener.api.config;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@ConfigurationPropertiesScan
@EnableScheduling
public class ApplicationConfig {
}
