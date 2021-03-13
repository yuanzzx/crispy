package com.yuandragon.crispy.raft.pojo.resp;

import com.alibaba.fastjson.annotation.JSONField;
import com.yuandragon.crispy.raft.pojo.bean.LogEntry;

import java.util.List;

/**
 * @Author: yuanzhanzhenxing
 */
public class ApplyStateMachineResp {

    /**
     * 应用日志集合
     */
    @JSONField(serialize = false)
    private List<LogEntry> entries;

    /**
     * 跟随者包含了匹配上 prevLogIndex 和 prevLogTerm 的日志时为真
     */
    private boolean success;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public List<LogEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<LogEntry> entries) {
        this.entries = entries;
    }

    @Override
    public String toString() {
        return "ApplyStateMachineResp{" +
                "success=" + success +
                '}';
    }
}
