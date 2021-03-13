package com.yuandragon.crispy.raft.consistence;


import com.yuandragon.crispy.raft.pojo.req.AppendLogReq;
import com.yuandragon.crispy.raft.pojo.req.ApplyStateMachineReq;
import com.yuandragon.crispy.raft.pojo.req.VoteReq;
import com.yuandragon.crispy.raft.pojo.resp.AppendLogResp;
import com.yuandragon.crispy.raft.pojo.resp.ApplyStateMachineResp;
import com.yuandragon.crispy.raft.pojo.resp.VoteResp;

/**
 * @Author: yuanzhanzhenxing
 */
public interface Consensus {

    /**
     * 投票
     *
     * @param voteReq
     * @return
     */
    VoteResp handleVoteRequest(VoteReq voteReq);

    /**
     * 追加日志
     *
     * @param appendLogReq
     * @return
     */
    AppendLogResp handleLogRequest(AppendLogReq appendLogReq);

    /**
     * 通过日志批次id应用日志到状态机
     *
     * @param applyStateMachineReq
     * @return
     */
    ApplyStateMachineResp handleApplyStateMachineRequest(ApplyStateMachineReq applyStateMachineReq);

    /**
     * 修正日志
     */
    void correctionLog();
}
