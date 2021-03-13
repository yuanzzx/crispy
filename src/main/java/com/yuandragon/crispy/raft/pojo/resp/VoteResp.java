package com.yuandragon.crispy.raft.pojo.resp;

import java.io.Serializable;

/**
 * @Author: yuanzhanzhenxing
 */
public class VoteResp implements Serializable {

    /**
     * 当前任期号，以便于候选人去更新自己的任期
     */
    private Long term;

    /**
     * 候选人赢得了此张选票时为真
     */
    private Boolean voteGranted;

    public VoteResp() {
    }

    public Long getTerm() {
        return term;
    }

    public void setTerm(Long term) {
        this.term = term;
    }

    public Boolean getVoteGranted() {
        return voteGranted;
    }

    public void setVoteGranted(Boolean voteGranted) {
        this.voteGranted = voteGranted;
    }

    @Override
    public String toString() {
        return "VoteResp{" +
                "term=" + term +
                ", voteGranted=" + voteGranted +
                '}';
    }

    public static final class Builder {
        private Long term;
        private Boolean voteGranted;

        private Builder() {
        }

        public static Builder aVoteResp() {
            return new Builder();
        }

        public Builder term(Long term) {
            this.term = term;
            return this;
        }

        public Builder voteGranted(Boolean voteGranted) {
            this.voteGranted = voteGranted;
            return this;
        }

        public VoteResp build() {
            VoteResp voteResp = new VoteResp();
            voteResp.setTerm(term);
            voteResp.setVoteGranted(voteGranted);
            return voteResp;
        }
    }
}
