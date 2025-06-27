package org.example.controller;

import org.example.redis.RedisService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/redis")
public class RedisController {

    private final RedisService redisService;

    public RedisController(RedisService redisService) {
        this.redisService = redisService;
    }

    @PostMapping("/save")
    public ResponseEntity<String> save(@RequestParam String key, @RequestParam String value) {
        redisService.saveData(key, value);
        return ResponseEntity.ok("Saved in Redis");
    }

    @GetMapping("/get")
    public ResponseEntity<String> get(@RequestParam(name = "key") String key) {
        String value = redisService.getData(key);
        return value != null ?
                ResponseEntity.ok(value) :
                ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found");
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> delete(@RequestParam(name = "key") String key) {
        boolean deleted = redisService.deleteData(key);
        return deleted ?
                ResponseEntity.ok("Data deleted for key: " + key) :
                ResponseEntity.status(HttpStatus.NOT_FOUND).body("Key not found or already deleted");
    }
}

