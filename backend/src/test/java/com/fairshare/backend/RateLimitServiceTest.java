package com.fairshare.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RateLimitServiceTest {

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private String userId;

    @BeforeEach
    void setUp() {
        userId = "test-" + UUID.randomUUID();
        redisTemplate.delete("fairshare:ratelimit:" + userId);
    }

    @Test
    void allowsFiveRequestsAndRejectsTheSixth() {

        for (int i = 1; i <= 5; i++) {
            assertTrue(
                    rateLimitService.allowRequest(userId),
                    "Request " + i + " should be allowed"
            );
        }

        assertFalse(
                rateLimitService.allowRequest(userId),
                "Request 6 should be rejected"
        );
    }

    @Test
    void maintainsIndependentLimitsForDifferentUsers() {

        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimitService.allowRequest(userId));
        }

        assertFalse(rateLimitService.allowRequest(userId));

        assertTrue(
                rateLimitService.allowRequest(userId + "-other"),
                "A different user should have an independent limit"
        );
    }
    @Test
void allowsOnlyFiveRequestsUnderConcurrentLoad() throws Exception {
    String concurrentUser = "concurrent-" + UUID.randomUUID();

    int totalRequests = 20;
    AtomicInteger allowed = new AtomicInteger();

    ExecutorService executor = Executors.newFixedThreadPool(20);

    try {
        var tasks = java.util.stream.IntStream.range(0, totalRequests)
                .<java.util.concurrent.Callable<Boolean>>mapToObj(
                        i -> () -> rateLimitService.allowRequest(concurrentUser)
                )
                .toList();

        var futures = executor.invokeAll(tasks);

        for (Future<Boolean> future : futures) {
            if (future.get()) {
                allowed.incrementAndGet();
            }
        }

        assertEquals(
                5,
                allowed.get(),
                "Exactly five concurrent requests should be allowed"
        );
    } finally {
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        redisTemplate.delete("fairshare:ratelimit:" + concurrentUser);
    }
}

    @Test
    void incrementsRedisCounter() {

        assertTrue(rateLimitService.allowRequest(userId));

        String key = "fairshare:ratelimit:" + userId;

        assertEquals(
                "1",
                redisTemplate.opsForValue().get(key)
        );
    }
}