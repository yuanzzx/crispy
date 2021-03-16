package com.yuandragon.crispy.netty.handler;

import com.alibaba.fastjson.JSONObject;
import com.yuandragon.crispy.common.contans.RestStatus;
import com.yuandragon.crispy.common.setting.CrispySettingConfig;
import com.yuandragon.crispy.common.transport.Response;
import com.yuandragon.crispy.netty.http.NettyChannel;
import com.yuandragon.crispy.netty.http.NettyRequest;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.MemoryAttribute;
import io.netty.util.CharsetUtil;

import java.util.HashMap;
import java.util.List;
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
     * 处理转发
     *
     * @param channelHandlerContext
     * @param request
     */
    public void handlerDispatcher(ChannelHandlerContext channelHandlerContext, FullHttpRequest request) {
        NettyChannel nettyChannel = new NettyChannel(channelHandlerContext);
        try {
            // 返回失败
            if (request.decoderResult().isFailure()) {
                nettyChannel.sendResponseAndClose(new Response<>(RestStatus.INTERNAL_SERVER_ERROR, "request not found"));
            }

            // 获取处理服务
            String serviceCode = request.uri();
            // 如果为get请求
            if (request.method().equals(HttpMethod.GET)) {
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
            // 获取请求参数
            Map<String, Object> params = getParams(nettyChannel, request);
            // 包装
            NettyRequest nettyRequest = new NettyRequest(request, params);
            // 交给处理器处理
            Response response = handler.handle(nettyRequest);
            nettyChannel.sendResponseAndClose(response);
        } catch (Exception e) {
            e.printStackTrace();
            nettyChannel.sendResponseAndClose(new Response<>(RestStatus.INTERNAL_SERVER_ERROR, "server error"));
        }
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

    /**
     * 获取请求参数
     *
     * @param nettyChannel
     * @param request
     * @return
     */
    private Map<String, Object> getParams(NettyChannel nettyChannel, FullHttpRequest request) {
        Map<String, Object> params = new HashMap<>();
        if (request.method() == HttpMethod.GET) {
            getUrlParamsFromChannel(request, params);
        } else if (request.method() == HttpMethod.POST) {
            getBodyParamsFromChannel(request, params);
        } else if (request.method() == HttpMethod.DELETE) {
            getBodyParamsFromChannel(request, params);
        } else if (request.method() == HttpMethod.PUT) {
            getBodyParamsFromChannel(request, params);
        } else {
            nettyChannel.sendResponseAndClose(new Response<>(RestStatus.INTERNAL_SERVER_ERROR, "request not found"));
        }
        return params;
    }

    /**
     * 获取GET方式传递的参数
     *
     * @param fullHttpRequest
     * @param params
     * @return
     */
    private void getUrlParamsFromChannel(FullHttpRequest fullHttpRequest, Map<String, Object> params) {
        // 处理get请求
        QueryStringDecoder decoder = new QueryStringDecoder(fullHttpRequest.uri());
        Map<String, List<String>> paramList = decoder.parameters();
        for (Map.Entry<String, List<String>> entry : paramList.entrySet()) {
            params.put(entry.getKey(), entry.getValue().get(0));
        }
    }

    /**
     * 获取POST方式传递的参数
     *
     * @param fullHttpRequest
     * @param params
     * @return
     */
    private void getBodyParamsFromChannel(FullHttpRequest fullHttpRequest, Map<String, Object> params) {
        // 处理POST请求
        String contentType = fullHttpRequest.headers().get("Content-Type").trim();
        if (contentType.contains("x-www-form-urlencoded")) {
            getFormParams(fullHttpRequest, params);
        } else if (contentType.contains("application/json")) {
            getJSONParams(fullHttpRequest, params);
        }
    }

    /**
     * 解析from表单数据（Content-Type = x-www-form-urlencoded）
     *
     * @param fullHttpRequest
     * @return
     */
    private void getFormParams(FullHttpRequest fullHttpRequest, Map<String, Object> params) {
        HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(new DefaultHttpDataFactory(false), fullHttpRequest);
        List<InterfaceHttpData> postData = decoder.getBodyHttpDatas();
        for (InterfaceHttpData data : postData) {
            if (data.getHttpDataType() == InterfaceHttpData.HttpDataType.Attribute) {
                MemoryAttribute attribute = (MemoryAttribute) data;
                params.put(attribute.getName(), attribute.getValue());
            }
        }
    }

    /**
     * 解析json数据（Content-Type = application/json）
     *
     * @param fullHttpRequest
     * @return
     */
    private void getJSONParams(FullHttpRequest fullHttpRequest, Map<String, Object> params) {
        ByteBuf byteBuf = fullHttpRequest.content();
        String content = byteBuf.toString(CharsetUtil.UTF_8);
        JSONObject jsonParams = JSONObject.parseObject(content);
        for (Object key : jsonParams.keySet()) {
            params.put(key.toString(), jsonParams.get(key));
        }
    }

    private final static HandlerManager handlerManager = new HandlerManager();

    public static HandlerManager getInstance() {
        return handlerManager;
    }

    private HandlerManager() {

    }
}
