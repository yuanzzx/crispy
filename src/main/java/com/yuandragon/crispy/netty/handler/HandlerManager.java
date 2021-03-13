package com.yuandragon.crispy.netty.handler;

import com.yuandragon.crispy.common.contans.RestStatus;
import com.yuandragon.crispy.common.setting.CrispySettingConfig;
import com.yuandragon.crispy.common.transport.Response;
import com.yuandragon.crispy.netty.http.NettyChannel;
import com.yuandragon.crispy.netty.http.NettyRequest;
import io.netty.handler.codec.http.HttpMethod;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: yuanzhanzhenxing
 */
public class HandlerManager {


    private final static Map<String, Handler> HANDLER_MAP = new HashMap<>();

    static {
        registHandler(new KeysHandler());
        registHandler(new SystemHandler());
        // 集群模式，才加载raft模块
        if (CrispySettingConfig.NODE_MODEL.equals(CrispySettingConfig.CLUSTER)) {
            registHandler(new RaftHandler());
        }
    }

    /**
     * 转发给处理器
     *
     * @param nettyRequest
     * @param nettyChannel
     */
    public void handlerDispatcher(NettyRequest nettyRequest, NettyChannel nettyChannel) {
        // 获取处理服务
        String serviceCode = nettyRequest.getUri();
        // 如果为get请求
        if (nettyRequest.getMethod().equals(HttpMethod.GET)) {
            int pathEndPos = serviceCode.indexOf('?');
            if (pathEndPos > -1) {
                serviceCode = serviceCode.substring(0, pathEndPos);
            }
        }

        // 集群才判断，判断服务是否开启，存在同步的情况，数据未完成同步，则不开启服务
        if (CrispySettingConfig.NODE_MODEL.equals(CrispySettingConfig.CLUSTER) && isClose()) {
            // 刚启动只支持raft的请求
            if (!serviceCode.equals(RaftHandler.SERVICE_CODE)) {
                nettyChannel.sendResponseAndClose(new Response<>(RestStatus.UNAUTHORIZED, "synchronizing log"));
            }
        }

        // 获取服务处理器
        Handler handler = HANDLER_MAP.get(serviceCode);
        if (handler == null) {
            nettyChannel.sendResponseAndClose(new Response<>(RestStatus.BAD_REQUEST, "request not found"));
            return;
        }
        Response response = handler.handle(nettyRequest);
        nettyChannel.sendResponseAndClose(response);
    }

    /**
     * 服务是否为关闭状态
     *
     * @return
     */
    private boolean isClose() {
        return CrispySettingConfig.SERVICE_SWITCH == CrispySettingConfig.OFF;
    }

    /**
     * 新增处理器
     *
     * @param handler
     */
    public static void registHandler(Handler handler) {
        HANDLER_MAP.put(handler.getServiceCode(), handler);
    }

    /**
     * 获取处理器
     *
     * @param serviceCode
     */
    public static Handler getHandler(String serviceCode) {
        return HANDLER_MAP.get(serviceCode);
    }

    private final static HandlerManager handlerManager = new HandlerManager();

    public static HandlerManager getInstance() {
        return handlerManager;
    }

    private HandlerManager() {

    }
}
