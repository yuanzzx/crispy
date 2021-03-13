package com.yuandragon.crispy.raft.pojo.bean;

import java.io.Serializable;
import java.util.Map;

/**
 * @Author: yuanzhanzhenxing
 */
public class Command implements Serializable {

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

    public Command() {
    }

    public Command(Integer command, String key, Map<String, Object> value) {
        this.command = command;
        this.key = key;
        this.value = value;
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


    public static final class CommandBuilder {
        private Integer command;
        private String key;
        private Map<String, Object> value;

        private CommandBuilder() {
        }

        public static CommandBuilder aCommand() {
            return new CommandBuilder();
        }

        public CommandBuilder withCommand(Integer command) {
            this.command = command;
            return this;
        }

        public CommandBuilder withKey(String key) {
            this.key = key;
            return this;
        }

        public CommandBuilder withValue(Map<String, Object> value) {
            this.value = value;
            return this;
        }

        public Command build() {
            return new Command(command, key, value);
        }
    }

    @Override
    public String toString() {
        return "Command{" +
                "command=" + command +
                ", key='" + key + '\'' +
                ", value=" + value +
                '}';
    }
}
