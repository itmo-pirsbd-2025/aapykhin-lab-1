package ru.aapykhin.lab1.cache;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Базовая реализация LRU кэша на основе LinkedHashMap.
 */
public class SimpleLRUCache<K, V> implements EvictingCache<K, V> {

    private final int capacity;
    private final LinkedHashMap<K, V> map;

    public SimpleLRUCache(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.capacity = capacity;
        this.map = new LinkedHashMap<>(capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > SimpleLRUCache.this.capacity;
            }
        };
    }

    @Override
    public V put(K key, V value) {
        Objects.requireNonNull(key, "key");
        return map.put(key, value);
    }

    @Override
    public Optional<V> get(K key) {
        Objects.requireNonNull(key, "key");
        return Optional.ofNullable(map.get(key));
    }

    @Override
    public Optional<V> remove(K key) {
        Objects.requireNonNull(key, "key");
        return Optional.ofNullable(map.remove(key));
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public int capacity() {
        return capacity;
    }

    @Override
    public void clear() {
        map.clear();
    }
}
