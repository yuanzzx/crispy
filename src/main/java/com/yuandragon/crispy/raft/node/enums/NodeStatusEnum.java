package com.yuandragon.crispy.raft.node.enums;

/**
 * @Author: yuanzhanzhenxing
 */
public class NodeStatusEnum {

    /**
     * 主
     */
    public static int LEADER = 1;

    /**
     * 从
     */
    public static int FOLLOWER = 2;

    /**
     * 候选人
     */
    public static int CANDIDATE = 3;


}
