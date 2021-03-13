package com.yuandragon.crispy.raft.consistence;

import com.yuandragon.crispy.common.utils.CollectionUtil;
import com.yuandragon.crispy.common.utils.TimeUtil;
import com.yuandragon.crispy.raft.log.Log;
import com.yuandragon.crispy.raft.meta.Peer;
import com.yuandragon.crispy.raft.node.ClusterNode;
import com.yuandragon.crispy.raft.node.enums.NodeStatusEnum;
import com.yuandragon.crispy.raft.pojo.bean.Command;
import com.yuandragon.crispy.raft.pojo.bean.LogEntry;
import com.yuandragon.crispy.raft.pojo.bean.StateMachineLog;
import com.yuandragon.crispy.raft.pojo.req.AppendLogReq;
import com.yuandragon.crispy.raft.pojo.req.ApplyStateMachineReq;
import com.yuandragon.crispy.raft.pojo.req.VoteReq;
import com.yuandragon.crispy.raft.pojo.resp.AppendLogResp;
import com.yuandragon.crispy.raft.pojo.resp.ApplyStateMachineResp;
import com.yuandragon.crispy.raft.pojo.resp.VoteResp;
import org.apache.commons.lang3.StringUtils;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author: yuanzhanzhenxing
 */
public class DefaultConsensus implements Consensus {

    private static final Logger LOGGER = LoggerFactory.getLogger(Consensus.class);

    /**
     * 竞选锁
     */
    private ReentrantLock voteLock = new ReentrantLock();
    /**
     * 节点
     */
    private ClusterNode node;
    /**
     * 日志模块
     */
    private Log log;

    public DefaultConsensus(ClusterNode node) {
        this.node = node;
        this.log = node.getLog();
    }

    private volatile String logBatchNumber = "";
    private volatile List<LogEntry> entries;
    private volatile long createTime = 0;

    /**
     * 投票
     * @param voteReq
     * @return
     */
    @Override
    public VoteResp handleVoteRequest(VoteReq voteReq) {
        LOGGER.info("收到选举请求:{}", voteReq);
        try {
            // 获取锁失败则返回
            if (!voteLock.tryLock(3000, TimeUnit.SECONDS)) {
                return VoteResp.Builder.aVoteResp().voteGranted(false).build();
            }
            // 判断对方的 term 是否小于自己，如果小于自己，直接返回失败
            if (voteReq.getCurrentTerm() < node.getCurrentTerm()) {
                return VoteResp.Builder.aVoteResp().voteGranted(false).build();
            } else if (voteReq.getCurrentTerm() > node.getCurrentTerm()) {
                /*
                    存在这种情况，其他节点的数据没有该节点新，但是其他节点的任期比当前节点要大。
                    则会造成：由于其他节点的数据没有该节点新，所以后续的流程会失败，这时由于该节点记录的任期比较老，故而在竞选的定时任务中，
                    会发送比其他节点低或是相同的任期的竞选，由于所有节点在一个任期内只能投一张票，所以当其他节点比当前节点竞选更快时，
                    会造成一直选举不上的问题。
                 */
                // 当对方节点任期比自己大时，则更新新的任期
                node.setCurrentTerm(voteReq.getCurrentTerm());
                // 设置新任期内，没有投票给其他节点
                node.setVotedFor("");
            } else {
                // 判断在该任期是否已经投票
                if (StringUtils.isNotBlank(node.getVotedFor())) {
                    return VoteResp.Builder.aVoteResp().voteGranted(false).build();
                }
            }

            // 由于初次启动没有日志，需要判断是否为空
            if (log.getLastLog() != null) {
                // 对方节点任期大，则判断日志的大小
                if (voteReq.getLastLogIndex() < log.getLastLogIndex()) {
                    return VoteResp.Builder.aVoteResp().voteGranted(false).build();
                }
                // 比较日志大小
                if (voteReq.getLastLogTerm() < log.getLastLog().getTerm()) {
                    return VoteResp.Builder.aVoteResp().voteGranted(false).build();
                }
            }
            // 当一切条件都服务，则投给目标节点一票
            // 更新最后一次投票的任期
            node.setCurrentTerm(voteReq.getCurrentTerm());
            // 更新投票的节点
            node.setVotedFor(voteReq.getCandidateId());
            // 切换状态
            node.setNodeStatus(NodeStatusEnum.FOLLOWER);
            // 更新 leader 对象
            node.getPeersManager().setLeader(new Peer(voteReq.getCandidateId()));
            // 投了赞成票之后，则刷新本地的选举时间，因为投过赞成票，则说明请求节点日志比当前多或是相同，为了避免因为网络原因造成的多次选举，这里修改上次选举时间
            node.setPreElectionTime(TimeUtil.currentTimeMillis());
            return VoteResp.Builder.aVoteResp().voteGranted(true).build();
        } catch (InterruptedException e) {
            LOGGER.error("获取竞选锁失败：{}", e.getMessage());
            return VoteResp.Builder.aVoteResp().voteGranted(false).build();
        } finally {
            voteLock.unlock();
        }
    }

