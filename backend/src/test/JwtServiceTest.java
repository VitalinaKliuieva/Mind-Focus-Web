package com.mindfocus;

import com.mindfocus.config.AppProperties;
import com.mindfocus.security.AuthUser;
import com.mindfocus.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Security-focused unit tests for token handling. */
class JwtServiceTest {
    private static final String SECRET = "unit-test-secret-unit-test-secret-1234567890";

    private static JwtService service(String secret, long minutes) {
        return new JwtService(new AppProperties("Europe/Warsaw", "http://localhost:4200", "no-reply@test",
                new AppProperties.Jwt(secret, minutes), new AppProperties.Push("", "", "")));
    }

    @Test
    @DisplayName("a token round-trips to the same user id and email")
    void roundTrip() {
        JwtService jwt = service(SECRET, 60);
        Optional<AuthUser> user = jwt.parse(jwt.issue(42L, "ann@example.com"));
        assertThat(user).contains(new AuthUser(42L, "ann@example.com"));
    }

    @Test
    @DisplayName("a token with a modified payload is rejected")
    void tamperedPayloadIsRejected() {
        JwtService jwt = service(SECRET, 60);
        String[] parts = jwt.issue(1L, "ann@example.com").split("\\.");
        char first = parts[1].charAt(0);
        parts[1] = (first == 'e' ? 'f' : 'e') + parts[1].substring(1);
        assertThat(jwt.parse(String.join(".", parts))).isEmpty();
    }

    @Test
    @DisplayName("an expired token is rejected")
    void expiredTokenIsRejected() {
        JwtService jwt = service(SECRET, -5);
        assertThat(jwt.parse(jwt.issue(1L, "ann@example.com"))).isEmpty();
    }

    @Test
    @DisplayName("a token signed with another secret is rejected")
    void foreignSignatureIsRejected() {
        String token = service(SECRET, 60).issue(1L, "ann@example.com");
        assertThat(service("another-secret-another-secret-0987654321", 60).parse(token)).isEmpty();
    }

    @ParameterizedTest(name = "garbage \"{0}\" is rejected")
    @ValueSource(strings = {"", " ", "abc", "a.b.c", "Bearer x", "eyJhbGciOiJub25lIn0.e30."})
    void garbageIsRejected(String token) {
        assertThat(service(SECRET, 60).parse(token)).isEmpty();
    }

    @Test
    @DisplayName("the app refuses to start with a weak (too short) secret")
    void weakSecretFailsFast() {
        assertThrows(RuntimeException.class, () -> service("too-short", 60));
    }
}
