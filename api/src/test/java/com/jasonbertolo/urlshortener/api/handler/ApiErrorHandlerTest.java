package com.jasonbertolo.urlshortener.api.handler;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.jasonbertolo.urlshortener.api.component.SpringContext;
import com.jasonbertolo.urlshortener.api.config.settings.UrlShortenerSettings;
import com.jasonbertolo.urlshortener.api.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.mock.web.reactive.function.server.MockServerRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.introspection.OAuth2IntrospectionException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.reactive.function.server.EntityResponse;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebInputException;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.*;

@ExtendWith(MockitoExtension.class)
class ApiErrorHandlerTest {

    private static final MockServerRequest MOCK_SERVER_REQUEST = MockServerRequest.builder().build();
    private static final String MOCK_DOCS_URL = "http://localhost:8090/api/v1/docs/swagger-ui/index.html";

    @Mock
    ErrorAttributes errorAttributes;

    @Mock
    ApplicationContext applicationContext;

    @Mock
    ServerCodecConfigurer serverCodecConfigurer;

    UrlShortenerSettings urlShortenerSettings;

    // System under test
    ApiErrorHandler apiErrorHandler;

    @BeforeEach
    void beforeEach() throws IOException {
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.DEBUG);

        urlShortenerSettings = new UrlShortenerSettings();
        urlShortenerSettings.setDocumentationBaseUrl(MOCK_DOCS_URL);

        // Mock ClassLoader for Spring's application context
        ClassLoader mockClassLoader = mock(ClassLoader.class);
        when(applicationContext.getClassLoader()).thenReturn(mockClassLoader);
        when(mockClassLoader.getResources(anyString())).thenReturn(new Enumeration<>() {
            public boolean hasMoreElements() {return false;}
            public URL nextElement() {return null;}
        });
        apiErrorHandler = new ApiErrorHandler(errorAttributes, new WebProperties(), applicationContext,
                serverCodecConfigurer);
    }

    @Test
    @DisplayName("Routing function not null")
    void getRoutingFunction() {
        RouterFunction<ServerResponse> routerFunction = apiErrorHandler.getRoutingFunction(errorAttributes);
        assertThat(routerFunction).isNotNull();
    }

    static Stream<Arguments> exceptionProblemDetails() {
        String s = "Mock exception";
        return Stream.of(
                Arguments.of(new ServerWebInputException(s), BAD_REQUEST),
                Arguments.of(new IllegalStateException(s), BAD_REQUEST),
                Arguments.of(new IllegalArgumentException(s), BAD_REQUEST),
                Arguments.of(new OAuth2IntrospectionException(s), UNAUTHORIZED),
                Arguments.of(new JwtException(s), UNAUTHORIZED),
                Arguments.of(new AccessDeniedException(s), FORBIDDEN),
                Arguments.of(new NoSuchElementException(s), NOT_FOUND),
                Arguments.of(new ResourceAccessException(s), NOT_FOUND),
                Arguments.of(new ResourceNotFoundException(s), NOT_FOUND),
                Arguments.of(new Exception(s), INTERNAL_SERVER_ERROR)
        );
    }
    @ParameterizedTest(name="problem={0}")
    @MethodSource("exceptionProblemDetails")
    @DisplayName("Create User ShortUrl - bad request, invalid url")
    void renderProblemDetailsIllegalStateException(Throwable t, HttpStatus expected) {
        when(errorAttributes.getError(MOCK_SERVER_REQUEST)).thenReturn(t);
        try (MockedStatic<SpringContext> mockSpringContext = mockStatic(SpringContext.class)) {
            mockSpringContext.when(SpringContext.getBean(any())).thenReturn(urlShortenerSettings);

            StepVerifier.create(apiErrorHandler.renderErrorResponse(MOCK_SERVER_REQUEST))
                    .consumeNextWith(serverResponse -> {
                        String exceptionName = t.getClass().getSimpleName().replace("Exception", "");
                        assertThat(serverResponse.statusCode()).isEqualTo(expected);
                        ProblemDetail problemDetail = extractProblemDetail(serverResponse);
                        assertThat(problemDetail.getType().toString()).hasToString(
                                MOCK_DOCS_URL + "?problem=" + exceptionName);
                        if (t instanceof ResourceNotFoundException) {
                            assertThat(problemDetail.getTitle()).isEqualTo("Resource not found");
                        } else {
                            assertThat(problemDetail.getTitle()).isEqualTo(exceptionName);
                        }
                        assertThat(problemDetail.getStatus()).isEqualTo(expected.value());
                        assertThat(problemDetail.getDetail()).contains("Mock exception");
                    })
                    .verifyComplete();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ProblemDetail extractProblemDetail(ServerResponse serverResponse) {
        assertThat(serverResponse).isInstanceOf(EntityResponse.class);
        EntityResponse<ProblemDetail> entityResponse = (EntityResponse) serverResponse;
        return entityResponse.entity();
    }
}
