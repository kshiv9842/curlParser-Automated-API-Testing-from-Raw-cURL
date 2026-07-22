package com.apiautomation.security;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory sliding-window rate limiter (per key).
 * Fine for single-instance Render; not shared across multiple instances.
 */
@Component
public class RateLimiter {

    private final Map<String, Deque<Long>> windows = new ConcurrentHashMap<>();

    public synchronized boolean tryAcquire(String key, int limitPerMinute) {
        long now = System.currentTimeMillis();
        long windowStart = now - 60_000L;
        Deque<Long> q = windows.computeIfAbsent(key, k -> new ArrayDeque<>());
        Iterator<Long> it = q.iterator();
        while (it.hasNext()) {
            if (it.next() < windowStart) {
                it.remove();
            } else {
                break;
            }
        }
        if (q.size() >= limitPerMinute) {
            return false;
        }
        q.addLast(now);
        return true;
    }

    public long retryAfterSeconds(String key) {
        Deque<Long> q = windows.get(key);
        if (q == null || q.isEmpty()) {
            return 1;
        }
        long oldest = q.peekFirst();
        long waitMs = Math.max(1, (oldest + 60_000L) - System.currentTimeMillis());
        return Math.max(1, (waitMs + 999) / 1000);
    }
}
