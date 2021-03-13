package com.yuandragon.crispy.raft.pojo.req;

import com.yuandragon.crispy.raft.pojo.common.BaseReq;

/**
 * @Author: yuanzhanzhenxing
 */
public class ApplyStateMachineReq extends BaseReq {

    /**
     * 应用 id
     */
    private String logBatchNumber;

    public String getLogBatchNumber() {
        return logBatchNumber;
    }

    public void setLogBatchNumber(String logBatchNumber) {
        this.logBatchNumber = logBatchNumber;
    }

    @Override
    public String toString() {
        return "ApplyStateMachineReq{" +
                "logBatchNumber='" + logBatchNumber + '\'' +
                ", currentTerm=" + currentTerm +
                ", serverAddress='" + serverAddress + '\'' +
                '}';
    }
}
