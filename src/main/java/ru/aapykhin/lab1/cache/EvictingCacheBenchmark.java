package ru.aapykhin.lab1.cache;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * JMH бенчмарк для сравнения SimpleLRU и OptimizedLRU кэшей.
 * Измеряет время выполнения BATCH_SIZE операций в миллисекундах.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 2, jvmArgs = {"-Xms1g", "-Xmx1g", "-XX:+UseG1GC"})
public class EvictingCacheBenchmark {

    private static final int SAMPLE_SIZE = 1 << 20;
    private static final int BATCH_SIZE = 100_000;

    @Param({"Simple", "Optimized"})
    private String cacheType;

    @Param({"10000", "50000"})
    private int capacity;

    private EvictingCache<Integer, Integer> cache;
    private int[] keys;
    private boolean[] writes;
    private int mask;
    private int idx;

    @Setup(Level.Trial)
    public void setup() {
        cache = createCache(cacheType, capacity);

        for (int i = 0; i < capacity; i++) {
            cache.put(i, i);
        }

        int keySpace = capacity * 2;
        mask = SAMPLE_SIZE - 1;
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        keys = new int[SAMPLE_SIZE];
        writes = new boolean[SAMPLE_SIZE];
        for (int i = 0; i < SAMPLE_SIZE; i++) {
            keys[i] = rnd.nextInt(keySpace);
            writes[i] = rnd.nextDouble() < 0.20;
        }
    }

    private EvictingCache<Integer, Integer> createCache(String type, int cap) {
        return switch (type) {
            case "Simple" -> new SimpleLRUCache<>(cap);
            case "Optimized" -> new OptimizedLRUCache<>(cap);
            default -> throw new IllegalArgumentException("Unknown cache type: " + type);
        };
    }

    @Benchmark
    public void benchGet(Blackhole bh) {
        for (int j = 0; j < BATCH_SIZE; j++) {
            int key = keys[idx++ & mask];
            bh.consume(cache.get(key));
        }
    }

    @Benchmark
    public void benchPut(Blackhole bh) {
        for (int j = 0; j < BATCH_SIZE; j++) {
            int key = keys[idx++ & mask];
            bh.consume(cache.put(key, key));
        }
    }

    @Benchmark
    public void benchMixed(Blackhole bh) {
        for (int j = 0; j < BATCH_SIZE; j++) {
            int i = idx++ & mask;
            int key = keys[i];
            if (writes[i]) {
                bh.consume(cache.put(key, key));
            } else {
                bh.consume(cache.get(key));
            }
        }
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(EvictingCacheBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
}
