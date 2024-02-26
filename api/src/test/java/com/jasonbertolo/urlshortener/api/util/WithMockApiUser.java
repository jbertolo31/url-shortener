package com.jasonbertolo.urlshortener.api.util;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.*;

@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@WithSecurityContext(factory = WithMockApiUserSecurityContextFactory.class)
public @interface WithMockApiUser {

    /**
     * The mock User's username. Default 'user'.
     */
    String username() default "user";

    /**
     * The mock User's JWT scopes. Default 'shorturl:read,shorturl:write,openid,profile'
     */
    String[] jwtScopes() default {
            "shorturl:read",
            "shorturl:write",
            "openid",
            "profile"
    };

    /**
     * The mock User's authorities. Default 'ROLE_USER'.
     */
    String[] authorities() default {
            //"ROLE_USER"
    };
}
