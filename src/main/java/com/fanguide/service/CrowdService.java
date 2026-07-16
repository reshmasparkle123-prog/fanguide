package com.fanguide.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

/**
* Simulates a live stadium crowd-density feed. Reads are lock-free: the
* scheduled updater is the only writer, so /api/crowd and /api/chat can be
* hit by thousands of concurrent fans without any per-request locking.
*/
@Service
public class CrowdService {

    private static final List<String> ZONES = List.of(
        "Gate 1", "Gate 2", "Gate 3", "Gate 4", "Gate 5",
        "Food Court - Level 1", "Food Court - Level 2",
        "Restroom - Sec 108", "Restroom - Sec 210"
    );

    private final AtomicReference<Map<String, Integer>> snapshot =
        new AtomicReference<>(initialSnapshot());

    private static Map<String, Integer> initialSnapshot() {
        Map<String, Integer> initial = new LinkedHashMap<>();
        for (String zone : ZONES) {
            initial.put(zone, ThreadLocalRandom.current().nextInt(20, 80));
        }
        return Collections.unmodifiableMap(initial);
    }

    /** Nudges values every 3s to simulate live movement, decoupled from reads. */
    @Scheduled(fixedRate = 3000)
    public void refreshCrowdData() {
        Map<String, Integer> current = snapshot.get();
        Map<String, Integer> next = new LinkedHashMap<>();
        current.forEach((zone, value) -> {
            int delta = ThreadLocalRandom.current().nextInt(-10, 11);
            next.put(zone, Math.max(5, Math.min(98, value + delta)));
        });
        snapshot.set(Collections.unmodifiableMap(next));
    }

    /** O(1), lock-free read — safe to call from any thread, any volume. */
    public Map<String, Integer> getLiveCrowdData() {
        return snapshot.get();
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
