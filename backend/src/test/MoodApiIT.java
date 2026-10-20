package com.mindfocus;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MoodApiIT extends AbstractIntegrationTest {

    @Test
    @DisplayName("saving a mood for the same day twice replaces it instead of adding a second entry")
    void upsertPerDay() throws Exception {
        String token = newUserToken();
        putJson("/api/moods/2026-10-17", token, j("{'score':2,'note':'tired'}")).andExpect(status().isOk());
        putJson("/api/moods/2026-10-17", token, j("{'score':5,'note':'better'}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(5));
        getAs("/api/moods", token)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].note").value("better"));
    }

    @Test
    @DisplayName("moods are listed newest day first")
    void newestFirst() throws Exception {
        String token = newUserToken();
        putJson("/api/moods/2026-10-10", token, j("{'score':3}")).andExpect(status().isOk());
        putJson("/api/moods/2026-10-12", token, j("{'score':4}")).andExpect(status().isOk());
        putJson("/api/moods/2026-10-11", token, j("{'score':1}")).andExpect(status().isOk());
        getAs("/api/moods", token)
                .andExpect(jsonPath("$[0].entryDate").value("2026-10-12"))
                .andExpect(jsonPath("$[1].entryDate").value("2026-10-11"))
                .andExpect(jsonPath("$[2].entryDate").value("2026-10-10"));
    }

    @Test
    @DisplayName("the note is optional and the lowest and highest scores are accepted")
    void boundariesAreAccepted() throws Exception {
        String token = newUserToken();
        putJson("/api/moods/2026-10-17", token, j("{'score':1}")).andExpect(status().isOk());
        putJson("/api/moods/2026-10-18", token, j("{'score':5}")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("a mood can be deleted, and deleting a missing one does not fail")
    void deleteMood() throws Exception {
        String token = newUserToken();
        putJson("/api/moods/2026-10-17", token, j("{'score':3}")).andExpect(status().isOk());
        deleteAs("/api/moods/2026-10-17", token).andExpect(status().isOk());
        deleteAs("/api/moods/2026-10-17", token).andExpect(status().isOk());
        getAs("/api/moods", token).andExpect(jsonPath("$.length()").value(0));
    }
}
