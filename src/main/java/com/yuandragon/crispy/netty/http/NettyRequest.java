package com.yuandragon.crispy.netty.http;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;

import java.util.Map;

/**
 * @Author: yuanzhanzhenxing
 */
public class NettyRequest {

    private final FullHttpRequest request;
    private final Map<String, Object> params;
    private final HttpMethod method;

    public NettyRequest(FullHttpRequest request, Map<String, Object> params) {
        this.request = request;
        this.params = params;
        this.method = request.method();
    }

    public FullHttpRequest getRequest() {
        return request;
    }


    public Map<String, Object> getParams() {
        return params;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public String getUri() {
        return request.uri();
    }

}
