package com.yuandragon.crispy.raft.pojo.req;


import com.yuandragon.crispy.raft.pojo.common.BaseReq;

/**
 * @Author: yuanzhanzhenxing
 */
public class VoteReq extends BaseReq {

    /**
     * 候选人Id
     */
    private String candidateId;

    /**
     * 候选人的最后一条日志的索引值
     */
    private long lastLogIndex;

    /**
     * 候选人的最后一条日志的任期号
     */
    private long lastLogTerm;

    public String getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(String candidateId) {
        this.candidateId = candidateId;
    }

    public long getLastLogIndex() {
        return lastLogIndex;
    }

    public void setLastLogIndex(long lastLogIndex) {
        this.lastLogIndex = lastLogIndex;
    }

    public long getLastLogTerm() {
        return lastLogTerm;
    }

    public void setLastLogTerm(long lastLogTerm) {
        this.lastLogTerm = lastLogTerm;
    }

    @Override
    public String toString() {
        return "RvoteReq{" +
                "candidateId='" + candidateId + '\'' +
                ", lastLogIndex=" + lastLogIndex +
                ", lastLogTerm=" + lastLogTerm +
                ", currentTerm=" + currentTerm +
                ", serverAddress='" + serverAddress + '\'' +
                '}';
    }
}
