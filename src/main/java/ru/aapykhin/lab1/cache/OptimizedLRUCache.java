package ru.aapykhin.lab1.cache;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Оптимизированная реализация LRU кэша на параллельных массивах.
 *
 * Оптимизации по сравнению с SimpleLRUCache:
 * 1. Нет объектов-обёрток (Node) — данные хранятся в примитивных массивах
 * 2. Открытая адресация — лучше локальность данных
 * 3. Битовая маска вместо деления — быстрее вычисление индекса
 * 4. Inline doubly-linked list через массивы prev/next
 */
public class OptimizedLRUCache<K, V> implements EvictingCache<K, V> {

    private static final int EMPTY = -1;

    private final int capacity;
    private final int tableMask;

    private final int[] table;

    private final Object[] keys;
    private final Object[] values;
    private final int[] hashCodes;

    private final int[] prev;
    private final int[] next;

    private final int[] chainNext;

    private int head;
    private int tail;

    private int freeList;
    private int size;

    public OptimizedLRUCache(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.capacity = capacity;

        int tableSize = tableSizeFor((int) (capacity * 1.5));
        this.tableMask = tableSize - 1;
        this.table = new int[tableSize];
        Arrays.fill(table, EMPTY);

        this.keys = new Object[capacity];
        this.values = new Object[capacity];
        this.hashCodes = new int[capacity];
        this.prev = new int[capacity];
        this.next = new int[capacity];
        this.chainNext = new int[capacity];

        Arrays.fill(prev, EMPTY);
        Arrays.fill(next, EMPTY);
        Arrays.fill(chainNext, EMPTY);

        for (int i = 0; i < capacity - 1; i++) {
            chainNext[i] = i + 1;
        }
        chainNext[capacity - 1] = EMPTY;
        freeList = 0;

        head = EMPTY;
        tail = EMPTY;
        size = 0;
    }

    private static int tableSizeFor(int cap) {
        int n = cap - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        if (n < 0) {
            return 1;
        }
        if (n >= (1 << 30)) {
            return 1 << 30;
        }
        return n + 1;
    }

    private static int spreadHash(int h) {
        h ^= (h >>> 16);
        h *= 0x85ebca6b;
        h ^= (h >>> 13);
        return h;
    }

    @Override
    public V put(K key, V value) {
        Objects.requireNonNull(key, "key");

        int hash = spreadHash(key.hashCode());
        int bucket = hash & tableMask;

        int slot = table[bucket];
        while (slot != EMPTY) {
            if (hashCodes[slot] == hash && key.equals(keys[slot])) {
                // Key exists — update value and move to head
                @SuppressWarnings("unchecked")
                V oldValue = (V) values[slot];
                values[slot] = value;
                moveToHead(slot);
                return oldValue;
            }
            slot = chainNext[slot];
        }

        if (size >= capacity) {
            evictLRU();
        }

        int newSlot = freeList;
        freeList = chainNext[freeList];

        keys[newSlot] = key;
        values[newSlot] = value;
        hashCodes[newSlot] = hash;

        chainNext[newSlot] = table[bucket];
        table[bucket] = newSlot;

        addToHead(newSlot);
        size++;

        return null;
    }

    @Override
    public Optional<V> get(K key) {
        Objects.requireNonNull(key, "key");

        int hash = spreadHash(key.hashCode());
        int bucket = hash & tableMask;

        int slot = table[bucket];
        while (slot != EMPTY) {
            if (hashCodes[slot] == hash && key.equals(keys[slot])) {
                moveToHead(slot);
                @SuppressWarnings("unchecked")
                V value = (V) values[slot];
                return Optional.of(value);
            }
            slot = chainNext[slot];
        }

        return Optional.empty();
    }

    @Override
    public Optional<V> remove(K key) {
        Objects.requireNonNull(key, "key");

        int hash = spreadHash(key.hashCode());
        int bucket = hash & tableMask;

        int slot = table[bucket];
        int prevSlot = EMPTY;
        while (slot != EMPTY) {
            if (hashCodes[slot] == hash && key.equals(keys[slot])) {
                @SuppressWarnings("unchecked")
                V oldValue = (V) values[slot];

                if (prevSlot == EMPTY) {
                    table[bucket] = chainNext[slot];
                } else {
                    chainNext[prevSlot] = chainNext[slot];
                }

                removeFromList(slot);

                keys[slot] = null;
                values[slot] = null;
                chainNext[slot] = freeList;
                freeList = slot;
                size--;

                return Optional.of(oldValue);
            }
            prevSlot = slot;
            slot = chainNext[slot];
        }

        return Optional.empty();
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public int capacity() {
        return capacity;
    }

    @Override
    public void clear() {
        Arrays.fill(table, EMPTY);
        Arrays.fill(keys, null);
        Arrays.fill(values, null);
        Arrays.fill(prev, EMPTY);
        Arrays.fill(next, EMPTY);

        for (int i = 0; i < capacity - 1; i++) {
            chainNext[i] = i + 1;
        }
        chainNext[capacity - 1] = EMPTY;
        freeList = 0;

        head = EMPTY;
        tail = EMPTY;
        size = 0;
    }

    private void evictLRU() {
        if (tail == EMPTY) return;

        int slot = tail;
        int hash = hashCodes[slot];
        int bucket = hash & tableMask;

        int s = table[bucket];
        int ps = EMPTY;
        while (s != EMPTY) {
            if (s == slot) {
                if (ps == EMPTY) {
                    table[bucket] = chainNext[s];
                } else {
                    chainNext[ps] = chainNext[s];
                }
                break;
            }
            ps = s;
            s = chainNext[s];
        }

        removeFromList(slot);

        keys[slot] = null;
        values[slot] = null;
        chainNext[slot] = freeList;
        freeList = slot;
        size--;
    }

    private void addToHead(int slot) {
        prev[slot] = EMPTY;
        next[slot] = head;

        if (head != EMPTY) {
            prev[head] = slot;
        }
        head = slot;

        if (tail == EMPTY) {
            tail = slot;
        }
    }

    private void removeFromList(int slot) {
        int p = prev[slot];
        int n = next[slot];

        if (p != EMPTY) {
            next[p] = n;
        } else {
            head = n;
        }

        if (n != EMPTY) {
            prev[n] = p;
        } else {
            tail = p;
        }

        prev[slot] = EMPTY;
        next[slot] = EMPTY;
    }

    private void moveToHead(int slot) {
        if (slot == head) return;
        removeFromList(slot);
        addToHead(slot);
    }
}
