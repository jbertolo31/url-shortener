package com.jasonbertolo.urlshortener.web.handler;

import com.google.common.annotations.VisibleForTesting;
import com.jasonbertolo.urlshortener.web.service.ApiService;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Logger;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.*;
import org.thymeleaf.spring6.view.reactive.ThymeleafReactiveViewResolver;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.owasp.esapi.Logger.EVENT_FAILURE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_HTML;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;
import static org.springframework.web.reactive.function.server.ServerResponse.status;

@Component
@Order(-2)
public class GlobalErrorHandler extends AbstractErrorWebExceptionHandler {

    private static final Logger LOGGER = ESAPI.getLogger(GlobalErrorHandler.class.getSimpleName());

    public GlobalErrorHandler(ErrorAttributes errorAttributes, WebProperties webProperties,
                              ApplicationContext applicationContext, ServerCodecConfigurer serverCodecConfigurer,
                              ThymeleafReactiveViewResolver thymeleafReactiveViewResolver) {
        super(errorAttributes, webProperties.getResources(), applicationContext);
        super.setMessageWriters(serverCodecConfigurer.getWriters());
        super.setMessageReaders(serverCodecConfigurer.getReaders());
        super.setViewResolvers(List.of(thymeleafReactiveViewResolver));
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    @VisibleForTesting
    public Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
        Throwable t = getError(request);
        LOGGER.error(EVENT_FAILURE, String.format(
                "Error: [%s] %s", t.getClass().getSimpleName(), t.getMessage()));

        if (t instanceof ApiService.ApiException apiException) {
            // For endpoints that return JSON response, return JSON errors too
            return status(apiException.problemDetail.getStatus())
                    .contentType(APPLICATION_JSON).bodyValue(apiException.problemDetail);
        } else {
            // Send exception type to FE, it will switch display errors accordingly
            return ok().contentType(TEXT_HTML).render("error", Map.of("ex", t.getClass().getSimpleName()));
        }
    }
}
