package com.jasonbertolo.urlshortener.api.handler;

import com.google.common.annotations.VisibleForTesting;
import com.jasonbertolo.urlshortener.api.component.SpringContext;
import com.jasonbertolo.urlshortener.api.config.settings.UrlShortenerSettings;
import com.jasonbertolo.urlshortener.api.exception.ApiException;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Logger;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.introspection.OAuth2IntrospectionException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.reactive.function.server.*;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.owasp.esapi.Logger.EVENT_FAILURE;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.server.ServerResponse.status;

@Component
@Order(-2)
public class ApiErrorHandler extends AbstractErrorWebExceptionHandler {

    private static final Logger LOGGER = ESAPI.getLogger(ApiErrorHandler.class.getSimpleName());
    private static final String LOG_FORMAT = "[%s] [%s] %s";

    public ApiErrorHandler(ErrorAttributes errorAttributes, WebProperties webProperties,
                           ApplicationContext applicationContext, ServerCodecConfigurer serverCodecConfigurer) {
        super(errorAttributes, webProperties.getResources(), applicationContext);
        super.setMessageWriters(serverCodecConfigurer.getWriters());
        super.setMessageReaders(serverCodecConfigurer.getReaders());
    }

    @Override
    public RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    @VisibleForTesting
    Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
        final Throwable t = getError(request);
        if (t instanceof ApiException apiException) {
            // Handle ApiExceptions
            LOGGER.error(EVENT_FAILURE, String.format(LOG_FORMAT, extractTraceId(apiException.getProblemDetail()),
                    t.getClass().getSimpleName(), t.getMessage()));
            return status(apiException.getStatus()).contentType(APPLICATION_JSON)
                    .bodyValue(apiException.getProblemDetail());
        } else {
            // Handle Spring exceptions
            if (t instanceof ServerWebInputException
                    || t instanceof IllegalStateException
                    || t instanceof IllegalArgumentException) {
                return logAndReturnProblemDeail(t, BAD_REQUEST);
            } else if (t instanceof OAuth2IntrospectionException
                    || t instanceof JwtException) {
                return logAndReturnProblemDeail(t, UNAUTHORIZED);
            } else if (t instanceof AccessDeniedException) {
                return logAndReturnProblemDeail(t, FORBIDDEN);
            } else if (t instanceof NoSuchElementException
                    || t instanceof ResourceAccessException) {
                return logAndReturnProblemDeail(t, NOT_FOUND);
            } else {
                return logAndReturnProblemDeail(t, INTERNAL_SERVER_ERROR);
            }
        }
    }

    private Mono<ServerResponse> logAndReturnProblemDeail(Throwable t, HttpStatus status) {
        String className = t.getClass().getSimpleName();
        String message = t.getMessage();
        ProblemDetail problemDetail = toProblemDetail(t, status);
        LOGGER.error(EVENT_FAILURE, String.format(LOG_FORMAT,
                extractTraceId(problemDetail), className, message));
        return status(status).contentType(APPLICATION_JSON).bodyValue(problemDetail);
    }

    public static ProblemDetail toProblemDetail(Throwable t, HttpStatus status) {
        String exceptionName = t.getClass().getSimpleName().replace("Exception", "");
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, t.getMessage());
        problemDetail.setType(URI.create(SpringContext.getBean(UrlShortenerSettings.class).getDocumentationBaseUrl() +
                "?problem=" + exceptionName));
        problemDetail.setTitle(exceptionName);
        problemDetail.setInstance(URI.create("/api/v1"));
        problemDetail.setProperty("timestamp", Instant.now().toEpochMilli());
        problemDetail.setProperty("traceId", UUID.randomUUID().toString());
        return problemDetail;
    }

    public static String extractTraceId(ProblemDetail problemDetail) {
        return Optional.ofNullable(problemDetail.getProperties()).orElse(Map.of()).getOrDefault("traceId", null).toString();
    }
}
