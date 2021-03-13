package com.yuandragon.crispy.netty.server;

import com.alibaba.fastjson.JSONObject;
import com.yuandragon.crispy.netty.http.NettyChannel;
import com.yuandragon.crispy.netty.http.NettyRequest;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.MemoryAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: yuanzhanzhenxing
 */
@ChannelHandler.Sharable
public class NettyHttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static Logger logger = LoggerFactory.getLogger(NettyHttpServerHandler.class.getSimpleName());

    private ServerTransport serverTransport;

    NettyHttpServerHandler(ServerTransport serverTransport) {
        this.serverTransport = serverTransport;
    }

    /**
     * 处理请求
     *
     * @param channelHandlerContext
     * @param request
     */
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, FullHttpRequest request) {
        logger.debug("接收到请求：{}    {}", request.method(), request.uri());
        // 获取请求参数
        Map<String, Object> params = getParams(channelHandlerContext, request);
        // 包装
        NettyRequest nettyRequest = new NettyRequest(request, params);
        NettyChannel nettyChannel = new NettyChannel(channelHandlerContext);
        // 转发请求
        if (request.decoderResult().isSuccess()) {
            serverTransport.dispatchRequest(nettyRequest, nettyChannel);
        } else {
            FullHttpResponse response = responseOK(HttpResponseStatus.INTERNAL_SERVER_ERROR, null);
            channelHandlerContext.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }

    /**
     * 获取请求参数
     *
     * @param channelHandlerContext
     * @param request
     * @return
     */
    private Map<String, Object> getParams(ChannelHandlerContext channelHandlerContext, FullHttpRequest request) {
        if (request.method() == HttpMethod.GET) {
            return getUrlParamsFromChannel(request);
        } else if (request.method() == HttpMethod.POST) {
            return getBodyParamsFromChannel(request);
        } else if (request.method() == HttpMethod.DELETE) {
            return getBodyParamsFromChannel(request);
        } else if (request.method() == HttpMethod.PUT) {
            return getBodyParamsFromChannel(request);
        } else {
            FullHttpResponse response = responseOK(HttpResponseStatus.INTERNAL_SERVER_ERROR, null);
            channelHandlerContext.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            return null;
        }
    }

    /**
     * 获取GET方式传递的参数
     *
     * @param fullHttpRequest
     * @return
     */
    private Map<String, Object> getUrlParamsFromChannel(FullHttpRequest fullHttpRequest) {
        Map<String, Object> params = new HashMap<>();
        // 处理get请求
        QueryStringDecoder decoder = new QueryStringDecoder(fullHttpRequest.uri());
        Map<String, List<String>> paramList = decoder.parameters();
        for (Map.Entry<String, List<String>> entry : paramList.entrySet()) {
            params.put(entry.getKey(), entry.getValue().get(0));
        }
        return params;
    }

    /**
     * 获取POST方式传递的参数
     *
     * @param fullHttpRequest
     * @return
     */
    private Map<String, Object> getBodyParamsFromChannel(FullHttpRequest fullHttpRequest) {

        Map<String, Object> params;

        // 处理POST请求
        String strContentType = fullHttpRequest.headers().get("Content-Type").trim();
        if (strContentType.contains("x-www-form-urlencoded")) {
            params = getFormParams(fullHttpRequest);
        } else if (strContentType.contains("application/json")) {
            try {
                params = getJSONParams(fullHttpRequest);
            } catch (UnsupportedEncodingException e) {
                return null;
            }
        } else {
            return null;
        }
        return params;
    }

    /**
     * 解析from表单数据（Content-Type = x-www-form-urlencoded）
     *
     * @param fullHttpRequest
     * @return
     */
    private Map<String, Object> getFormParams(FullHttpRequest fullHttpRequest) {
        Map<String, Object> params = new HashMap<>();

        HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(new DefaultHttpDataFactory(false), fullHttpRequest);
        List<InterfaceHttpData> postData = decoder.getBodyHttpDatas();

        for (InterfaceHttpData data : postData) {
            if (data.getHttpDataType() == InterfaceHttpData.HttpDataType.Attribute) {
                MemoryAttribute attribute = (MemoryAttribute) data;
                params.put(attribute.getName(), attribute.getValue());
            }
        }

        return params;
    }

    /**
     * 解析json数据（Content-Type = application/json）
     *
     * @param fullHttpRequest
     * @return
     * @throws UnsupportedEncodingException
     */
    private Map<String, Object> getJSONParams(FullHttpRequest fullHttpRequest) throws UnsupportedEncodingException {
        Map<String, Object> params = new HashMap<String, Object>();

        ByteBuf content = fullHttpRequest.content();
        byte[] reqContent = new byte[content.readableBytes()];
        content.readBytes(reqContent);
        String strContent = new String(reqContent, "UTF-8");

        JSONObject jsonParams = JSONObject.parseObject(strContent);
        for (Object key : jsonParams.keySet()) {
            params.put(key.toString(), jsonParams.get(key));
        }

        return params;
    }

    private FullHttpResponse responseOK(HttpResponseStatus status, ByteBuf content) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, content);
        if (content != null) {
            response.headers().set("Content-Type", "text/plain;charset=UTF-8");
            response.headers().set("Content_Length", response.content().readableBytes());
        }
        return response;
    }

}