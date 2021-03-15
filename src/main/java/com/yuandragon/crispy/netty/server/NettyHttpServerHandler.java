package com.yuandragon.crispy.netty.server;

import com.yuandragon.crispy.netty.handler.HandlerManager;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author: yuanzhanzhenxing
 */
@ChannelHandler.Sharable
public class NettyHttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static Logger logger = LoggerFactory.getLogger(NettyHttpServerHandler.class.getSimpleName());

    private final HandlerManager dispatcher = HandlerManager.getInstance();

    /**
     * 处理请求
     *
     * @param channelHandlerContext
     * @param request
     */
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, FullHttpRequest request) {
        logger.debug("接收到请求：{}    {}", request.method(), request.uri());
        dispatcher.handlerDispatcher(channelHandlerContext, request);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }


}