package com.yuandragon.crispy.raft.pojo.resp;

import java.io.Serializable;

/**
 * @Author: yuanzhanzhenxing
 */
public class AppendLogResp implements Serializable {

    /**
     * 当前的任期号，用于领导人去更新自己
     */
    private long term;

    /**
     * 跟随者包含了匹配上 prevLogIndex 和 prevLogTerm 的日志时为真
     */
    private boolean success;

    public long getTerm() {
        return term;
    }

    public void setTerm(long term) {
        this.term = term;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    @Override
    public String toString() {
        return "AppendLogResp{" +
                "term=" + term +
                ", success=" + success +
                '}';
    }


    public static final class Builder {
        private long term;
        private boolean success;

        private Builder() {
        }

        public static Builder anAppendLogResp() {
            return new Builder();
        }

        public Builder term(long term) {
            this.term = term;
            return this;
        }

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public AppendLogResp build() {
            AppendLogResp appendLogResp = new AppendLogResp();
            appendLogResp.setTerm(term);
            appendLogResp.setSuccess(success);
            return appendLogResp;
        }
    }
}
