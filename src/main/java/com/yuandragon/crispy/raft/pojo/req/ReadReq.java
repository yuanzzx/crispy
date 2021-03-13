package com.yuandragon.crispy.raft.pojo.req;

import com.yuandragon.crispy.raft.pojo.common.BaseReq;

/**
 * @Author: yuanzhanzhenxing
 */
public class ReadReq extends BaseReq {


    /**
     * key
     */
    private String key;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return "ReadReq{" +
                "key='" + key + '\'' +
                ", currentTerm=" + currentTerm +
                ", serverAddress='" + serverAddress + '\'' +
                '}';
    }
}
