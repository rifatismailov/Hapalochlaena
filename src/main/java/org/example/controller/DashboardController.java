package org.example.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class DashboardController {

    @GetMapping("/api/dashboard")
    public Map<String, Integer> getDashboardStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("verified", 12);
        stats.put("templates", 8);
        stats.put("requests", 27);
        stats.put("errors", 3);
        return stats;
    }
}

