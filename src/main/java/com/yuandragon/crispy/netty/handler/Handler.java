package com.yuandragon.crispy.netty.handler;


import com.yuandragon.crispy.common.transport.Response;
import com.yuandragon.crispy.netty.http.NettyRequest;

/**
 * @Author: yuanzhanzhenxing
 */
public interface Handler {


    /**
     * 处理种类
     */
    String getServiceCode();

    /**
     * 处理方法
     */
    Response handle(NettyRequest nettyRequest);
}
