package com.yuandragon.crispy.cache.core;


import com.yuandragon.crispy.cache.bean.CacheValue;

/**
 * @Author: yuanzhanzhenxing
 */
public interface Cahce {


    /**
     * 获取缓存
     *
     * @param key
     * @return
     */
    CacheValue get(String key);

    /**
     * 加入缓存
     *
     * @param key
     * @param value
     */
    void put(String key, CacheValue value);

    /**
     * 删除缓存
     *
     * @param key
     */
    CacheValue delete(String key);

    /**
     * 删除缓存 同时判断key和value是否相同
     *
     * @param key
     * @param value
     * @return
     */
    boolean delete(String key, CacheValue value);

    /**
     * 总量
     *
     * @return
     */
    long sum();
}
