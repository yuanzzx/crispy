package com.yuandragon.crispy.raft.meta;


import com.yuandragon.crispy.common.utils.IpUtil;

import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author: yuanzhanzhenxing
 */
public class Peer {

    /**
     * 节点锁
     */
    private ReentrantLock lock = new ReentrantLock();

    /**
     * 原始地址
     */
    private String rawAddress;

    /**
     * http地址
     */
    private String address;

    /**
     * raft地址
     */
    private String raftAddress;

    public Peer(String address) {
        if (address.contains("127.0.0.1")) {
            address = address.replace("127.0.0.1", IpUtil.getHostAddress());
        }
        if (address.contains("localhost")) {
            address = address.replace("localhost", IpUtil.getHostAddress());
        }
        this.rawAddress = address;
        this.address = "http://" + address;
        this.raftAddress = this.address + "/raft";
    }

    /**
     * 获取锁
     *
     * @return
     */
    public boolean getLock() {
        return lock.tryLock();
    }

    /**
     * 判断锁是否已经被获取
     *
     * @return
     */
    public boolean isLocked() {
        return lock.isLocked();
    }

    /**
     * 解锁
     */
    public void unLock() {
        lock.unlock();
    }

    public void setLock(ReentrantLock lock) {
        this.lock = lock;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getRaftAddress() {
        return raftAddress;
    }

    public void setRaftAddress(String raftAddress) {
        this.raftAddress = raftAddress;
    }

    public String getRawAddress() {
        return rawAddress;
    }

    public void setRawAddress(String rawAddress) {
        this.rawAddress = rawAddress;
    }

    @Override
    public String toString() {
        return "Peer{" +
                "lock=" + lock +
                ", rawAddress='" + rawAddress + '\'' +
                ", address='" + address + '\'' +
                ", raftAddress='" + raftAddress + '\'' +
                '}';
    }
}