    /**
     * 接收日志
     *
     * @param appendLogReq
     * @return
     */
    @Override
    public synchronized AppendLogResp handleLogRequest(AppendLogReq appendLogReq) {
        if (appendLogReq.getEntries() != null) {
            LOGGER.info("接收到日志:{}", appendLogReq);
        } else {
            LOGGER.debug("接收到心跳:{}", appendLogReq);
        }
        // 获取当前时间
        long currentTimeMillis = TimeUtil.currentTimeMillis();

        // 判断任期大小
        if (appendLogReq.getCurrentTerm() < node.getCurrentTerm()) {
            return AppendLogResp.Builder.anAppendLogResp().success(false).build();
        } else if (appendLogReq.getCurrentTerm() == node.getCurrentTerm()) {
            // 从节点重启，这段时间并没有重新选举，这时两个节点任期相同，则判断从节点是否设置主节点信息，为空则设置
            if (node.getPeersManager().getLeader() == null) {
                // 更新 主节点信息
                node.getPeersManager().setLeader(new Peer(appendLogReq.getLeaderId()));
            }
        } else {
            // 更新 主节点信息
            node.getPeersManager().setLeader(new Peer(appendLogReq.getLeaderId()));
            // 更新 最新任期
            node.setCurrentTerm(appendLogReq.getCurrentTerm());
        }
        // 更新最新的心跳时间
        node.setPreHeartBeatTime(currentTimeMillis);
        // 设置节点状态
        node.setNodeStatus(NodeStatusEnum.FOLLOWER);

        // 获取日志内容
        List<LogEntry> entries = appendLogReq.getEntries();
        // 为空，则为心跳
        if (entries == null || entries.size() == 0) {
            // 返回信息
            return AppendLogResp.Builder.anAppendLogResp().success(true).build();
        }

        // 真实日志
        // 如果不为第一次写入日志，则判断任期是否匹配
        if (appendLogReq.getPrevLogIndex() != -1) {
            // 判断当前节点保存的日志是否为初始化日志，是的话，则需要主节点全量复制日志
            if (log.getLastLogIndex() == -1) {
                // 修眠一秒，等待同步请求先发出去，见SyncLog类
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return AppendLogResp.Builder.anAppendLogResp().success(false).build();
            } else {
                LogEntry logEntry = log.read(appendLogReq.getPrevLogIndex());
                if (logEntry != null) {
                    // 前一条日志的任期对不上，则说明该节点的上一条日志是错误的，需要让主节点递减日志，再次发送
                    if (logEntry.getTerm() != appendLogReq.getPreLogTerm()) {
                        return AppendLogResp.Builder.anAppendLogResp().success(false).build();
                    }
                } else {
                    // 为空则说明，该节点没有对应的上条日志，需要主节点递减日志序号，再次发送
                    return AppendLogResp.Builder.anAppendLogResp().success(false).build();
                }
            }
        }

        // 获取日志集合中的第一条日志
        LogEntry firstLogEntry = entries.get(0);
        // 获取本地日志
        LogEntry currentLogEntry = log.read(firstLogEntry.getIndex());
        // 存在日志保存成功，这是leader挂了，这时新选出的leader使用相同的index做插入
        if (currentLogEntry != null) {
            // 可能在leader向从节点批量同步的时候挂掉，然后重新选主了，如果任期不对，则删除从新增日志之后的所有日志
            if (firstLogEntry.getTerm() > currentLogEntry.getTerm()) {
                // 删除异常日志部分
                log.removeOnStartIndex(currentLogEntry.getIndex());
            } else if (firstLogEntry.getTerm() < currentLogEntry.getTerm()) {
                // 如果传过来的任期小于当前已保存日志的任期，则返回失败
                return AppendLogResp.Builder.anAppendLogResp().success(false).build();
            }
        }

        // 为空则新增
        for (LogEntry entry : entries) {
            try {
                log.write(entry);
            } catch (RocksDBException e) {
                e.printStackTrace();
                return AppendLogResp.Builder.anAppendLogResp().success(false).build();
            }
        }

        // 记录日志批次号
        this.logBatchNumber = appendLogReq.getLogBatchNumber();
        this.entries = entries;
        this.createTime = TimeUtil.currentTimeMillis();

        return AppendLogResp.Builder.anAppendLogResp().success(true).build();
    }

