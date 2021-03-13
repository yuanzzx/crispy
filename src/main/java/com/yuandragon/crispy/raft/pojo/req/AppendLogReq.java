package com.yuandragon.crispy.raft.pojo.req;


import com.yuandragon.crispy.raft.pojo.bean.LogEntry;
import com.yuandragon.crispy.raft.pojo.common.BaseReq;

import java.util.List;

/**
 * @Author: yuanzhanzhenxing
 */
public class AppendLogReq extends BaseReq {

    /**
     * 日志批次号
     */
    private String logBatchNumber;

    /**
     * 领导人的 Id，以便于跟随者重定向请求
     */
    private String leaderId;

    /**
     * 新的日志条目紧随之前的索引值
     */
    private long prevLogIndex;

    /**
     * prevLogIndex 条目的任期号
     */
    private long preLogTerm;

    /**
     * 准备存储的日志条目（表示心跳时为空；一次性发送多个是为了提高效率）
     */
    private List<LogEntry> entries;

    /**
     * 领导人已经提交的日志的索引值
     */
    private long leaderCommit;

    public AppendLogReq() {
    }

    public String getLogBatchNumber() {
        return logBatchNumber;
    }

    public void setLogBatchNumber(String logBatchNumber) {
        this.logBatchNumber = logBatchNumber;
    }

    public String getLeaderId() {
        return leaderId;
    }

    public void setLeaderId(String leaderId) {
        this.leaderId = leaderId;
    }

    public long getPrevLogIndex() {
        return prevLogIndex;
    }

    public void setPrevLogIndex(long prevLogIndex) {
        this.prevLogIndex = prevLogIndex;
    }

    public long getPreLogTerm() {
        return preLogTerm;
    }

    public void setPreLogTerm(long preLogTerm) {
        this.preLogTerm = preLogTerm;
    }

    public List<LogEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<LogEntry> entries) {
        this.entries = entries;
    }

    public long getLeaderCommit() {
        return leaderCommit;
    }

    public void setLeaderCommit(long leaderCommit) {
        this.leaderCommit = leaderCommit;
    }

    @Override
    public String toString() {
        return "AppendLogReq{" +
                "logBatchNumber='" + logBatchNumber + '\'' +
                ", leaderId='" + leaderId + '\'' +
                ", prevLogIndex=" + prevLogIndex +
                ", preLogTerm=" + preLogTerm +
                ", entries=" + entries +
                ", leaderCommit=" + leaderCommit +
                ", currentTerm=" + currentTerm +
                ", serverAddress='" + serverAddress + '\'' +
                '}';
    }
}
