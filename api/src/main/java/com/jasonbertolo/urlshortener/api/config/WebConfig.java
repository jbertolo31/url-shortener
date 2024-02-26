package com.jasonbertolo.urlshortener.api.config;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.jasonbertolo.urlshortener.api.handler.ShortUrlHandler;
import com.jasonbertolo.urlshortener.api.springdoc.ShortUrlSpringDoc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.path;

@Configuration
@EnableWebFlux
public class WebConfig implements WebFluxConfigurer {

    @Override
    public void configureHttpMessageCodecs(@NonNull ServerCodecConfigurer configurer) {
        configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(
                Jackson2ObjectMapperBuilder
                        .json()
                        .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                        .build()));
    }

    @ShortUrlSpringDoc
    @Bean
    public RouterFunction<ServerResponse> routerFunction(ShortUrlHandler shortUrlHandler) {
        return RouterFunctions.route()
                .nest(path("/api/v1"), baseUrlBuilder -> baseUrlBuilder
                        .GET("/cache/{key}", shortUrlHandler::getAndCacheShortUrl)
                        .POST("/shorturl", shortUrlHandler::createUserShortUrl)
                        .GET("/shorturl", shortUrlHandler::getUserShortUrls)
                        .GET("/shorturl/{id}", shortUrlHandler::getUserShortUrl)
                        .DELETE("/shorturl/{id}", shortUrlHandler::deleteUserShortUrl)
                )
                .build();
    }
}
