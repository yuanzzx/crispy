package com.yuandragon.crispy.raft.pojo.resp;

/**
 * @Author: yuanzhanzhenxing
 */
public class CompareLogResp {

    public CompareLogResp(Boolean syncFlag) {
        this.syncFlag = syncFlag;
    }

    /**
     * 同步标志， true表示同步， false表示无需同步
     */
    private Boolean syncFlag;

    public Boolean getSyncFlag() {
        return syncFlag;
    }

    public void setSyncFlag(Boolean syncFlag) {
        this.syncFlag = syncFlag;
    }
}
