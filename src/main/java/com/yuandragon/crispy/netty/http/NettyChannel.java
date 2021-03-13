package com.yuandragon.crispy.netty.http;

import com.alibaba.fastjson.JSON;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;

import static io.netty.buffer.Unpooled.copiedBuffer;

/**
 * @Author: yuanzhanzhenxing
 */
public class NettyChannel {

    private final ChannelHandlerContext channelHandlerContext;

    public NettyChannel(ChannelHandlerContext channelHandlerContext) {
        this.channelHandlerContext = channelHandlerContext;
    }

    public void sendResponse(Object data) {
        String result = JSON.toJSONString(data);
        ByteBuf buf = copiedBuffer(result, CharsetUtil.UTF_8);
        FullHttpResponse response = responseOK(HttpResponseStatus.OK, buf);
        // 发送响应
        channelHandlerContext.writeAndFlush(response);
    }

    public void sendResponseAndClose(Object data) {
        String result = JSON.toJSONString(data);
        ByteBuf buf = copiedBuffer(result, CharsetUtil.UTF_8);
        FullHttpResponse response = responseOK(HttpResponseStatus.OK, buf);
        // 发送响应
        channelHandlerContext.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private FullHttpResponse responseOK(HttpResponseStatus status, ByteBuf content) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, content);
        response.headers().set("Content-Type", "application/json");
        response.headers().set("Content_Length", response.content().readableBytes());
        return response;
    }

}
