package com.yuandragon.crispy.raft.node;

import com.yuandragon.crispy.cache.bean.CacheValue;
import com.yuandragon.crispy.common.id.Sequence;
import com.yuandragon.crispy.common.setting.CrispySettingConfig;
import com.yuandragon.crispy.common.transport.Request;
import com.yuandragon.crispy.common.transport.Response;
import com.yuandragon.crispy.common.utils.TimeUtil;
import com.yuandragon.crispy.raft.consistence.Consensus;
import com.yuandragon.crispy.raft.consistence.DefaultConsensus;
import com.yuandragon.crispy.raft.enums.RequestCmd;
import com.yuandragon.crispy.raft.log.DefaultLog;
import com.yuandragon.crispy.raft.log.Log;
import com.yuandragon.crispy.raft.meta.Peer;
import com.yuandragon.crispy.raft.meta.PeersManager;
import com.yuandragon.crispy.raft.node.enums.NodeStatusEnum;
import com.yuandragon.crispy.raft.pojo.bean.Command;
import com.yuandragon.crispy.raft.pojo.bean.LogEntry;
import com.yuandragon.crispy.raft.pojo.bean.StateMachineLog;
import com.yuandragon.crispy.raft.pojo.req.*;
import com.yuandragon.crispy.raft.pojo.resp.*;
import com.yuandragon.crispy.raft.rpc.HttpTransport;
import com.yuandragon.crispy.raft.rpc.TcpTransport;
import com.yuandragon.crispy.raft.stateMachine.DefaultStateMachine;
import com.yuandragon.crispy.raft.stateMachine.StateMachine;
import com.yuandragon.crispy.raft.threadPool.RaftThreadPoolExecutor;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * @Author: yuanzhanzhenxing
 */
