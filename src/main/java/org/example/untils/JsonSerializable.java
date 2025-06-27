package org.example.untils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;

public interface JsonSerializable {
    @JsonIgnore
    default String getJson() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (Exception e) {
            return "{\"error\":\"serialization failed\"}";
        }
    }

    static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return new ObjectMapper().readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("❌ Не вдалося десеріалізувати JSON: " + json, e);
        }
    }
}

