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
import java.util.stream.Stream;

public class WithMockApiUserSecurityContextFactory implements WithSecurityContextFactory<WithMockApiUser> {

    @Override
    public SecurityContext createSecurityContext(WithMockApiUser annotation) {
        Jwt jwt = Jwt.withTokenValue("ey...")
                .subject(annotation.username())
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

        Stream<GrantedAuthority> scopes = Arrays.stream(annotation.jwtScopes())
                .map(s -> "SCOPE_" + s)
                .map(SimpleGrantedAuthority::new);
        Stream<GrantedAuthority> authorities = Arrays.stream(annotation.authorities()).map(SimpleGrantedAuthority::new);
        Stream<GrantedAuthority> allAuthorities = Stream.concat(scopes, authorities);
        Set<GrantedAuthority> authoritySet = allAuthorities.collect(Collectors.toSet());

        JwtAuthenticationToken jwtAuthenticationToken = new JwtAuthenticationToken(jwt, authoritySet);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(jwtAuthenticationToken);
        return context;
    }
}
