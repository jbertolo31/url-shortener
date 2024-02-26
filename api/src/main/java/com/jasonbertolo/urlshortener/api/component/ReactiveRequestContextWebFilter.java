package com.jasonbertolo.urlshortener.api.component;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import static com.jasonbertolo.urlshortener.api.util.ReactiveRequestContextHolder.REQUEST_CONTEXT_KEY;

@Component
public class ReactiveRequestContextWebFilter implements WebFilter {

    @NonNull
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        return chain.filter(exchange).contextWrite(ctx -> ctx.put(REQUEST_CONTEXT_KEY, request));
    }
}
