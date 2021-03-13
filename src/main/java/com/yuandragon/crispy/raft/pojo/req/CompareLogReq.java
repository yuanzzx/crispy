package com.yuandragon.crispy.raft.pojo.req;

import com.yuandragon.crispy.raft.pojo.bean.LogEntry;
import com.yuandragon.crispy.raft.pojo.common.BaseReq;

/**
 * @Author: yuanzhanzhenxing
 */
public class CompareLogReq extends BaseReq {

    /**
     * 最后一条日志
     */
    private LogEntry logEntry;

    public CompareLogReq() {
    }

    public CompareLogReq(LogEntry logEntry) {
        this.logEntry = logEntry;
    }

    public LogEntry getLogEntry() {
        return logEntry;
    }

    public void setLogEntry(LogEntry logEntry) {
        this.logEntry = logEntry;
    }

    @Override
    public String toString() {
        return "CompareLogReq{" +
                "logEntry=" + logEntry +
                ", currentTerm=" + currentTerm +
                ", serverAddress='" + serverAddress + '\'' +
                '}';
    }
}

