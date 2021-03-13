package com.yuandragon.crispy.raft.pojo.bean;

import java.util.Map;

/**
 * @Author: yuanzhanzhenxing
 */
public class StateMachineLog {

    /**
     * 操作类型
     */
    private Integer command;

    /**
     * 操作key
     */
    private String key;

    /**
     * 操作value
     */
    private Map<String, Object> value;

    public StateMachineLog() {
    }

    public StateMachineLog(Integer command, String key, Map<String, Object> value) {
        this.command = command;
        this.key = key;
        this.value = value;
    }

    @Override
    public String toString() {
        return "StateMachineLog{" +
                "command=" + command +
                ", key='" + key + '\'' +
                ", value=" + value +
                '}';
    }

    public Integer getCommand() {
        return command;
    }

    public void setCommand(Integer command) {
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
}
