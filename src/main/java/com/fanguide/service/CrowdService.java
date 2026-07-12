package com.fanguide.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Simulates a live stadium crowd-density feed. In a production system this
 * would be fed by turnstile counters / CCTV analytics; here it generates
 * realistic fluctuating values per zone so the AI layer has something live
 * to reason over during the demo.
 */
@Service
public class CrowdService {

    private final List<String> zones = List.of(
        "Gate 1", "Gate 2", "Gate 3", "Gate 4", "Gate 5",
        "Food Court - Level 1", "Food Court - Level 2",
        "Restroom - Sec 108", "Restroom - Sec 210"
    );

    private final Map<String, Integer> density = new LinkedHashMap<>();

    public CrowdService() {
        for (String zone : zones) {
            density.put(zone, ThreadLocalRandom.current().nextInt(20, 80));
        }
    }

    /** Nudges each zone's density up/down slightly to simulate live movement. */
    public Map<String, Integer> getLiveCrowdData() {
        density.replaceAll((zone, current) -> {
            int delta = ThreadLocalRandom.current().nextInt(-10, 11);
            int updated = Math.max(5, Math.min(98, current + delta));
            return updated;
        });
        return density;
    }

    public String getCrowdSummaryForPrompt() {
        Map<String, Integer> live = getLiveCrowdData();
        StringBuilder sb = new StringBuilder();
        live.forEach((zone, pct) -> {
            String status = pct < 40 ? "LOW" : pct < 70 ? "MODERATE" : "HIGH";
            sb.append(zone).append(": ").append(pct).append("% (").append(status).append(")\n");
        });
        return sb.toString();
    }
}
