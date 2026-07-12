package com.fanguide.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class GroqService {

    @Value("${GROQ_API_KEY:}")
    private String apiKey;

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.3-70b-versatile";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();
    private final CrowdService crowdService;

    public GroqService(CrowdService crowdService) {
        this.crowdService = crowdService;
    }

    /**
     * Answers a fan's question in their chosen language, grounded in live
     * (mocked) stadium crowd data so responses can recommend less-congested
     * gates/routes in real time.
     */
    public String answerFanQuery(String question, String language) {
        if (apiKey == null || apiKey.isBlank()) {
            return fallbackAnswer(question, language);
        }

        try {
            String crowdContext = crowdService.getCrowdSummaryForPrompt();

            String systemPrompt = """
                You are FanGuide, a helpful multilingual stadium assistant for FIFA World Cup 2026.
                Always reply ONLY in this language: %s
                Keep answers short (2-4 sentences), friendly, and practical for someone walking
                around a stadium right now. Use the live crowd data below to recommend the
                least congested gates, restrooms, or routes when relevant.

                Live stadium crowd data:
                %s
                """.formatted(language, crowdContext);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = Map.of(
                "model", MODEL,
                "messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", question)
                ),
                "temperature", 0.4,
                "max_tokens", 300
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(GROQ_URL, entity, String.class);

            JsonNode root = mapper.readTree(response.getBody());
            return root.path("choices").get(0).path("message").path("content").asText();

        } catch (Exception e) {
            return fallbackAnswer(question, language);
        }
    }

    /** Simple keyword-based fallback so the demo never breaks if Groq is unreachable. */
    private String fallbackAnswer(String question, String language) {
        String q = question.toLowerCase();
        String base;
        if (q.contains("gate") || q.contains("entry")) {
            base = "Head to the gate shown as GREEN on the crowd map for the shortest wait.";
        } else if (q.contains("restroom") || q.contains("toilet") || q.contains("washroom")) {
            base = "The nearest restroom is on Level 1, near Section 108.";
        } else if (q.contains("food") || q.contains("water") || q.contains("eat")) {
            base = "Food and water counters are available on Level 2, opposite Gate 4.";
        } else if (q.contains("exit")) {
            base = "Use Gate 5 for exit — it currently has the lowest crowd density.";
        } else {
            base = "I can help with directions, restrooms, food, or exits — could you tell me a bit more?";
        }
        return "[" + language + "] " + base;
    }
}
