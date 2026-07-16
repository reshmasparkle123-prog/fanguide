package com.fanguide.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Simulates a live stadium crowd-density feed. In a production system this
 * would be fed by turnstile counters / CCTV analytics; here it generates
 * realistic fluctuating values per zone so the AI layer has something live
 * to reason over during the demo.
 *
 * This is a singleton Spring bean shared across all concurrent requests —
 * at stadium scale, thousands of fans can hit /api/crowd and /api/chat in
 * the same second, so all mutable state access is synchronized and every
 * getter returns a defensive copy rather than the live internal map.
 */
@Service
public class CrowdService {

    private static final List<String> ZONES = List.of(
        "Gate 1", "Gate 2", "Gate 3", "Gate 4", "Gate 5",
        "Food Court - Level 1", "Food Court - Level 2",
        "Restroom - Sec 108", "Restroom - Sec 210"
    );

    private final Map<String, Integer> density = new LinkedHashMap<>();

    public CrowdService() {
        for (String zone : ZONES) {
            density.put(zone, ThreadLocalRandom.current().nextInt(20, 80));
        }
    }

    /** Nudges each zone's density up/down slightly to simulate live movement. */
    public synchronized Map<String, Integer> getLiveCrowdData() {
        density.replaceAll((zone, current) -> {
            int delta = ThreadLocalRandom.current().nextInt(-10, 11);
            return Math.max(5, Math.min(98, current + delta));
        });
        return new LinkedHashMap<>(density); // defensive copy — callers can't mutate shared state
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
