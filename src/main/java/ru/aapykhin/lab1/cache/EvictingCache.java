package ru.aapykhin.lab1.cache;

import java.util.Optional;

public interface EvictingCache<K, V> {

    V put(K key, V value);

    Optional<V> get(K key);

    Optional<V> remove(K key);

    int size();

    int capacity();

    void clear();
}
