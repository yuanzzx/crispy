package com.yuandragon.crispy.raft.rpc;

import com.yuandragon.crispy.common.transport.Request;
import com.yuandragon.crispy.common.transport.Response;

import java.io.IOException;

/**
 * @Author: yuanzhanzhenxing
 */
public interface TcpTransport {


    /**
     * 发送请求
     */
    Response sendMessage(Request request) throws IOException;

    /**
     * 发送请求
     */
    Response sendMessage(Request request, int timeout) throws IOException;

    /**
     * 发送请求
     */
    <T> Response<T> sendMessage(Request request, Class<T> clazz);

}
