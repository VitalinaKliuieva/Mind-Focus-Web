package com.mindfocus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base class for integration tests: the real Spring context, the real security chain,
 * and a real PostgreSQL started by Testcontainers (needs Docker). Mail is mocked.
 * Every test creates its own users, so tests do not depend on each other or on execution order.
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    // one container for the whole test run (singleton pattern), stopped by Testcontainers' Ryuk
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // the background reminder job must not run by itself during tests; tests call it explicitly
        registry.add("app.reminder-interval-ms", () -> "3600000");
        registry.add("app.reminder-initial-delay-ms", () -> "3600000");
    }

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    @MockBean
    protected JavaMailSender mailSender;

    // ---------- helpers ----------

    /** Lets test JSON use single quotes: j("{'a':1}") becomes {"a":1}. */
    protected static String j(String singleQuoted) {
        return singleQuoted.replace('\'', '"');
    }

    protected static String uniqueEmail() {
        return "qa-" + UUID.randomUUID() + "@example.com";
    }

    protected static String registerBody(String email, String password, String name) {
        return j("{'email':'" + email + "','password':'" + password + "','displayName':'" + name + "'}");
    }

    protected String register(String email) throws Exception {
        String response = mvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email, "password1", "QA Tester")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("token").asText();
    }

    /** Registers a brand-new user and returns the JWT. */
    protected String newUserToken() throws Exception {
        return register(uniqueEmail());
    }

    protected ResultActions postJson(String url, String token, String body) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post(url).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    protected ResultActions putJson(String url, String token, String body) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.put(url).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    protected ResultActions getAs(String url, String token) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.get(url).header("Authorization", "Bearer " + token));
    }

    protected ResultActions deleteAs(String url, String token) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.delete(url).header("Authorization", "Bearer " + token));
    }

    protected JsonNode read(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }

    protected long idOf(ResultActions result) throws Exception {
        return read(result).get("id").asLong();
    }

    /** POST and expect 201, return the new id. */
    protected long create(String path, String token, String body) throws Exception {
        return idOf(postJson(path, token, body).andExpect(status().isCreated()));
    }
}
