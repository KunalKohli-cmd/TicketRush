package com.TicketMaster.user_service.service;

import com.TicketMaster.user_service.config.JwtProperties;
import com.TicketMaster.user_service.entity.Role;
import com.TicketMaster.user_service.entity.User;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    // 32-byte Base64 key (same as application-local.yml)
    private static final String SECRET_B64 = "dGlja2V0cnVzaC1sb2NhbC1kZXYtc2VjcmV0LWtleS0zMmJ5dGVz";

    JwtService jwtService;

    @BeforeEach
    void setUp() {
        byte[] bytes = Base64.getDecoder().decode(SECRET_B64);
        SecretKey key = new SecretKeySpec(bytes, "HmacSHA256");

        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        JwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        JwtProperties props = new JwtProperties(SECRET_B64, "test-issuer", Duration.ofMinutes(15), Duration.ofDays(7));
        jwtService = new JwtService(encoder, decoder, props);
    }

    @Test
    void createAccessToken_happyPath_returnsNonNullToken() throws Exception {
        User user = buildUserWithId(42L, "alice@example.com", Role.USER);

        String token = jwtService.createAccessToken(user);

        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void verify_validToken_returnsJwtWithEmailClaim() throws Exception {
        User user = buildUserWithId(42L, "alice@example.com", Role.USER);

        String token = jwtService.createAccessToken(user);
        Jwt jwt = jwtService.verify(token);

        assertThat(jwt).isNotNull();
        assertThat(jwt.getClaimAsString("email")).isEqualTo("alice@example.com");
        assertThat(jwt.getSubject()).isEqualTo("42");
    }

    @Test
    void extractSubject_returnsSubjectFromJwt() throws Exception {
        User user = buildUserWithId(7L, "bob@example.com", Role.ADMIN);

        String token = jwtService.createAccessToken(user);
        Jwt jwt = jwtService.verify(token);

        assertThat(jwtService.extractSubject(jwt)).isEqualTo("7");
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private User buildUserWithId(Long id, String email, Role role) throws Exception {
        User user = new User();
        user.setEmail(email);
        user.setName("Test User");
        user.setRole(role);
        // Set the private id field via reflection since there is no setter for it
        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, id);
        return user;
    }
}
