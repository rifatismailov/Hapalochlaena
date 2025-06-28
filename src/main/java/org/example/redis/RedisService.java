package org.example.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * RedisService — сервіс для роботи з Redis як із кешем або тимчасовим сховищем.
 * <p>
 * Реалізує базові операції:
 * - збереження та отримання ключ-значення (String → String)
 * - видалення ключів
 * - операції з чергою (списком): додавання в кінець, витяг з початку
 */
@Service
public class RedisService {

    /**
     * RedisTemplate — шаблон для взаємодії з Redis.
     * Працює з ключами та значеннями типу String.
     * Впроваджується автоматично через Spring.
     */
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * Зберігає значення у Redis за вказаним ключем.
     *
     * @param key   ключ, за яким зберігається значення
     * @param value значення, яке потрібно зберегти
     */
    public void saveData(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * Отримує значення з Redis за вказаним ключем.
     *
     * @param key ключ, за яким зберігається значення
     * @return значення, що відповідає ключу, або null, якщо ключ не існує
     */
    public String getData(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * Видаляє запис з Redis за вказаним ключем.
     *
     * @param key ключ для видалення
     * @return true, якщо запис було успішно видалено; false — якщо такого ключа не існувало
     */
    public boolean deleteData(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    /**
     * Додає значення в кінець списку Redis (черга).
     * Якщо ключ ще не існує, створюється новий список.
     *
     * @param key   ключ списку
     * @param value значення, яке потрібно додати
     */
    public void addToLine(String key, String value) {
        redisTemplate.opsForList().rightPush(key, value);
    }

    /**
     * Витягує перше значення зі списку Redis (черга).
     * Після витягу значення видаляється з початку списку.
     *
     * @param key ключ списку
     * @return перший елемент черги або null, якщо черга порожня
     */
    public String getOnLine(String key) {
        return redisTemplate.opsForList().leftPop(key);
    }
}
