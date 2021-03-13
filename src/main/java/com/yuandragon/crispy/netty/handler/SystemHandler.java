package com.yuandragon.crispy.netty.handler;


import com.yuandragon.crispy.cache.core.Cahce;
import com.yuandragon.crispy.cache.core.CahceImpl;
import com.yuandragon.crispy.cache.timecircle.OverdueKeyHandler;
import com.yuandragon.crispy.common.contans.RestStatus;
import com.yuandragon.crispy.common.transport.Response;
import com.yuandragon.crispy.netty.http.NettyRequest;

import java.util.Map;

/**
 * @Author: yuanzhanzhenxing
 */
public class SystemHandler implements Handler {

    /**
     * 缓存实现
     */
    private Cahce cahce = CahceImpl.getCahce();

    @Override
    public String getServiceCode() {
        return "/system";
    }

    @Override
    public Response handle(NettyRequest request) {
        return doHandle(request);
    }

    private Response doHandle(NettyRequest request) {
        Map<String, Object> params = request.getParams();
        Object obj = params.get("key");
        if (obj == null) {
            return new Response<>(RestStatus.FORBIDDEN, "请指定key");
        }
        String value = obj.toString();
        if (value.equals("expirationCount")) {
            long expirationCount = OverdueKeyHandler.getExpirationCount();
            return new Response<>(RestStatus.OK, expirationCount);
        } else if (value.equals("totalCount")) {
            long sum = cahce.sum();
            return new Response<>(RestStatus.OK, sum);
        }
        return Response.success();
    }
}
