package com.jasonbertolo.urlshortener.api.config;

import com.jasonbertolo.urlshortener.api.config.settings.MicroservicesSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.springframework.http.HttpMethod.*;
import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.security.oauth2.core.authorization.OAuth2ReactiveAuthorizationManagers.hasScope;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .authorizeExchange(spec -> spec
                        .pathMatchers(GET, "/api/v1/shorturl", "/api/v1/shorturl/{id}").access(hasScope("shorturl:read"))
                        .pathMatchers(POST, "/api/v1/shorturl").access(hasScope("shorturl:write"))
                        .pathMatchers(DELETE, "/api/v1/shorturl/{id}").access(hasScope("shorturl:write"))
                        .pathMatchers(GET, "/api/v1/cache/{key}").access(hasScope("cache:write"))
                        .pathMatchers(GET, "/actuator/**", "/api/v1/docs/**").permitAll()
                        .anyExchange().authenticated())

                // Stateless session
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())

                // Disable features not used
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .requestCache(ServerHttpSecurity.RequestCacheSpec::disable)

                // Default OAuth2 jwt setup
                .oauth2ResourceServer(spec -> spec.jwt(withDefaults()));
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfiguration(MicroservicesSettings microservicesSettings) {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.applyPermitDefaultValues();
        corsConfig.addAllowedMethod(HttpMethod.DELETE);
        corsConfig.setAllowedOrigins(List.of(microservicesSettings.getBffBaseUrl()));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        return source;
    }
}
