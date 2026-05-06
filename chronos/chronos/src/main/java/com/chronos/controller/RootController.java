package com.chronos.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@RestController
public class RootController {

    @GetMapping("/")
    public Map<String, Object> welcome() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "Welcome to Chronos API - Production");
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        return response;
    }
}
