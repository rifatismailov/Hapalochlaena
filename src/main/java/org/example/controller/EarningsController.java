package org.example.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class EarningsController {

    @GetMapping("/api/earnings")
    public Map<String, Object> getEarnings() {
        Map<String, Object> response = new HashMap<>();
        response.put("labels", new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"});
        response.put("values", new int[]{1000, 2220, 300, 400, 550, 100, 6000, 800, 320, 100, 4200, 1200});
        return response;
    }
}
