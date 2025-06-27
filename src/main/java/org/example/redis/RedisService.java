package org.example.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public void saveData(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public String getData(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public boolean deleteData(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    public void addToLine(String key, String value) {
        redisTemplate.opsForList().rightPush(key, value);
    }

    public String getOnLine(String key) {
        return redisTemplate.opsForList().leftPop(key);
    }

}
