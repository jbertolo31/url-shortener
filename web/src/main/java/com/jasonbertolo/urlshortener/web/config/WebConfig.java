package com.jasonbertolo.urlshortener.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.jasonbertolo.urlshortener.web.config.settings.UiSettings;
import com.jasonbertolo.urlshortener.web.handler.SupportHandler;
import com.jasonbertolo.urlshortener.web.handler.WebAppHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class WebConfig implements WebFluxConfigurer {

    @Value("classpath:/static/index.html")
    private Resource indexHtml;

    @Override
    public void configureHttpMessageCodecs(@NonNull ServerCodecConfigurer configurer) {
        configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(
                Jackson2ObjectMapperBuilder
                        .json()
                        .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                        .build()));
    }

    @Bean
    public RouterFunction<ServerResponse> routerFunction(UiSettings uiSettings, SupportHandler supportHandler,
                                                         WebAppHandler webAppHandler) {
        RouterFunctions.Builder builder = route()
                // Support
                .GET("/jwt", supportHandler::getJwt)
                .GET("/config.js", supportHandler::getUiConfig)

                // Web
                .GET("/u/{key}", webAppHandler::redirect)

                // Thymeleaf views
                .GET("/", webAppHandler::indexPage)
                .GET("/my-short-urls", webAppHandler::myShortUrlsPage)
                .GET("/u/{key}/r", webAppHandler::revealPage)
                .GET("/u/{key}/reveal", webAppHandler::revealPage)
                .GET("/error", webAppHandler::errorPage);

        // Reserved for React frontend. Not currently used.
        builder.GET("/index", webAppHandler::indexPage);

        //uiSettings.getBrowserRoutingPaths().forEach(path -> builder.GET(path, request -> ok()
        //        .contentType(TEXT_HTML)
        //        .bodyValue(indexHtml))); //NOSONAR

        return builder.build();
    }

    @Bean("apiWebClient")
    public WebClient apiWebClient(ReactiveOAuth2AuthorizedClientManager authorizedClientManager) {
        ServerOAuth2AuthorizedClientExchangeFilterFunction filterFunction =
                new ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
        filterFunction.setDefaultClientRegistrationId("url_shortener_cache");
        ObjectMapper snakeCaseMapper = Jackson2ObjectMapperBuilder.json()
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .build();
        return WebClient.builder()
                .filter(filterFunction)
                .codecs(clientDefaultCodecsConfigurer -> {
                    clientDefaultCodecsConfigurer.defaultCodecs()
                            .jackson2JsonEncoder(new Jackson2JsonEncoder(snakeCaseMapper, APPLICATION_JSON));
                    clientDefaultCodecsConfigurer.defaultCodecs()
                            .jackson2JsonDecoder(new Jackson2JsonDecoder(snakeCaseMapper, APPLICATION_JSON));
                })
                .build();
    }
}
