package com.jasonbertolo.urlshortener.web.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jasonbertolo.urlshortener.web.config.settings.UiSettings;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static org.owasp.esapi.Logger.EVENT_FAILURE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@Component
public class SupportHandler {

    private static final Logger LOGGER = ESAPI.getLogger(SupportHandler.class.getSimpleName());

    private final ReactiveOAuth2AuthorizedClientManager authorizedClientManager;
    private final UiSettings uiSettings;
    private final ObjectMapper objectMapper;

    @Autowired
    public SupportHandler(ReactiveOAuth2AuthorizedClientManager authorizedClientManager, UiSettings uiSettings,
                          ObjectMapper objectMapper) {
        this.authorizedClientManager = authorizedClientManager;
        this.uiSettings = uiSettings;
        this.objectMapper = objectMapper;
    }

    @NonNull
    public Mono<ServerResponse> getJwt(@SuppressWarnings("unused") ServerRequest serverRequest) {
        return ReactiveSecurityContextHolder.getContext()
                .mapNotNull(SecurityContext::getAuthentication)
                .filter(OAuth2AuthenticationToken.class::isInstance)
                .cast(OAuth2AuthenticationToken.class)
                .flatMap(this::authorizeClient)
                .mapNotNull(OAuth2AuthorizedClient::getAccessToken)
                .switchIfEmpty(Mono.error(new AccessDeniedException("Authorization failed")))
                .flatMap(oAuth2AccessToken -> ok().contentType(APPLICATION_JSON).bodyValue(oAuth2AccessToken));
    }

    @NonNull
    public Mono<ServerResponse> getUiConfig(@SuppressWarnings("unused") ServerRequest serverRequest) {
        String configParams = "{}";
        try {
            configParams = objectMapper.writeValueAsString(uiSettings.getFrontendConfig());
        } catch (JsonProcessingException e) {
            LOGGER.error(EVENT_FAILURE, "Error generating config for frontend", e);
        }
        return Mono.just(String.format("const CONFIG=%s", configParams))
                .flatMap(response -> ok().contentType(MediaType.valueOf("application/javascript;charset=UTF-8"))
                        .bodyValue(response));
    }

    private Mono<OAuth2AuthorizedClient> authorizeClient(Authentication authentication) {
        OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest.withClientRegistrationId("url_shortener")
                .principal(authentication)
                .build();
        return authorizedClientManager.authorize(request)
                .switchIfEmpty(Mono.error(new AccessDeniedException("User authentication is required")))
                .retry(2L);
    }
}
