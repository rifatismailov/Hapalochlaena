package org.example.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
public class SourcesController {

    @GetMapping("/api/sources")
    public Map<String, Object> getSources() {
        Map<String, Object> response = new HashMap<>();

        // Labels for the pie chart
        response.put("labels", Arrays.asList("Documents", "Templates", "Requests"));

        // Values for each label (must match in order)
        double[] percentages = calculatePercentages(12, 8, 20);
        response.put("values", percentages);

        return response;
    }

    public static double[] calculatePercentages(int value1, int value2, int value3) {
        double sum = value1 + value2 + value3;
        double percent1 = (value1 / sum) * 100.0;
        double percent2 = (value2 / sum) * 100.0;
        double percent3 = (value3 / sum) * 100.0;
        return new double[]{percent1, percent2, percent3};
    }
}

