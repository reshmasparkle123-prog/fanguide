package com.fanguide.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FanGuideControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpoint_returnsOk() throws Exception {
        mockMvc.perform(get("/api/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"))
            .andExpect(jsonPath("$.service").value("FanGuide"));
    }

    @Test
    void crowdEndpoint_returnsNineZones() throws Exception {
        mockMvc.perform(get("/api/crowd"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.['Gate 1']").exists())
            .andExpect(jsonPath("$.['Food Court - Level 1']").exists());
    }

    @Test
    void chatEndpoint_returnsAnswerForValidRequest() throws Exception {
        String requestBody = """
            {"question": "Where is the nearest restroom?", "language": "English"}
            """;

        mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.answer").exists());
    }

    @Test
    void chatEndpoint_defaultsToEnglishWhenLanguageMissing() throws Exception {
        String requestBody = """
            {"question": "Where can I eat?"}
            """;

        mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.answer").exists());
    }

    @Test
    void chatEndpoint_rejectsBlankQuestion() throws Exception {
        String requestBody = """
            {"question": "", "language": "English"}
            """;

        mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void chatEndpoint_rejectsOverlongQuestion() throws Exception {
        String longQuestion = "a".repeat(600);
        String requestBody = String.format("{\"question\": \"%s\", \"language\": \"English\"}", longQuestion);

        mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest());
    }

    @Test
    void chatEndpoint_coercesUnrecognizedLanguageToEnglish() throws Exception {
        String requestBody = """
            {"question": "Where is Gate 3?", "language": "Klingon"}
            """;

        mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.answer").exists());
    }
}
