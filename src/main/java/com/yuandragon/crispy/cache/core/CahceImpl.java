package com.yuandragon.crispy.cache.core;

import com.yuandragon.crispy.cache.bean.CacheValue;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: yuanzhanzhenxing
 */
public class CahceImpl implements Cahce {

    private final ConcurrentHashMap<String, CacheValue> cahceMap = new ConcurrentHashMap<>(2 << 15);

    private CahceImpl() {

    }

    public final static Cahce CAHCE = new CahceImpl();

    public static Cahce getCahce() {
        return CAHCE;
    }

    @Override
    public CacheValue get(String key) {
        return cahceMap.get(key);
    }

    @Override
    public void put(String key, CacheValue value) {
        cahceMap.put(key, value);
    }

    @Override
    public CacheValue delete(String key) {
        return cahceMap.remove(key);
    }

    @Override
    public boolean delete(String key, CacheValue value) {
        return cahceMap.remove(key, value);
    }

    @Override
    public long sum() {
        return cahceMap.mappingCount();
    }
}
