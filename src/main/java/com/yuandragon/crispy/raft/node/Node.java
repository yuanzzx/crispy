package com.yuandragon.crispy.raft.node;

import com.yuandragon.crispy.raft.pojo.req.*;
import com.yuandragon.crispy.raft.pojo.resp.*;
import org.rocksdb.RocksIterator;

/**
 * @Author: yuanzhanzhenxing
 */
public interface Node {


    /**
     * 处理写入请求
     *
     * @param request
     * @return
     */
    WrittenResp handleWrittenRequest(WrittenReq request);

    /**
     * 处理读请求
     *
     * @param request
     * @return
     */
    ReadResp handleReadRequest(ReadReq request);

    /**
     * 处理选举请求
     *
     * @param voteReq
     * @return
     */
    VoteResp handleVoteRequest(VoteReq voteReq);

    /**
     * 处理日志请求
     *
     * @param appendLogReq
     * @return
     */
    AppendLogResp handleLogRequest(AppendLogReq appendLogReq);

    /**
     * 处理应用到状态机请求
     *
     * @param applyStateMachineReq
     * @return
     */
    ApplyStateMachineResp handleApplyStateMachineRequest(ApplyStateMachineReq applyStateMachineReq);

    /**
     * 处理日志比较
     *
     * @param compareLogReq
     * @return
     */
    CompareLogResp handleCompareLog(CompareLogReq compareLogReq);

    /**
     * 获取状态机的迭代器
     *
     * @return
     */
    RocksIterator newIterator();
}
