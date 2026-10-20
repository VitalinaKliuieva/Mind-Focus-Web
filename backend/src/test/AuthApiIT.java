package com.mindfocus;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Registration, login and the "who may call what" rules. */
class AuthApiIT extends AbstractIntegrationTest {

    @Test
    @DisplayName("register returns 201, a token and the user, and never the password hash")
    void registerReturnsTokenAndUser() throws Exception {
        String email = uniqueEmail();
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email, "password1", "Ann")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.displayName").value("Ann"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @ParameterizedTest(name = "invalid input {0} / {1} / {2} -> 400")
    @CsvSource(delimiter = '|', value = {
            "not-an-email|password1|Ann",
            "a@b.pl|short|Ann",
            "a@b.pl|password1|''"
    })
    void invalidRegistrationIsRejected(String email, String password, String name) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email, password, name)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("the same email cannot register twice, even with different letter case")
    void duplicateEmailIsRejectedCaseInsensitively() throws Exception {
        String email = uniqueEmail();
        register(email);
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email.toUpperCase(), "password1", "Copy")))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("login works with the right password, and the email is case-insensitive")
    void loginSucceeds() throws Exception {
        String email = uniqueEmail();
        register(email);
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(j("{'email':'" + email.toUpperCase() + "','password':'password1'}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("wrong password and unknown email give the same 401 (no user enumeration)")
    void loginFailuresLookTheSame() throws Exception {
        String email = uniqueEmail();
        register(email);
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(j("{'email':'" + email + "','password':'wrong-password'}")))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(j("{'email':'" + uniqueEmail() + "','password':'password1'}")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("/me returns the logged-in user")
    void meReturnsCurrentUser() throws Exception {
        String email = uniqueEmail();
        String token = register(email);
        getAs("/api/auth/me", token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @ParameterizedTest(name = "{0} without a token -> 401")
    @ValueSource(strings = {"/api/notes", "/api/plans", "/api/todos", "/api/goals", "/api/habits", "/api/habits/stats",
            "/api/moods", "/api/calendar?from=2026-10-01&to=2026-10-31", "/api/analytics/summary",
            "/api/notifications", "/api/auth/me"})
    void protectedEndpointsNeedLogin(String url) throws Exception {
        mvc.perform(get(url)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("a garbage token and a token signed with a foreign key are both rejected")
    void badTokensAreRejected() throws Exception {
        getAs("/api/notes", "not-a-token").andExpect(status().isUnauthorized());

        SecretKey foreign = Keys.hmacShaKeyFor("a-completely-different-secret-key-0123456789".getBytes(StandardCharsets.UTF_8));
        String forged = Jwts.builder().subject("1").claim("email", "x@y.pl")
                .expiration(Date.from(Instant.now().plusSeconds(600))).signWith(foreign).compact();
        getAs("/api/notes", forged).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("the VAPID public key endpoint is public (the browser needs it before login state matters)")
    void publicKeyIsPublic() throws Exception {
        mvc.perform(get("/api/push/public-key")).andExpect(status().isOk());
    }
}
