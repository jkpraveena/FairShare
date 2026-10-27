package com.fairshare.backend;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class RateLimitController {

    private final RateLimitService rateLimitService;

    public RateLimitController(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @GetMapping("/limited")
    public ResponseEntity<Map<String, String>> limitedEndpoint(
            @RequestHeader(value = "X-User-Id", defaultValue = "guest") String userId) {

        if (!rateLimitService.allowRequest(userId)) {
            return ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of(
                            "status", "ERROR",
                            "message", "Rate limit exceeded. Try again later."
                    ));
        }

        return ResponseEntity.ok(
                Map.of(
                        "status", "SUCCESS",
                        "message", "Request accepted for user " + userId
                )
        );
    }
}