package com.fanguide.controller;

import com.fanguide.model.ChatRequest;
import com.fanguide.service.CrowdService;
import com.fanguide.service.GroqService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@CrossOrigin
public class FanGuideController {

    private static final int MAX_QUESTION_LENGTH = 500;
    private static final Set<String> ALLOWED_LANGUAGES = Set.of(
        "English", "Hindi", "Spanish", "Portuguese", "French", "Arabic", "Mandarin", "Kannada"
    );

    private final GroqService groqService;
    private final CrowdService crowdService;

    public FanGuideController(GroqService groqService, CrowdService crowdService) {
        this.groqService = groqService;
        this.crowdService = crowdService;
    }

    /** Multilingual Q&A endpoint — the core AI feature. */
    @PostMapping("/api/chat")
    public ResponseEntity<Map<String, String>> chat(@RequestBody ChatRequest request) {
        String question = request.getQuestion();

        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Question must not be empty."));
        }
        if (question.length() > MAX_QUESTION_LENGTH) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Question exceeds maximum length of " + MAX_QUESTION_LENGTH + " characters."));
        }

        String lang = (request.getLanguage() == null || request.getLanguage().isBlank())
            ? "English" : request.getLanguage();
        if (!ALLOWED_LANGUAGES.contains(lang)) {
            lang = "English"; // reject unrecognized input rather than pass it unchecked into a prompt
        }

        String answer = groqService.answerFanQuery(question.trim(), lang);
        return ResponseEntity.ok(Map.of("answer", answer));
    }

    /** Live crowd density feed for the map/dashboard view. */
    @GetMapping("/api/crowd")
    public Map<String, Integer> crowd() {
        return crowdService.getLiveCrowdData();
    }

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "FanGuide");
    }
}
