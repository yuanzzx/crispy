package com.yuandragon.crispy.raft.pojo.req;


import com.yuandragon.crispy.raft.pojo.common.BaseReq;

import java.util.Map;

/**
 * @Author: yuanzhanzhenxing
 */
public class WrittenReq extends BaseReq {

    /**
     * 操作类型
     */
    private int command;

    /**
     * 操作key
     */
    private String key;

    /**
     * 操作value
     */
    private Map<String, Object> value;


    public int getCommand() {
        return command;
    }

    public void setCommand(int command) {
        this.command = command;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Map<String, Object> getValue() {
        return value;
    }

    public void setValue(Map<String, Object> value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "WrittenReq{" +
                "command=" + command +
                ", key='" + key + '\'' +
                ", value=" + value +
                ", currentTerm=" + currentTerm +
                ", serverAddress='" + serverAddress + '\'' +
                '}';
    }
}
