package com.fanguide.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class GroqService {

    private static final Logger log = LoggerFactory.getLogger(GroqService.class);

    @Value("${GROQ_API_KEY:}")
    private String apiKey;

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.3-70b-versatile";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(8);
    private static final double TEMPERATURE = 0.4;
    private static final int MAX_TOKENS = 300;

    private final WebClient webClient;
    private final ObjectMapper mapper = new ObjectMapper();
    private final CrowdService crowdService;

    public GroqService(CrowdService crowdService) {
        this.crowdService = crowdService;
        // Non-blocking client — under stadium-scale concurrent load this frees
        // up threads instead of holding them for the duration of each Groq call.
        this.webClient = WebClient.builder().baseUrl(GROQ_URL).build();
    }

    /**
     * Answers a fan's question in their chosen language, grounded in live
     * (mocked) stadium crowd data so responses can recommend less-congested
     * gates/routes in real time.
     */
    public String answerFanQuery(String question, String language) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("GROQ_API_KEY not set; using offline fallback responder.");
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

            Map<String, Object> body = Map.of(
                "model", MODEL,
                "messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", question)
                ),
                "temperature", TEMPERATURE,
                "max_tokens", MAX_TOKENS
            );

            String responseBody = webClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .headers(h -> h.setBearerAuth(apiKey))
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block(REQUEST_TIMEOUT);

            JsonNode root = mapper.readTree(responseBody);
            return root.path("choices").get(0).path("message").path("content").asText();

        } catch (WebClientException e) {
            log.warn("Groq API call failed, using offline fallback: {}", e.getMessage());
            return fallbackAnswer(question, language);
        } catch (Exception e) {
            log.error("Unexpected error answering fan query, using offline fallback", e);
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
