package com.jasonbertolo.authserver;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AuthServerApplicationTest {

    @Autowired
    private AuthorizationServerSettings authorizationServerSettings;

    @Test
    void contextLoads() {
        assertThat(authorizationServerSettings).isNotNull();
    }
}
