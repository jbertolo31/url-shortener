package com.jasonbertolo.urlshortener.web.util;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WithMockWebUserSecurityContextFactory implements WithSecurityContextFactory<WithMockWebUser> {

    @Override
    public SecurityContext createSecurityContext(WithMockWebUser annotation) {
        Stream<GrantedAuthority> scopes = Arrays.stream(annotation.jwtScopes())
                .map(s -> "SCOPE_" + s)
                .map(SimpleGrantedAuthority::new);
        Stream<GrantedAuthority> authorities = Arrays.stream(annotation.authorities()).map(SimpleGrantedAuthority::new);
        Stream<GrantedAuthority> allAuthorities = Stream.concat(scopes, authorities);
        Set<GrantedAuthority> authoritySet = allAuthorities.collect(Collectors.toSet());

        OidcIdToken oidcIdToken = OidcIdToken.withTokenValue("ey...")
                .subject(annotation.username())
                .audience(List.of("url_shortener"))
                .authorizedParty("url_shortener")
                .authTime(Instant.now())
                .issuer("http://localhost:8085")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .nonce("abc123")
                .claim("jti", UUID.randomUUID().toString())
                .claim("sid", "def456")
                .build();

        DefaultOidcUser defaultOidcUser = new DefaultOidcUser(authoritySet, oidcIdToken);

        OAuth2AuthenticationToken oAuth2AuthenticationToken = new OAuth2AuthenticationToken(defaultOidcUser,
                authoritySet, "url-shortener");

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(oAuth2AuthenticationToken);
        return context;
    }
}
