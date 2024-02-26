package com.jasonbertolo.urlshortener.api.util;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class WithMockClientSecurityContextFactory implements WithSecurityContextFactory<WithMockClient> {

    @Override
    public SecurityContext createSecurityContext(WithMockClient annotation) {
        Jwt jwt = Jwt.withTokenValue("ey...")
                .subject(annotation.clientId())
                .claim("scope", Arrays.stream(annotation.jwtScopes()).toList())
                .audience(List.of("url_shortener"))
                .notBefore(Instant.now())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .issuer("http://localhost:8085")
                .jti(UUID.randomUUID().toString())
                .headers(map -> {
                    map.put("alg", "HS256");
                    map.put("type", "JWT");
                })
                .build();

        Set<GrantedAuthority> scopes = Arrays.stream(annotation.jwtScopes())
                .map(s -> "SCOPE_" + s)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());

        JwtAuthenticationToken jwtAuthenticationToken = new JwtAuthenticationToken(jwt, scopes);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(jwtAuthenticationToken);
        return context;
    }
}
