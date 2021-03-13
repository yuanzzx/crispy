package com.yuandragon.crispy.raft.meta;


import com.yuandragon.crispy.common.setting.CrispySettingConfig;
import com.yuandragon.crispy.common.utils.IpUtil;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @Author: yuanzhanzhenxing
 */
public class PeersManager implements Serializable {

    /**
     * 记录主节点
     */
    private volatile Peer leader;

    /**
     * 记录当前节点信息
     */
    private Peer self;

    /**
     * 记录除了自己的所有的节点
     */
    private final List<Peer> peers = new ArrayList<>();

    /**
     * 正在同步日志的节点
     */
    private final Set<Peer> syncPeers = new HashSet<>();

    /**
     * raft 通过数量
     */
    private int passesQuantity = 1;

    private PeersManager() {
        String address = IpUtil.getHostAddress() + ":" + CrispySettingConfig.SERVER_PORT;
        self = new Peer(address);
    }

    public void setSelf(Peer peer) {
        self = peer;
    }

    public Peer getSelf() {
        return self;
    }

    public void addPeer(Peer peer) {
        peers.add(peer);
        this.passesQuantity = Integer.parseInt(
                new BigDecimal(peers.size()).divide(new BigDecimal(2), 0, BigDecimal.ROUND_HALF_UP).toString());
    }

    public void removePeer(Peer peer) {
        peers.remove(peer);
    }

    public void addSyncPeer(Peer peer) {
        syncPeers.add(peer);
    }

    public void removeSyncPeer(Peer peer) {
        syncPeers.remove(peer);
    }

    public List<Peer> getPeers() {
        return peers;
    }

    public int getPeersSize() {
        return peers.size();
    }

    public int getSyncPeersSize() {
        return syncPeers.size();
    }

    public Peer getLeader() {
        return leader;
    }

    public void setLeader(Peer peer) {
        leader = peer;
    }

    public int getPassesQuantity() {
        return passesQuantity;
    }

    public void setPassesQuantity(int passesQuantity) {
        this.passesQuantity = passesQuantity;
    }

    private static final PeersManager INSTANCE = new PeersManager();

    public static PeersManager getInstance() {
        return INSTANCE;
    }

}