public class ClusterNode implements Node {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterNode.class);

    private static final ClusterNode clusterNode = new ClusterNode();

    public static ClusterNode getInstance() {
        return clusterNode;
    }

    /**
     * 节点所知道到的最后一个任期
     */
    private volatile long currentTerm;

    /**
     * 在当前获得选票的候选人的 Id
     */
    private volatile String votedFor;

    /**
     * 日志条目集；每一个条目包含一个用户状态机执行的指令，和收到时的任期号
     */
    private Log log;

    /**
     * 当前节点角色 默认为 follower
     */
    private volatile int NODE_STATUS = NodeStatusEnum.FOLLOWER;

    /**
     * 上一次选举时间 时间当前时间
     */
    private volatile long preElectionTime = TimeUtil.currentTimeMillis();

    /**
     * 心跳时间戳 主节点最后一次发来的心跳
     */
    private volatile long preHeartBeatTime = 0;

    /**
     * 节点信息
     */
    private PeersManager peersManager = PeersManager.getInstance();

    /**
     * 数据传输
     */
    private static TcpTransport TRANSPORT = new HttpTransport();
    ;

    /**
     * 状态机
     */
    private StateMachine stateMachine;

    /**
     * 一致性模块
     */
    private Consensus consensus;

    /**
     * 处理化节点
     */
    public ClusterNode() {
        LOGGER.info("init cluster node");
        // 日志模块
        this.log = DefaultLog.getInstance();
        // 获取最后一个日志
        LogEntry lastLogEntry = log.getLastLog();
        // 设置任期
        currentTerm = lastLogEntry.getTerm();
        // 状态机
        this.stateMachine = DefaultStateMachine.getInstance();
        // 设置一致性模块
        this.consensus = new DefaultConsensus(this);
        // 校验是否需要同步日志
        RaftThreadPoolExecutor.execute(new SyncLog());
        // 设置心跳定时任务
        RaftThreadPoolExecutor.scheduleWithFixedDelay(new Heartbeat(), 3000);
        // 设置选举定时任务
        RaftThreadPoolExecutor.scheduleAtFixedRate(new ElectionTask(), ThreadLocalRandom.current().nextInt(5000) + 8000, 500);
    }

    /**
     * 处理写入请求
     * 1:区分请求
     * 1.1:get请求 可以自己处理
     * 1.2:其余请求，则转发给leader节点进行处理，并返回
     * 2:如果当前为leader节点
     * 2.1:Leader 先将数据写在本地日志，这时候数据还是 Uncommitted
     * 2.2:Leader 给所有的 Follower 节点发送 AppendEntries 请求
     * 2.2.1:没有取得大部分 Follower 节点的成功响应，则删除本地的日志，并结束
     * 2.2.2:得大部分 Follower 节点的成功响应，则应用到状态机，并在次向节点发送 AppendEntries 请求。end
     *
     * @param request
     * @return 根据返回结果，操作缓存
     */
    @Override
    public WrittenResp handleWrittenRequest(WrittenReq request) {
        // 如果是读，则直接从缓存层面返回，这里只做写请求操作
        // 判断当前状态, 如果不是主节点则发送给主节点操作
        if (NodeStatusEnum.LEADER != NODE_STATUS) {
            return redirect(request);
        }
        LOGGER.info("接收到写请求:{}", request);

        // 加锁
        synchronized (this) {
            // 如果是写操作， 生成日志
            LogEntry logEntry = getLogEntry(request);
            try {
                // 预提交到本地日志
                log.write(logEntry);
            } catch (RocksDBException e) {
                // 异常则返回
                e.printStackTrace();
                LOGGER.error("日志保存出错:{}", logEntry);
                return WrittenResp.fail();
            }

            // 并行向其他节点发起复制
            if (parallelReplication(logEntry)) {
                return WrittenResp.success();
            } else {
                // 回滚已经提交的日志.
                log.removeOnStartIndex(logEntry.getIndex());
                return WrittenResp.fail();
            }
        }
    }

    /**
     * 处理读请求
     *
     * @param request
     * @return
     */
    @Override
    public ReadResp handleReadRequest(ReadReq request) {
        CacheValue cacheValue = stateMachine.get(request.getKey());
        return ReadResp.success(cacheValue);
    }

    /**
     * 处理选举请求
     *
     * @param voteReq
     * @return
     */
    @Override
    public VoteResp handleVoteRequest(VoteReq voteReq) {
        VoteResp voteResp = consensus.handleVoteRequest(voteReq);
        voteResp.setTerm(currentTerm);
        LOGGER.info("选举请求结果：{}", voteResp);
        return voteResp;
    }

    /**
     * 处理日志请求
     *
     * @param appendLogReq
     * @return
     */
    @Override
    public AppendLogResp handleLogRequest(AppendLogReq appendLogReq) {
        AppendLogResp appendLogResp = consensus.handleLogRequest(appendLogReq);
        appendLogResp.setTerm(currentTerm);
        return appendLogResp;
    }

    /**
     * 处理应用到状态机请求
     *
     * @param applyStateMachineReq
     * @return
     */
    @Override
    public ApplyStateMachineResp handleApplyStateMachineRequest(ApplyStateMachineReq applyStateMachineReq) {
        ApplyStateMachineResp applyStateMachineResp = consensus.handleApplyStateMachineRequest(applyStateMachineReq);
        return applyStateMachineResp;
    }

    /**
     * 日志比较
     *
     * @param compareLogReq
     * @return
     */
    @Override
    public CompareLogResp handleCompareLog(CompareLogReq compareLogReq) {
        // 获取请求方信息
        Peer requestingPartyPeer = null;
        // 根据地址，获取节点信息
        for (Peer peer : peersManager.getPeers()) {
            if (peer.getRawAddress().equals(compareLogReq.getServerAddress())) {
                requestingPartyPeer = peer;
            }
        }
        // 获取锁，避免同步冲突
        if (requestingPartyPeer == null || requestingPartyPeer.isLocked()) {
            return new CompareLogResp(true);
        }

        LogEntry requestingPartyLogEntry = compareLogReq.getLogEntry();
        // 获取最后一条日志的索引
        LogEntry localLastLogEntry = log.getLastLog();
        // 如果两个索引相同，任期也相同，则不需要同步
        if (requestingPartyLogEntry.getIndex().equals(localLastLogEntry.getIndex()) && requestingPartyLogEntry.getTerm() == localLastLogEntry.getTerm()) {
            return new CompareLogResp(false);
        }
        // 不相同则获取请求方日志的上一条日志，存在从节点一条日志都没有的情况
        LogEntry preLogEntry;
        if (requestingPartyLogEntry.getIndex() == -1) {
            preLogEntry = log.read(requestingPartyLogEntry.getIndex());
        } else {
            preLogEntry = log.read(requestingPartyLogEntry.getIndex() - 1);
        }
        // 如果上一条日志不存在，则说明请求方数据有问题，则全量同步
        if (preLogEntry == null) {
            preLogEntry = log.read(-1L);
        }
        // 获取前一个日志后的所有日志不包括最后一条，最后一条可能会被删除
        List<LogEntry> logEntries = log.getOnStartIndex(preLogEntry.getIndex() + 1);
        // 同步日志
        syncLogEntry(requestingPartyPeer, preLogEntry, logEntries);
        return new CompareLogResp(true);
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

    /**
     * 并行复制日志
     *
     * @param logEntry
     * @return
     */
    private boolean parallelReplication(LogEntry logEntry) {
        // 获取一个批次id
        String logBatchNumber = Sequence.createId();
        // 日志集合
        List<LogEntry> logEntries = new ArrayList<>();
        logEntries.add(logEntry);
        // 执行并行复制
        return doParallelReplication(logBatchNumber, logEntries);
    }

    /**
     * 并行复制
     *
     * @param logBatchNumber
     * @param logEntries
     */
    public boolean doParallelReplication(String logBatchNumber, List<LogEntry> logEntries) {
        // 判断当前同步的节点是否超过一半，超过则直接返回失败
        if (peersManager.getPassesQuantity() < peersManager.getSyncPeersSize()) {
            return false;
        }
        // 获取第一个日志
        LogEntry firstLogEntry = logEntries.get(0);
        // 获取当前日志的上一个日志
        long lastLogIndex = firstLogEntry.getIndex() - 1;
        // 获取上一个日志信息
        LogEntry preLogEntry = log.read(lastLogIndex);
        // 获取当前任期
        long currentTerm = this.currentTerm;
        // 记录响应值
        List<ReplicationResp> replicationResps = new ArrayList<>();
        // 并发向从节点发送请求
        for (Peer peer : peersManager.getPeers()) {
            // 并行发起 RPC 复制.
            replicationResps.add(replication(peer, logBatchNumber, currentTerm, preLogEntry, logEntries));
        }

        // 同步器，数量设置为一半
        CountDownLatch latch = new CountDownLatch(peersManager.getPassesQuantity());
        // 获取结果
        for (ReplicationResp replicationResp : replicationResps) {
            RaftThreadPoolExecutor.execute(() -> {
                try {
                    // 判断是否成功
                    if (replicationResp.getResult().get(1000, MILLISECONDS)) {
                        // 成功同步器加一
                        latch.countDown();
                    }
                } catch (CancellationException | TimeoutException | ExecutionException | InterruptedException e) {
                    // 出现异常则不记录
                    e.printStackTrace();
                    LOGGER.error("向节点：{}，同步超时", replicationResp.getPeer().getRawAddress());
                }
            });
        }

        try {
            // 等待结果
            if (!latch.await(1500, MILLISECONDS)) {
                // 被打断，则表示超时
                LOGGER.error("未取得多数节点通过，日志批次编号：{}，日志内容：{}", logBatchNumber, logEntries);
                return false;
            }
        } catch (InterruptedException e) {
            // 被打断，则表示超时
            LOGGER.error("向其他节点同步消息超时，日志批次编号：{}，日志内容：{}", logBatchNumber, logEntries);
            e.printStackTrace();
            return false;
        }

        // 判断当前节点状态
        if (NODE_STATUS == NodeStatusEnum.FOLLOWER) {
            return false;
        }

        // 批量引用到状态机
        for (LogEntry logEntry : logEntries) {
            Command command = logEntry.getCommand();
            // 应用到状态机
            StateMachineLog stateMachineLog = null;
            try {
                stateMachineLog = new StateMachineLog(command.getCommand(), command.getKey(), command.getValue());
                stateMachine.apply(stateMachineLog);
            } catch (RocksDBException e) {
                LOGGER.error("主节点应用数据到状态机失败：{}", stateMachineLog);
                e.printStackTrace();
                return false;
            }
        }
        // 通知从节点应用到状态机
        for (Peer peer : peersManager.getPeers()) {
            // 应用到状态机
            applyStateMachine(peer, logBatchNumber);
        }
        return true;
    }

    /**
     * 应用到状态机
     *
     * @param peer
     * @param logBatchNumber
     */
    private void applyStateMachine(Peer peer, String logBatchNumber) {
        RaftThreadPoolExecutor.execute(() -> {
            ApplyStateMachineReq applyStateMachineReq = new ApplyStateMachineReq();
            applyStateMachineReq.setLogBatchNumber(logBatchNumber);
            Request request = new Request(RequestCmd.APPLY_STATE_MACHINE, applyStateMachineReq, peer.getRaftAddress());
            TRANSPORT.sendMessage(request, ApplyStateMachineResp.class);
        });
    }

    /**
     * 将请求参数转换日志格式
     *
     * @param request
     * @return
     */
    private LogEntry getLogEntry(WrittenReq request) {
        return LogEntry.LogEntryBuilder.aLogEntry()
                .withTerm(currentTerm)
                .withCommand(Command.CommandBuilder.aCommand()
                        .withCommand(request.getCommand())
                        .withKey(request.getKey())
                        .withValue(request.getValue())
                        .build())
                .build();
    }

    /**
     * 复制给指定节点
     *
     * @param peer
     * @param logBatchNumber
     * @param preLogEntry
     * @param logEntries
     * @return
     */
    private ReplicationResp replication(Peer peer, String logBatchNumber, long term, LogEntry preLogEntry, List<LogEntry> logEntries) {
        // 返回值
        ReplicationResp replicationResp = new ReplicationResp();
        // 节点信息
        replicationResp.setPeer(peer);

        // 获取执行结果
        Future<Boolean> result = RaftThreadPoolExecutor.submit(() -> {
            // 请求体
            AppendLogReq appendLogReq = new AppendLogReq();
            // 当前任期
            appendLogReq.setCurrentTerm(term);
            // 日志批次号
            appendLogReq.setLogBatchNumber(logBatchNumber);
            // 主节点地址
            appendLogReq.setLeaderId(peersManager.getSelf().getRawAddress());
            // 上一个日志的任期
            appendLogReq.setPreLogTerm(preLogEntry.getTerm());
            // 上一个日志的索引位
            appendLogReq.setPrevLogIndex(preLogEntry.getIndex());
            // 日志主体
            appendLogReq.setEntries(logEntries);

            Request request = new Request(RequestCmd.APPEND_LOG, appendLogReq, peer.getRaftAddress());
            // 发送请求
            Response<AppendLogResp> response = TRANSPORT.sendMessage(request, AppendLogResp.class);
            if (response.isFail() || response.getData() == null) {
                return false;
            }
            AppendLogResp appendLogResp = response.getData();
            // 响应成功则返回
            if (appendLogResp.isSuccess()) {
                return true;
            }

            // 对方节点任期比当前节点任期大，避免多个节点返回的都比当前的大，前一个已经修改了任期，后一个任期却相同，造成以为是
            // 数据错误的原因，造成数据同步，需要在这里做判断
            if (appendLogResp.getTerm() > term) {
                // 修改节点状态
                NODE_STATUS = NodeStatusEnum.FOLLOWER;
                // 判断是否比当前任期大，大则替换
                if (appendLogResp.getTerm() > currentTerm) {
                    // 更新任期
                    currentTerm = appendLogResp.getTerm();
                }
            } else {
                // 对方节点任期并不比当前节点大，却失败了，则说明：对方节点的最后一条日志不匹配，则减小索引重试，完成日志同步
                syncLogEntry(peer, preLogEntry, logEntries);
            }
            return false;
        });
        replicationResp.setResult(result);
        return replicationResp;
    }

    /**
     * 同步日志
     *
     * @param peer
     * @param preLogEntry
     * @param logEntries
     */
    private void syncLogEntry(Peer peer, LogEntry preLogEntry, List<LogEntry> logEntries) {
        // 新开线程同步
        RaftThreadPoolExecutor.execute(() -> {
            // 获取节点的锁
            if (!peer.getLock()) {
                // 失败则表示有在做同步操作，直接返回
                return;
            }
            // 加入到同步日志的集合中
            peersManager.addSyncPeer(peer);

            try {
                LogEntry headLogEntry = preLogEntry;
                // 保存日志主体
                LinkedList<LogEntry> logEntryList = new LinkedList<>(logEntries);

                // 从第一个日志开始往前推
                while (true) {
                    // 判断当前节点状态，如果存在回复任期比自己的节点，则会修改该节点的状态，所以需要判断节点状态
                    if (NODE_STATUS == NodeStatusEnum.FOLLOWER) {
                        return;
                    }
                    // 获取日志批次编号
                    String logBatchNumber = Sequence.createId();

                    // 复制日志
                    ReplicationResp replicationResp = replication(peer, logBatchNumber, currentTerm, headLogEntry, logEntryList);
                    // 获取结果
                    Future<Boolean> result = replicationResp.getResult();
                    try {
                        // 等待30秒 如果成功
                        if (result.get(30000, MILLISECONDS)) {
                            // 提醒从应用到状态机
                            //replication(peer, logBatchNumber, currentTerm, preLog, logEntryList);
                            applyStateMachine(peer, logBatchNumber);
                            // 结束同步，并从同步集合中删除
                            peersManager.removeSyncPeer(peer);
                            return;
                        }
                        // 失败则递减索引值重试, 判断节点的索引是否为初始化节点，如果从初始化节点都无法同步成功，则不同步
                        if (headLogEntry.getIndex() == -1) {
                            LOGGER.error("节点:【{}】无法同步", peer.getRawAddress());
                            break;
                        }
                        // 不等于初始化节点，将前一个节点加入到同步数据中
                        logEntryList.addFirst(headLogEntry);
                        // 获取头日志的上一条
                        headLogEntry = log.read(headLogEntry.getIndex() - 1);
                    } catch (InterruptedException | ExecutionException | TimeoutException e) {
                        e.printStackTrace();
                        LOGGER.info("数据同步失败，节点：{}", peer);
                        try {
                            // 30秒还未响应，则休眠10秒在尝试同步
                            Thread.sleep(10000);
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                    }
                }
            } finally {
                // 解锁
                peer.unLock();
            }
        });
    }

    /**
     * 转发给leader
     *
     * @param writtenReq
     * @return
     */
    public WrittenResp redirect(WrittenReq writtenReq) {
        Request request = new Request(RequestCmd.WRITE, writtenReq, peersManager.getLeader().getRaftAddress());
        Response<WrittenResp> response = TRANSPORT.sendMessage(request, WrittenResp.class);
        if (response.isFail()) {
            return WrittenResp.fail();
        }
        return response.getData();
    }

    /**
     * 选举
     */
    class ElectionTask implements Runnable {

        @Override
        public void run() {
            // 如果为主节点，则跳过
            if (NODE_STATUS == NodeStatusEnum.LEADER) {
                return;
            }
            // 获取当前时间
            long current = TimeUtil.currentTimeMillis();
            // 上一次心跳距当前时间是否已经超过心跳超时时间
            if (current - preHeartBeatTime < CrispySettingConfig.HEARTBEAT_TIMEOUT_THRESHOLD) {
                return;
            }
            // 当前时间 - 上次选举时间 < 选举间隔
            if (current - preElectionTime < CrispySettingConfig.ELECTION_INTERVAL_TIME) {
                return;
            }

            // 更新节点状态为候选者
            NODE_STATUS = NodeStatusEnum.CANDIDATE;
            // 更新上一次选举时间点
            preElectionTime = current;
            // 将任期加一
            currentTerm = currentTerm + 1;
            // 推荐自己.
            votedFor = peersManager.getSelf().getRawAddress();
            // 修正日志
            consensus.correctionLog();
            LOGGER.info("开始选举，当前时间：{}，上次心跳时间:{}，当前任期:{}", current, preHeartBeatTime, currentTerm);

            // 获取除了自己的所有节点
            List<Peer> peers = peersManager.getPeers();
            // 从日志数据库中获取最后一个日志的任期，初识一定会有一条记录
            long lastTerm = log.getLastLog().getTerm();
            // 发送请求 并发执行
            List<Future<Response<VoteResp>>> futureArrayList = peers.stream().map(peer -> RaftThreadPoolExecutor.submit(() -> {
                // 请求投票参数
                VoteReq voteReq = new VoteReq();
                voteReq.setCurrentTerm(currentTerm);
                voteReq.setCandidateId(peersManager.getSelf().getRawAddress());
                voteReq.setLastLogIndex(log.getLastLogIndex());
                voteReq.setLastLogTerm(lastTerm);
                // 发送投票请求
                Request request = new Request(RequestCmd.VOTE, voteReq, peer.getRaftAddress());
                return TRANSPORT.sendMessage(request, VoteResp.class);
            })).collect(Collectors.toList());

            // 并发获取，所以需要一个同步器
            CountDownLatch latch = new CountDownLatch(peersManager.getPassesQuantity());
            // 并发等待结果
            for (Future<Response<VoteResp>> responseFuture : futureArrayList) {
                RaftThreadPoolExecutor.execute(() -> {
                    try {
                        Response<VoteResp> response = responseFuture.get(1000, MILLISECONDS);
                        if (response.isFail()) {
                            return;
                        }
                        VoteResp voteResp = response.getData();
                        // 获取选举结果
                        if (voteResp.getVoteGranted()) {
                            // 成功则操作同步器
                            latch.countDown();
                        } else {
                            // 更新自己的任期.
                            long resTerm = voteResp.getTerm();
                            if (resTerm > currentTerm) {
                                currentTerm = resTerm;
                                LOGGER.info("目标节点任期({})比当前节点任期大，修改当前任期", currentTerm);
                            }
                        }
                    } catch (InterruptedException | ExecutionException | TimeoutException e) {
                        //LOGGER.error("节点超时");
                    }
                });
            }

            try {
                // 等待结果的获取
                if (!latch.await(1500, MILLISECONDS)) {
                    // 票数不足 重新选举
                    votedFor = "";
                    LOGGER.info("选举失败，未取得大多数节点通过，当前任期：{}", currentTerm);
                    return;
                }
            } catch (InterruptedException e) {
                // 票数不足 重新选举
                votedFor = "";
                LOGGER.info("选举失败，等待超过，当前任期：{}", currentTerm);
                return;
            }

            // 投票期间，可能接受了其他服务发生的选举，有可能状态以及变为 follower，这时选举需要停止
            if (NODE_STATUS == NodeStatusEnum.FOLLOWER) {
                LOGGER.info("选举中止，因为当前节点转变为follower");
                return;
            }

            // 票数足够，则当选
            NODE_STATUS = NodeStatusEnum.LEADER;
            peersManager.setLeader(peersManager.getSelf());
            // 重置上次心跳时间
            preHeartBeatTime = 0;
            LOGGER.info("选举成功，当前任期：{}", currentTerm);
            // 发送心跳
            RaftThreadPoolExecutor.execute(new Heartbeat());
        }
    }

    /**
     * 心跳
     */
    class Heartbeat implements Runnable {

        @Override
        public void run() {
            // 判断是否为主，只有主才需要向其他机器发送心跳
            if (NODE_STATUS != NodeStatusEnum.LEADER) {
                return;
            }
            // 获取当前时间
            long current = TimeUtil.currentTimeMillis();
            LOGGER.debug("开始发送心跳，当前时间：{},当前任期:{}", current, currentTerm);

            // 如果是主，则需要给所有节点发送心跳
            List<Peer> peers = peersManager.getPeers();
            // 发送心跳
            for (Peer peer : peers) {
                // 心跳,空日志.
                AppendLogReq appendLogReq = new AppendLogReq();
                appendLogReq.setLeaderId(peersManager.getSelf().getRawAddress());
                appendLogReq.setServerAddress(peer.getAddress());
                appendLogReq.setCurrentTerm(currentTerm);

                // 请求体
                Request request = new Request(RequestCmd.APPEND_LOG, appendLogReq, peer.getRaftAddress());
                // 向所有从机器发送心跳，并发执行
                RaftThreadPoolExecutor.execute(() -> {
                    // 发送心跳
                    Response<AppendLogResp> response = TRANSPORT.sendMessage(request, AppendLogResp.class);
                    if (response.isFail()) {
                        return;
                    }
                    // 返回值
                    AppendLogResp appendLogResp = response.getData();
                    // 获取心跳返回的任期
                    long term = appendLogResp.getTerm();
                    // 如果返回的任期大于当前任期
                    if (term > currentTerm) {
                        // 更新当前任期
                        currentTerm = term;
                        // 候选人设置空
                        votedFor = "";
                        // 更新节点为从节点
                        NODE_STATUS = NodeStatusEnum.FOLLOWER;
                    }
                }, false);
            }
        }
    }

    /**
     * 存在主节点时，发送当前节点最后一个日志的id给主，主校验日志并返回相差的日志，由这里同步完成，才可对外提供服务
     */
    class SyncLog implements Runnable {

        @Override
        public void run() {
            // 循环获取
            while (true) {
                // 获取当前是否存在主节点
                Peer leader = peersManager.getLeader();
                if (leader == null || preHeartBeatTime == 0) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }
                // 判断当前节点是否为主节点
                if (peersManager.getSelf().getRawAddress().equals(leader.getRawAddress())) {
                    // 结束，并开启服务
                    CrispySettingConfig.SERVICE_SWITCH = CrispySettingConfig.ON;
                    break;
                }

                LOGGER.info("开始发送同步数据请求，当前主节点：{}", leader.getRawAddress());
                // 获取当前节点最后一个日志
                CompareLogReq compareLogReq = new CompareLogReq();
                compareLogReq.setLogEntry(log.getLastLog());
                compareLogReq.setServerAddress(peersManager.getSelf().getRawAddress());

                // 发送给主节点
                Request request = new Request(RequestCmd.COMPARE_LOG, compareLogReq, leader.getRaftAddress());
                Response<CompareLogResp> response = TRANSPORT.sendMessage(request, CompareLogResp.class);

                // 返回值如果为空，则休眠10秒
                if (response.isFail() || response.getData() == null) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }

                // 判断返回值确定是否开启
                CompareLogResp compareLogResp = response.getData();
                // 无需同步则开启服务
                if (!compareLogResp.getSyncFlag()) {
                    // 开启服务
                    CrispySettingConfig.SERVICE_SWITCH = CrispySettingConfig.ON;
                    LOGGER.info("数据一致，无需同步，启动对外服务");
                } else {
                    LOGGER.info("数据不一致，等待主节点同步数据");
                    try {
                        // 在同步中，过10秒再次查询
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }
                break;
            }
        }
    }

    public long getCurrentTerm() {
        return currentTerm;
    }

    public Log getLog() {
        return log;
    }

    public void setCurrentTerm(long currentTerm) {
        this.currentTerm = currentTerm;
    }

    public String getVotedFor() {
        return votedFor;
    }

    public void setVotedFor(String votedFor) {
        this.votedFor = votedFor;
    }

    public PeersManager getPeersManager() {
        return peersManager;
    }

    public void setNodeStatus(int NODE_STATUS) {
        this.NODE_STATUS = NODE_STATUS;
    }

    public void setPreHeartBeatTime(long preHeartBeatTime) {
        this.preHeartBeatTime = preHeartBeatTime;
    }

    public StateMachine getStateMachine() {
        return stateMachine;
    }

    public void setPreElectionTime(long preElectionTime) {
        this.preElectionTime = preElectionTime;
    }
}
