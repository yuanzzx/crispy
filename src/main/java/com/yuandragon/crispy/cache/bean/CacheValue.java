package com.yuandragon.crispy.cache.bean;

/**
 * @Author: yuanzhanzhenxing
 */
public class CacheValue {

    /**
     * key 值
     */
    private String key;

    /**
     * value 值
     */
    private String value;

    /**
     * 创建时间
     */
    private Long createTime;

    /**
     * 存活时间
     */
    private Integer ttl;

    /**
     * 过期时间
     */
    private Long expiration;

    public CacheValue() {
    }

    public CacheValue(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Integer getTtl() {
        return ttl;
    }

    public void setTtl(Integer ttl) {
        this.ttl = ttl;
    }

    @Override
    public String toString() {
        return "CacheValue{" +
                "key='" + key + '\'' +
                ", value='" + value + '\'' +
                ", createTime='" + createTime + '\'' +
                ", ttl=" + ttl +
                ", expiration=" + expiration +
                '}';
    }

    public Long getExpiration() {
        return expiration;
    }

    public void setExpiration(Long expiration) {
        this.expiration = expiration;
    }
}
