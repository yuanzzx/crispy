package com.yuandragon.crispy.cache.core;


import com.yuandragon.crispy.cache.bean.CacheValue;

import java.util.Map;

/**
 * @Author: yuanzhanzhenxing
 */
public class CacheFacade {

    /**
     * 缓存实现
     */
    private final Cahce cahce = CahceImpl.getCahce();

    /**
     * 获取缓存
     *
     * @param key
     * @return
     */
    public CacheValue get(String key) {
        return cahce.get(key);
    }

    /**
     * 新增或更新
     *
     * @param key
     * @param cacheValue
     * @return
     */
    public CacheValue put(String key, CacheValue cacheValue) {
        cahce.put(key, cacheValue);
        return cacheValue;
    }

    /**
     * 删除
     * @param key
     * @return
     */
    public CacheValue delete(String key) {
        return cahce.delete(key);
    }

    /**
     * 将map转换为cachevalue
     *
     * @param params
     * @return
     */
    public CacheValue convertToCacheValue(String key, Map<String, Object> params) {
        CacheValue cacheValue = new CacheValue();
        cacheValue.setKey(key);
        cacheValue.setValue(params.get("value").toString());
        cacheValue.setCreateTime((long) params.get("createTime"));
        return cacheValue;
    }

    private CacheFacade() {
    }

    private final static CacheFacade INSTANCE = new CacheFacade();

    public static CacheFacade getInstance() {
        return INSTANCE;
    }
}
