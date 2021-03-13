package com.yuandragon.crispy.raft.node;

import com.yuandragon.crispy.cache.bean.CacheValue;
import com.yuandragon.crispy.raft.pojo.bean.StateMachineLog;
import com.yuandragon.crispy.raft.pojo.req.*;
import com.yuandragon.crispy.raft.pojo.resp.*;
import com.yuandragon.crispy.raft.stateMachine.DefaultStateMachine;
import com.yuandragon.crispy.raft.stateMachine.StateMachine;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author: yuanzhanzhenxing
 */
public class StandaloneNode implements Node {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterNode.class);
    /**
     * 状态机
     */
    public StateMachine stateMachine;

    private StandaloneNode() {
        LOGGER.info("初始化单机节点");
        stateMachine = DefaultStateMachine.getInstance();
    }

    @Override
    public WrittenResp handleWrittenRequest(WrittenReq request) {
        try {
            // 转换为持久化对象
            StateMachineLog stateMachineLog = new StateMachineLog(request.getCommand(), request.getKey(), request.getValue());
            // 持久化到磁盘
            stateMachine.apply(stateMachineLog);
            return WrittenResp.success();
        } catch (RocksDBException e) {
            // 异常则返回
            e.printStackTrace();
            LOGGER.error("持久化失败:{}", request);
            return WrittenResp.fail();
        }
    }

    @Override
    public ReadResp handleReadRequest(ReadReq request) {
        CacheValue cacheValue = stateMachine.get(request.getKey());
        return ReadResp.success(cacheValue);
    }

    @Override
    public VoteResp handleVoteRequest(VoteReq voteReq) {
        return null;
    }

    @Override
    public AppendLogResp handleLogRequest(AppendLogReq appendLogReq) {
        return null;
    }

    @Override
    public ApplyStateMachineResp handleApplyStateMachineRequest(ApplyStateMachineReq applyStateMachineReq) {
        return null;
    }

    @Override
    public CompareLogResp handleCompareLog(CompareLogReq compareLogReq) {
        return null;
    }


    /**
     * 获取状态机保存数据
     *
     * @return
     */
    @Override
    public RocksIterator newIterator() {
        return stateMachine.newIterator();
    }

    public static StandaloneNode standaloneNode = new StandaloneNode();

    public static StandaloneNode getInstance() {
        return standaloneNode;
    }
}
