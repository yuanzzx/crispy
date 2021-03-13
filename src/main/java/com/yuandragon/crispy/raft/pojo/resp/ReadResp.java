package com.yuandragon.crispy.raft.pojo.resp;

import com.yuandragon.crispy.cache.bean.CacheValue;

/**
 * @Author: yuanzhanzhenxing
 */
public class ReadResp {


    /**
     * value
     */
    private CacheValue value;

    public ReadResp() {
    }

    public ReadResp(CacheValue value) {
        this.value = value;
    }

    public CacheValue getValue() {
        return value;
    }

    public void setValue(CacheValue value) {
        this.value = value;
    }

    public static ReadResp success() {
        return success(null);
    }

    public static ReadResp success(CacheValue cacheValue) {
        return new ReadResp(cacheValue);
    }
}
