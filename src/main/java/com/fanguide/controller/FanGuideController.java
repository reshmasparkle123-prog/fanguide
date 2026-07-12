package com.fanguide.controller;

import com.fanguide.model.ChatRequest;
import com.fanguide.service.CrowdService;
import com.fanguide.service.GroqService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@CrossOrigin
public class FanGuideController {

    private final GroqService groqService;
    private final CrowdService crowdService;

    public FanGuideController(GroqService groqService, CrowdService crowdService) {
        this.groqService = groqService;
        this.crowdService = crowdService;
    }

    /** Multilingual Q&A endpoint — the core AI feature. */
    @PostMapping("/api/chat")
    public Map<String, String> chat(@RequestBody ChatRequest request) {
        String lang = (request.getLanguage() == null || request.getLanguage().isBlank())
            ? "English" : request.getLanguage();
        String answer = groqService.answerFanQuery(request.getQuestion(), lang);
        return Map.of("answer", answer);
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
