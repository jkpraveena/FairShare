package com.fairshare.backend;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RateLimitService {

    private static final int MAX_REQUESTS = 5;
    private static final long WINDOW_SECONDS = 60;

    private static final String KEY_PREFIX = "fairshare:ratelimit:";

    private final StringRedisTemplate redisTemplate;

    private static final DefaultRedisScript<Long> RATE_LIMIT_SCRIPT;

    static {
        RATE_LIMIT_SCRIPT = new DefaultRedisScript<>();

        RATE_LIMIT_SCRIPT.setScriptText(
                "local count = redis.call('INCR', KEYS[1]) " +
                "if count == 1 then " +
                "    redis.call('EXPIRE', KEYS[1], ARGV[1]) " +
                "end " +
                "return count"
        );

        RATE_LIMIT_SCRIPT.setResultType(Long.class);
    }

    public RateLimitService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean allowRequest(String userId) {

        String key = KEY_PREFIX + userId;

        Long count = redisTemplate.execute(
                RATE_LIMIT_SCRIPT,
                List.of(key),
                String.valueOf(WINDOW_SECONDS)
        );

        return count != null && count <= MAX_REQUESTS;
    }
}