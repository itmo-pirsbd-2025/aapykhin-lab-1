package ru.aapykhin.lab1.cache;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class EvictingCacheTest {

    static Stream<EvictingCache<String, Integer>> cacheProvider() {
        return Stream.of(
                new SimpleLRUCache<>(3),
                new OptimizedLRUCache<>(3)
        );
    }

    @ParameterizedTest
    @MethodSource("cacheProvider")
    void basicOperations(EvictingCache<String, Integer> cache) {
        assertNull(cache.put("a", 1));
        assertNull(cache.put("b", 2));
        assertNull(cache.put("c", 3));

        assertEquals(3, cache.size());
        assertEquals(3, cache.capacity());

        assertEquals(Optional.of(1), cache.get("a"));
        assertEquals(Optional.of(2), cache.get("b"));
        assertEquals(Optional.of(3), cache.get("c"));
        assertEquals(Optional.empty(), cache.get("d"));
    }

    @ParameterizedTest
    @MethodSource("cacheProvider")
    void updateExistingKey(EvictingCache<String, Integer> cache) {
        cache.put("a", 1);
        assertEquals(1, cache.put("a", 10));
        assertEquals(Optional.of(10), cache.get("a"));
        assertEquals(1, cache.size());
    }

    @ParameterizedTest
    @MethodSource("cacheProvider")
    void removeKey(EvictingCache<String, Integer> cache) {
        cache.put("a", 1);
        cache.put("b", 2);

        assertEquals(Optional.of(1), cache.remove("a"));
        assertEquals(Optional.empty(), cache.get("a"));
        assertEquals(1, cache.size());

        assertEquals(Optional.empty(), cache.remove("nonexistent"));
    }

    @ParameterizedTest
    @MethodSource("cacheProvider")
    void clearCache(EvictingCache<String, Integer> cache) {
        cache.put("a", 1);
        cache.put("b", 2);
        cache.clear();

        assertEquals(0, cache.size());
        assertEquals(Optional.empty(), cache.get("a"));
    }

    @ParameterizedTest
    @MethodSource("cacheProvider")
    void nullKeyRejected(EvictingCache<String, Integer> cache) {
        assertThrows(NullPointerException.class, () -> cache.put(null, 1));
        assertThrows(NullPointerException.class, () -> cache.get(null));
        assertThrows(NullPointerException.class, () -> cache.remove(null));
    }

    @Test
    void simpleLruEvictionPolicy() {
        SimpleLRUCache<String, Integer> cache = new SimpleLRUCache<>(3);
        cache.put("a", 1);
        cache.put("b", 2);
        cache.put("c", 3);

        // Обращаемся к "a", он становится MRU
        cache.get("a");

        // Добавляем "d" — вытесняется "b" (LRU)
        cache.put("d", 4);

        assertEquals(Optional.empty(), cache.get("b"), "b should be evicted (LRU)");
        assertEquals(Optional.of(1), cache.get("a"));
        assertEquals(Optional.of(3), cache.get("c"));
        assertEquals(Optional.of(4), cache.get("d"));
    }

    @Test
    void optimizedLruEvictionPolicy() {
        OptimizedLRUCache<String, Integer> cache = new OptimizedLRUCache<>(3);
        cache.put("a", 1);
        cache.put("b", 2);
        cache.put("c", 3);

        // Обращаемся к "a", он становится MRU
        cache.get("a");

        // Добавляем "d" — вытесняется "b" (LRU)
        cache.put("d", 4);

        assertEquals(Optional.empty(), cache.get("b"), "b should be evicted (LRU)");
        assertEquals(Optional.of(1), cache.get("a"));
        assertEquals(Optional.of(3), cache.get("c"));
        assertEquals(Optional.of(4), cache.get("d"));
    }

    @Test
    void invalidCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new SimpleLRUCache<>(0));
        assertThrows(IllegalArgumentException.class, () -> new SimpleLRUCache<>(-1));
        assertThrows(IllegalArgumentException.class, () -> new OptimizedLRUCache<>(0));
        assertThrows(IllegalArgumentException.class, () -> new OptimizedLRUCache<>(-1));
    }

    @ParameterizedTest
    @MethodSource("cacheProvider")
    void evictionUnderLoad(EvictingCache<String, Integer> cache) {
        // Добавляем больше элементов, чем capacity
        for (int i = 0; i < 10; i++) {
            cache.put("key" + i, i);
        }

        // Должно остаться только capacity элементов
        assertEquals(3, cache.size());

        // Последние 3 должны быть в кэше
        assertEquals(Optional.of(7), cache.get("key7"));
        assertEquals(Optional.of(8), cache.get("key8"));
        assertEquals(Optional.of(9), cache.get("key9"));

        // Первые должны быть вытеснены
        assertEquals(Optional.empty(), cache.get("key0"));
        assertEquals(Optional.empty(), cache.get("key6"));
    }

    @ParameterizedTest
    @MethodSource("cacheProvider")
    void putUpdateMovesToHead(EvictingCache<String, Integer> cache) {
        cache.put("a", 1);
        cache.put("b", 2);
        cache.put("c", 3);

        // Update "a" — должен стать MRU
        cache.put("a", 100);

        // Добавляем "d" — вытесняется "b" (теперь LRU)
        cache.put("d", 4);

        assertEquals(Optional.empty(), cache.get("b"), "b should be evicted");
        assertEquals(Optional.of(100), cache.get("a"));
    }

    @ParameterizedTest
    @MethodSource("cacheProvider")
    void removeAndReinsert(EvictingCache<String, Integer> cache) {
        cache.put("a", 1);
        cache.put("b", 2);

        cache.remove("a");
        assertEquals(1, cache.size());

        cache.put("a", 10);
        assertEquals(2, cache.size());
        assertEquals(Optional.of(10), cache.get("a"));
    }
}
