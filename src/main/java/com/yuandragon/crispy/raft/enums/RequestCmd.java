package com.yuandragon.crispy.raft.enums;

/**
 * @Author: yuanzhanzhenxing
 */
public class RequestCmd {

    /**
     * 投票
     */
    public static final int VOTE = 0;

    /**
     * 日志
     */
    public static final int APPEND_LOG = 1;

    /**
     * 写入请求
     */
    public static final int WRITE = 2;

    /**
     * 应用日志到状态机
     */
    public static final int APPLY_STATE_MACHINE = 3;

    /**
     * 比对日志
     */
    public static final int COMPARE_LOG = 4;

}
