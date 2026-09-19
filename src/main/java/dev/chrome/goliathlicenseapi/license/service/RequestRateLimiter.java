package dev.chrome.goliathlicenseapi.license.service;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class RequestRateLimiter {

    private final Map<String, Deque<Long>> attempts = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final Duration window;

    public RequestRateLimiter() {
        this.maxAttempts = 60; // default per-minute
        this.window = Duration.ofMinutes(1);
    }

    public boolean isBlocked(String key) {
        Deque<Long> queue = attempts.computeIfAbsent(key, k -> new ArrayDeque<>());
        long now = System.currentTimeMillis();
        long threshold = now - window.toMillis();
        while (!queue.isEmpty() && queue.peekFirst() < threshold) {
            queue.pollFirst();
        }
        return queue.size() >= maxAttempts;
    }

    public void record(String key) {
        Deque<Long> queue = attempts.computeIfAbsent(key, k -> new ArrayDeque<>());
        queue.addLast(System.currentTimeMillis());
        while (queue.size() > maxAttempts) {
            queue.pollFirst();
        }
    }

    public void reset(String key) {
        attempts.remove(key);
    }

    @PreDestroy
    public void cleanup() {
        attempts.clear();
    }
}
