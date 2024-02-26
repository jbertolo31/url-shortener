package com.jasonbertolo.urlshortener.api.util;

import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

import java.net.URI;

public class ReactiveRequestContextHolder {

    public static final Class<ServerHttpRequest> REQUEST_CONTEXT_KEY = ServerHttpRequest.class;

    private ReactiveRequestContextHolder() throws IllegalAccessException {
        throw new IllegalAccessException("This is a utility class");
    }

    public static Mono<ServerHttpRequest> getRequest() {
        return Mono.deferContextual(ctx -> ctx.hasKey(REQUEST_CONTEXT_KEY) ?
                Mono.just(ctx.get(REQUEST_CONTEXT_KEY)) : Mono.empty());
    }

    public static Mono<URI> getRequestPath() {
        return getRequest()
                .map(serverHttpRequest -> URI.create(serverHttpRequest.getPath().toString()))
                .defaultIfEmpty(URI.create("/api/v1"));
    }
}
