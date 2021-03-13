package com.yuandragon.crispy.netty.handler;

import com.alibaba.fastjson.JSON;
import com.yuandragon.crispy.cache.bean.CacheValue;
import com.yuandragon.crispy.cache.core.CacheFacade;
import com.yuandragon.crispy.common.transport.Response;
import com.yuandragon.crispy.netty.http.NettyRequest;
import com.yuandragon.crispy.raft.enums.RequestCmd;
import com.yuandragon.crispy.raft.node.Node;
import com.yuandragon.crispy.raft.pojo.bean.Command;
import com.yuandragon.crispy.raft.pojo.bean.LogEntry;
import com.yuandragon.crispy.raft.pojo.req.AppendLogReq;
import com.yuandragon.crispy.raft.pojo.req.ApplyStateMachineReq;
import com.yuandragon.crispy.raft.pojo.req.CompareLogReq;
import com.yuandragon.crispy.raft.pojo.req.VoteReq;
import com.yuandragon.crispy.raft.pojo.resp.AppendLogResp;
import com.yuandragon.crispy.raft.pojo.resp.ApplyStateMachineResp;
import com.yuandragon.crispy.raft.pojo.resp.CompareLogResp;
import com.yuandragon.crispy.raft.pojo.resp.VoteResp;
import com.yuandragon.crispy.raft.strategy.ChooseNodeStrategy;

import java.util.Map;

/**
 * @Author: yuanzhanzhenxing
 */
public class RaftHandler implements Handler {


    /**
     * 缓存门面
     */
    private final CacheFacade cacheFacade = CacheFacade.getInstance();

    /**
     * 节点实现
     */
    private final Node node = ChooseNodeStrategy.getNode();

    /**
     * 节点实现
     */
    private final KeysHandler keysHandler = (KeysHandler) HandlerManager.getHandler(KeysHandler.SERVICE_CODE);

    /**
     * 服务代码
     */
    public static final String SERVICE_CODE = "/raft";

    @Override
    public String getServiceCode() {
        return SERVICE_CODE;
    }

    @Override
    public Response handle(NettyRequest request) {
        return doHandle(request);
    }

    private Response doHandle(NettyRequest request) {
        Map<String, Object> params = request.getParams();
        int cmd = Integer.parseInt(params.get("cmd").toString());
        String obj = params.get("obj").toString();
        // 选举请求
        if (cmd == RequestCmd.VOTE) {
            VoteReq voteReq = JSON.parseObject(obj, VoteReq.class);
            VoteResp voteResp = node.handleVoteRequest(voteReq);
            return Response.success(voteResp);
        } // 增加日志请求
        else if (cmd == RequestCmd.APPEND_LOG) {
            AppendLogReq appendLogReq = JSON.parseObject(obj, AppendLogReq.class);
            AppendLogResp appendLogResp = node.handleLogRequest(appendLogReq);
            return Response.success(appendLogResp);
        } // 写入请求
        else if (cmd == RequestCmd.WRITE) {
            return keysHandler.doHandleRedirect(request);
        } // 应用状态机请求
        else if (cmd == RequestCmd.APPLY_STATE_MACHINE) {
            ApplyStateMachineReq applyStateMachineReq = JSON.parseObject(obj, ApplyStateMachineReq.class);
            ApplyStateMachineResp applyStateMachineResp = node.handleApplyStateMachineRequest(applyStateMachineReq);
            if (applyStateMachineResp.isSuccess()) {
                // 添加到缓存中
                for (LogEntry entry : applyStateMachineResp.getEntries()) {
                    Command command = entry.getCommand();
                    if (command.getCommand() == 1) {
                        CacheValue cacheValue = cacheFacade.convertToCacheValue(command.getKey(), command.getValue());
                        cacheFacade.put(command.getKey(), cacheValue);
                    } else {
                        cacheFacade.delete(command.getKey());
                    }
                }
            }
            return Response.success(applyStateMachineResp);
        } // 日志比较请求
        else if (cmd == RequestCmd.COMPARE_LOG) {
            CompareLogReq compareLogReq = JSON.parseObject(obj, CompareLogReq.class);
            CompareLogResp compareLogResp = node.handleCompareLog(compareLogReq);
            return Response.success(compareLogResp);
        }

        return Response.success();
    }

}