    /**
     * 应用到状态机
     *
     * @param applyStateMachineReq
     * @return
     */
    @Override
    public ApplyStateMachineResp handleApplyStateMachineRequest(ApplyStateMachineReq applyStateMachineReq) {
        ApplyStateMachineResp applyStateMachineResp = new ApplyStateMachineResp();
        applyStateMachineResp.setSuccess(false);
        // 保存快照
        List<LogEntry> entries = this.entries;
        // 如果不匹配
        if (!applyStateMachineReq.getLogBatchNumber().equals(this.logBatchNumber)) {
            return applyStateMachineResp;
        }
        // 应用过则清除
        this.logBatchNumber = null;
        this.entries = null;
        this.createTime = 0;
        // 一切正确，则应用到状态机
        for (LogEntry entry : entries) {
            Command command = entry.getCommand();
            StateMachineLog stateMachineLog = null;
            try {
                stateMachineLog = new StateMachineLog(command.getCommand(), command.getKey(), command.getValue());
                // 保存到状态机
                node.getStateMachine().apply(stateMachineLog);
            } catch (RocksDBException e) {
                LOGGER.error("主节点应用数据到状态机失败：{}", stateMachineLog);
                e.printStackTrace();
                return null;
            }
        }
        applyStateMachineResp.setEntries(entries);
        applyStateMachineResp.setSuccess(true);
        return applyStateMachineResp;
    }

    /**
     * 修正日志
     */
    @Override
    public synchronized void correctionLog() {
        // 保存快照
        List<LogEntry> entries = this.entries;
        // 判断上次创建的时间
        if (createTime == 0) {
            return;
        }
        // 判断是否数据
        if (CollectionUtil.isEmpty(entries)) {
            return;
        }
        // 获取当前时间
        long currentTimeMillis = TimeUtil.currentTimeMillis();
        // 如果间隔小于3秒则返回，因为存在触发选举时，正好其他节点选举成功，并且发送了日志，这时保存的日志是正确的，则不操作
        if (currentTimeMillis - createTime < 3000) {
            return;
        }

        // 清除数据
        log.removeOnStartIndex(entries.get(0).getIndex());
        // 清除记录
        this.logBatchNumber = null;
        this.entries = null;
        this.createTime = 0;
    }

}
