package com.fanguide.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GroqServiceTest {

    private final GroqService groqService = new GroqService(new CrowdService());

    @Test
    void answerFanQuery_neverReturnsNullOrBlank() {
        // With no GROQ_API_KEY set in the test environment, this exercises
        // the fallback path — the assistant must never go silent.
        String answer = groqService.answerFanQuery("Where is the nearest restroom?", "English");
        assertNotNull(answer);
        assertFalse(answer.isBlank());
    }

    @Test
    void fallback_restroomQuestion_mentionsRestroom() {
        String answer = groqService.answerFanQuery("Where is the toilet?", "English");
        assertTrue(answer.toLowerCase().contains("restroom"));
    }

    @Test
    void fallback_gateQuestion_mentionsGate() {
        String answer = groqService.answerFanQuery("Which gate should I use for entry?", "English");
        assertTrue(answer.toLowerCase().contains("gate"));
    }

    @Test
    void fallback_respectsRequestedLanguageTag() {
        String answer = groqService.answerFanQuery("Where can I eat?", "Hindi");
        assertTrue(answer.contains("Hindi"), "Fallback response should tag the requested language");
    }

    @Test
    void fallback_unknownQuestion_returnsHelpfulDefault() {
        String answer = groqService.answerFanQuery("asdkjaslkdj random text", "English");
        assertFalse(answer.isBlank());
    }
}
