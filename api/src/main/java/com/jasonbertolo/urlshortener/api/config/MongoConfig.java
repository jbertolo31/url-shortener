package com.jasonbertolo.urlshortener.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;

@Configuration
@EnableReactiveMongoAuditing
public class MongoConfig {

    @Bean
    public ReactiveAuditorAware<String> reactiveAuditorAware() {
        // Set @CreatedBy/@LastModifiedBy fields to Security Principal name
        return () -> ReactiveSecurityContextHolder.getContext()
                .mapNotNull(SecurityContext::getAuthentication)
                .mapNotNull(Authentication::getName);
    }
}
