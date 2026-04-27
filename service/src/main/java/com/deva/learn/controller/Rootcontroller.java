package com.deva.learn.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.deva.learn.exception.InventoryUnavailableException;
import com.deva.learn.exception.SessionRejectedException;
import com.deva.learn.exception.UnsupportedPlanException;

@RestController
@RequestMapping("/api")
public class Rootcontroller {
    private static final Map<String, Integer> PLAN_LIMITS = Map.of(
            "standard", 10,
            "premium", 50);
    private static final List<byte[]> RETAINED_BLOCKS = Collections.synchronizedList(new ArrayList<>());
    private static final Object RECONCILE_LOCK = new Object();

    private final AtomicInteger summaryRequests = new AtomicInteger();
    private final AtomicInteger inventoryRequests = new AtomicInteger();

    @PostMapping("/start-scan")
    ResponseEntity<String> startTask(){
        String scanId = "dummyID";
        return ResponseEntity.ok("Okayy.. creating " + scanId.toLowerCase());
    }

    @GetMapping("/lab/quote")
    ResponseEntity<Map<String, Object>> quote(
            @RequestParam(defaultValue = "starter") String plan,
            @RequestParam(defaultValue = "1") int items) {

        if (!PLAN_LIMITS.containsKey(plan)) {
            throw new UnsupportedPlanException(plan, PLAN_LIMITS.keySet());
        }
        int limit = PLAN_LIMITS.get(plan);
        int remaining = limit - items;

        return ResponseEntity.ok(Map.of(
                "plan", plan,
                "items", items,
                "remaining", remaining));
    }

    @GetMapping("/lab/summary")
    ResponseEntity<Map<String, Object>> summary() throws InterruptedException {
        int requestNumber = summaryRequests.incrementAndGet();
        // if (requestNumber % 3 == 0) {
        //     Thread.sleep(35_000);
        // } else {
        //     Thread.sleep(150);
        // }

        return ResponseEntity.ok(Map.of(
                "requestNumber", requestNumber,
                "status", "ready"));
    }

    @GetMapping("/lab/inventory")
    ResponseEntity<Map<String, Object>> inventory(@RequestParam(defaultValue = "sku-001") String sku) {
        int requestNumber = inventoryRequests.incrementAndGet();
        if (requestNumber % 4 == 0) {
            throw new InventoryUnavailableException(sku, requestNumber);
        }

        return ResponseEntity.ok(Map.of(
                "sku", sku,
                "available", true,
                "requestNumber", requestNumber));
    }

    @GetMapping("/lab/session")
    ResponseEntity<Map<String, Object>> session(@RequestParam(defaultValue = "expired") String token) {
        if (!"valid-token".equals(token)) {
            throw new SessionRejectedException();
        }

        return ResponseEntity.ok(Map.of(
                "status", "accepted",
                "issuedAt", Instant.now().toString()));
    }

    @GetMapping("/lab/cache")
    ResponseEntity<Map<String, Object>> cache(@RequestParam(defaultValue = "1") int mb) {
        int safeMegabytes = Math.max(1, Math.min(mb, 8));
        if (RETAINED_BLOCKS.size() < 96) {
            RETAINED_BLOCKS.add(new byte[safeMegabytes * 1024 * 1024]);
        }

        Runtime runtime = Runtime.getRuntime();
        long usedMegabytes = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);

        return ResponseEntity.ok(Map.of(
                "retainedBlocks", RETAINED_BLOCKS.size(),
                "usedMemoryMiB", usedMegabytes));
    }

    @GetMapping("/lab/reconcile")
    ResponseEntity<Map<String, Object>> reconcile() throws InterruptedException {
        synchronized (RECONCILE_LOCK) {
            Thread.sleep(3_000);
            return ResponseEntity.ok(Map.of(
                    "status", "complete",
                    "finishedAt", Instant.now().toString()));
        }
    }

    @GetMapping("/lab/profile")
    ResponseEntity<Map<String, Object>> profile() throws InterruptedException {
        int roll = ThreadLocalRandom.current().nextInt(10);
        if (roll < 2) {
            Thread.sleep(5_000);
        } else {
            Thread.sleep(80);
        }

        return ResponseEntity.ok(Map.of(
                "status", "loaded",
                "roll", roll));
    }

    @GetMapping(value = "/lab/export", produces = MediaType.TEXT_PLAIN_VALUE)
    String export(@RequestParam(defaultValue = "2048") int kb) {
        int safeKilobytes = Math.max(1, Math.min(kb, 9_000));
        return "x".repeat(safeKilobytes * 1024);
    }

    @GetMapping("/lab/aggregate")
    ResponseEntity<Map<String, Object>> aggregate(@RequestParam(defaultValue = "2500") int durationMs) {
        int safeDurationMs = Math.max(100, Math.min(durationMs, 8_000));
        long deadline = System.nanoTime() + safeDurationMs * 1_000_000L;
        long result = 0;

        while (System.nanoTime() < deadline) {
            result += ThreadLocalRandom.current().nextInt(1, 100);
        }

        return ResponseEntity.ok(Map.of(
                "durationMs", safeDurationMs,
                "result", result));
    }

    @GetMapping("/lab/feature")
    ResponseEntity<Map<String, Object>> feature() throws IOException {
        String featureConfig = Files.readString(Path.of("/etc/prod-core/feature-flag.json"));
        return ResponseEntity.ok(Map.of(
                "enabled", true,
                "configSize", featureConfig.length()));
    }
}
