package dev.chrome.goliathlicenseapi.license.service;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class LoginRateLimiter {

    private final Map<String, Deque<Long>> attempts = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final Duration window;

    public LoginRateLimiter() {
        this.maxAttempts = 5;
        this.window = Duration.ofMinutes(10);
    }

    public boolean isBlocked(String clientKey) {
        Deque<Long> queue = attempts.computeIfAbsent(clientKey, ignored -> new ArrayDeque<>());
        long now = System.currentTimeMillis();
        long threshold = now - window.toMillis();
        while (!queue.isEmpty() && queue.peekFirst() < threshold) {
            queue.pollFirst();
        }
        return queue.size() >= maxAttempts;
    }

    public void recordFailure(String clientKey) {
        Deque<Long> queue = attempts.computeIfAbsent(clientKey, ignored -> new ArrayDeque<>());
        queue.addLast(System.currentTimeMillis());
        while (queue.size() > maxAttempts) {
            queue.pollFirst();
        }
    }

    public void recordSuccess(String clientKey) {
        attempts.remove(clientKey);
    }

    @PreDestroy
    public void cleanup() {
        attempts.clear();
    }
}
