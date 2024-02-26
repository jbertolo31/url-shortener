package com.jasonbertolo.urlshortener.api.springdoc;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

import static io.swagger.v3.oas.annotations.enums.SecuritySchemeType.HTTP;

@Configuration
@OpenAPIDefinition(info = @Info(title = "URL Shortener API Reference", version = "v1",
        description = "<p>URL Shortener is a simple application which accepts valid URLs and produces short and " +
                "concise URLs which can be redirected to.</p>" +
                "<p>Visit /jwt of the BFF application to get a token to use for Swagger's 'Authorize' and 'Try it Out' " +
                "buttons.</p>"),
        servers = {@Server(url = "/", description = "Base Server URL")})
@SecurityScheme(
        name = "Bearer Token Authentication",
        type = HTTP,
        scheme = "bearer",
        bearerFormat = "OAuth2 Token"
)
public class OpenApiConfig {
}
