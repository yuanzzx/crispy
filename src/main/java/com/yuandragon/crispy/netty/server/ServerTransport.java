package com.yuandragon.crispy.netty.server;


import com.yuandragon.crispy.netty.handler.HandlerManager;
import com.yuandragon.crispy.netty.http.NettyChannel;
import com.yuandragon.crispy.netty.http.NettyRequest;

/**
 * @Author: yuanzhanzhenxing
 */
public class ServerTransport {

    private final HandlerManager dispatcher = HandlerManager.getInstance();

    void dispatchRequest(final NettyRequest nettyRequest, NettyChannel nettyChannel) {
        dispatcher.handlerDispatcher(nettyRequest, nettyChannel);
    }
}
