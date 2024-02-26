package com.jasonbertolo.urlshortener.api.util;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.*;

@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@WithSecurityContext(factory = WithMockClientSecurityContextFactory.class)
public @interface WithMockClient {

    /**
     * The mock client's client id. Default 'client_id'.
     */
    String clientId() default "url_shortener";

    /**
     * The mock client's JWT scopes. Default 'cache:read,cache:write'
     */
    String[] jwtScopes() default {
            "cache:read",
            "cache:write"
    };
}
