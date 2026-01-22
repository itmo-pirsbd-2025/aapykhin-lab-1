package ru.aapykhin.lab1.cache;

import java.util.Random;

/**
 * Профилировочный runner для OptimizedLRU Cache
 */
public final class ProfileOptimizedLRU {

    private static final int OPERATIONS = 10_000_000;
    private static final int CAPACITY = 50_000;
    private static final int KEY_SPACE = CAPACITY * 2;

    public static void main(String[] args) {
        EvictingCache<Integer, Integer> cache = new OptimizedLRUCache<>(CAPACITY);
        Random random = new Random(42);

        for (int i = 0; i < CAPACITY; i++) {
            cache.put(i, i);
        }

        System.out.println("Warmup...");
        runWorkload(cache, random, OPERATIONS / 10);

        System.out.println("Profiling OptimizedLRU Cache...");
        random.setSeed(42);
        long start = System.nanoTime();
        runWorkload(cache, random, OPERATIONS);
        long elapsed = System.nanoTime() - start;

        System.out.printf("Done: %d ms, %.2f ops/ms%n",
                elapsed / 1_000_000,
                OPERATIONS * 1_000_000.0 / elapsed);
    }

    private static void runWorkload(EvictingCache<Integer, Integer> cache, Random random, int ops) {
        for (int i = 0; i < ops; i++) {
            int key = random.nextInt(KEY_SPACE);
            double dice = random.nextDouble();
            if (dice < 0.20) {
                cache.put(key, i);
            } else {
                cache.get(key);
            }
        }
    }
}
