package com.jasonbertolo.urlshortener.api.component;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class ResponseTimeWebFilter implements WebFilter {

    public static final String RESPONSE_HEADER = "X-Response-Time";

    @NonNull
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        long before = System.currentTimeMillis();
        exchange.getResponse().beforeCommit(() -> Mono.defer(() -> {
            exchange.getResponse().getHeaders().add(RESPONSE_HEADER, Long.toString(System.currentTimeMillis() - before));
            return Mono.empty();
        }));
        return chain.filter(exchange);
    }
}
