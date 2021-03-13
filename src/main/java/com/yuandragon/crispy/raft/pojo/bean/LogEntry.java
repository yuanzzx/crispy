package com.yuandragon.crispy.raft.pojo.bean;

import java.io.Serializable;

/**
 * 日志信息
 *
 * @author
 * @see //LogModule
 */

/**
 * @Author: yuanzhanzhenxing
 */
public class LogEntry implements Serializable {

    /**
     *  唯一索引
     */
    private Long index;

    /**
     * 任期
     */
    private Long term;

    /**
     * 操作内容
     */
    private Command command;

    public LogEntry() {
    }

    public LogEntry(Long term, Command command) {
        this.term = term;
        this.command = command;
    }

    public LogEntry(Long index, Long term, Command command) {
        this.index = index;
        this.term = term;
        this.command = command;
    }

    public Long getIndex() {
        return index;
    }

    public void setIndex(Long index) {
        this.index = index;
    }

    public long getTerm() {
        return term;
    }


    public void setTerm(long term) {
        this.term = term;
    }

    public Command getCommand() {
        return command;
    }

    public void setCommand(Command command) {
        this.command = command;
    }


    public static final class LogEntryBuilder {
        private Long index;
        private Long preIndex;
        private Long term;
        private Command command;

        private LogEntryBuilder() {
        }

        public static LogEntryBuilder aLogEntry() {
            return new LogEntryBuilder();
        }

        public LogEntryBuilder withIndex(Long index) {
            this.index = index;
            return this;
        }

        public LogEntryBuilder withTerm(Long term) {
            this.term = term;
            return this;
        }

        public LogEntryBuilder withCommand(Command command) {
            this.command = command;
            return this;
        }

        public LogEntry build() {
            LogEntry logEntry = new LogEntry();
            logEntry.setIndex(index);
            logEntry.setTerm(term);
            logEntry.setCommand(command);
            return logEntry;
        }
    }

    @Override
    public String toString() {
        return "LogEntry{" +
                "index=" + index +
                ", term=" + term +
                ", command=" + command +
                '}';
    }
}
