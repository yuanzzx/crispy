package com.yuandragon.crispy.raft.pojo.resp;

import com.yuandragon.crispy.raft.meta.Peer;

import java.util.concurrent.Future;

/**
 * @Author: yuanzhanzhenxing
 */
public class ReplicationResp {

    /**
     * 节点信息
     */
    private Peer peer;

    /**
     * 是否成功
     */
    private Future<Boolean> result;

    public Peer getPeer() {
        return peer;
    }

    public void setPeer(Peer peer) {
        this.peer = peer;
    }

    public Future<Boolean> getResult() {
        return result;
    }

    public void setResult(Future<Boolean> result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "ReplicationResult{" +
                "peer=" + peer +
                ", result=" + result +
                '}';
    }
}
