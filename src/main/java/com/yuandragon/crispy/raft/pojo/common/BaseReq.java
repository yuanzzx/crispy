package com.yuandragon.crispy.raft.pojo.common;

import java.io.Serializable;

/**
 * @Author: yuanzhanzhenxing
 */
public class BaseReq implements Serializable {

    /**
     * 当前任期号
     */
    protected Long currentTerm;

    /**
     * 当前服务地址
     */
    protected String serverAddress;

    public Long getCurrentTerm() {
        return currentTerm;
    }

    public void setCurrentTerm(Long currentTerm) {
        this.currentTerm = currentTerm;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    @Override
    public String toString() {
        return "BaseReq{" +
                "currentTerm=" + currentTerm +
                ", serverAddress='" + serverAddress + '\'' +
                '}';
    }
}
