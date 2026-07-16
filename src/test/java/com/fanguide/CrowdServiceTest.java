package com.fanguide.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CrowdServiceTest {

    private final CrowdService crowdService = new CrowdService();

    @Test
    void liveCrowdData_returnsAllNineZones() {
        Map<String, Integer> data = crowdService.getLiveCrowdData();
        assertEquals(9, data.size(), "Expected exactly 9 stadium zones");
        assertTrue(data.containsKey("Gate 1"));
        assertTrue(data.containsKey("Food Court - Level 1"));
        assertTrue(data.containsKey("Restroom - Sec 108"));
    }

    @RepeatedTest(20)
    void liveCrowdData_valuesAlwaysWithinValidRange() {
        Map<String, Integer> data = crowdService.getLiveCrowdData();
        data.forEach((zone, density) -> {
            assertTrue(density >= 5 && density <= 98,
                zone + " density " + density + " out of expected 5-98 bound");
        });
    }

    @Test
    void crowdSummaryForPrompt_containsStatusLabels() {
        String summary = crowdService.getCrowdSummaryForPrompt();
        assertNotNull(summary);
        assertFalse(summary.isBlank());
        assertTrue(
            summary.contains("LOW") || summary.contains("MODERATE") || summary.contains("HIGH"),
            "Summary should classify at least one zone with a status label"
        );
    }

    @Test
    void crowdSummaryForPrompt_includesAllZoneNames() {
        String summary = crowdService.getCrowdSummaryForPrompt();
        assertTrue(summary.contains("Gate 1"));
        assertTrue(summary.contains("Restroom - Sec 210"));
    }

    @Test
    void liveCrowdData_survivesConcurrentAccess() throws InterruptedException {
        // Simulates many fans hitting /api/crowd at the same instant during
        // a match — the shared singleton map must not corrupt or throw.
        int threadCount = 50;
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                Map<String, Integer> data = crowdService.getLiveCrowdData();
                assertEquals(9, data.size());
            });
            threads[i].start();
        }
        for (Thread t : threads) {
            t.join();
        }
    }

    @Test
    void liveCrowdData_returnsDefensiveCopyNotSharedReference() {
        Map<String, Integer> first = crowdService.getLiveCrowdData();
        Map<String, Integer> second = crowdService.getLiveCrowdData();
        assertNotSame(first, second, "Each call should return an independent copy");
    }
}
