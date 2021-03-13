package com.yuandragon.crispy.netty.handler;

import com.alibaba.fastjson.JSON;
import com.yuandragon.crispy.cache.bean.CacheValue;
import com.yuandragon.crispy.cache.core.CacheFacade;
import com.yuandragon.crispy.common.contans.RestStatus;
import com.yuandragon.crispy.common.transport.Response;
import com.yuandragon.crispy.common.utils.TimeUtil;
import com.yuandragon.crispy.netty.http.NettyRequest;
import com.yuandragon.crispy.raft.enums.CommandEnum;
import com.yuandragon.crispy.raft.node.Node;
import com.yuandragon.crispy.raft.pojo.req.WrittenReq;
import com.yuandragon.crispy.raft.pojo.resp.WrittenResp;
import com.yuandragon.crispy.raft.strategy.ChooseNodeStrategy;
import io.netty.handler.codec.http.HttpMethod;
import org.rocksdb.RocksIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @Author: yuanzhanzhenxing
 */
public class KeysHandler implements Handler {
    private static final Logger LOGGER = LoggerFactory.getLogger(KeysHandler.class);

    /**
     * 缓存门面
     */
    private final CacheFacade cacheFacade = CacheFacade.getInstance();

    /**
     * 节点实现
     */
    private final Node node = ChooseNodeStrategy.getNode();

    /**
     * 服务代码
     */
    public static final String SERVICE_CODE = "/keys";

    KeysHandler() {
        LOGGER.info("开始将状态机数据读取到缓存");
        // 获取状态机应用的数据
        RocksIterator iter = node.newIterator();
        for (iter.seekToFirst(); iter.isValid(); iter.next()) {
            cacheFacade.put(new String(iter.key()), JSON.parseObject(iter.value(), CacheValue.class));
        }
        LOGGER.info("状态机数据读取到缓存结束");
    }

    @Override
    public String getServiceCode() {
        return SERVICE_CODE;
    }

    @Override
    public Response handle(NettyRequest request) {
        return doHandle(request);
    }

    /**
     * 处理请求
     *
     * @param request
     * @return
     */
    private Response doHandle(NettyRequest request) {
        Map<String, Object> params = request.getParams();
        Object obj = params.get("key");
        if (obj == null) {
            return new Response<>(RestStatus.FORBIDDEN, "请指定key");
        }
        // 设置创建时间
        params.put("createTime", TimeUtil.currentTimeMillis());
        // 获取key
        String key = obj.toString();

        Response response;
        if (request.getMethod().equals(HttpMethod.POST)) {
            // 新增
            response = this.processPostRequest(key, params);
        } else if (request.getMethod().equals(HttpMethod.PUT)) {
            // 修改
            response = this.processPutRequest(key, params);
        } else if (request.getMethod().equals(HttpMethod.GET)) {
            // 获取
            CacheValue cacheValue = cacheFacade.get(key);
            response = Response.success(cacheValue);
        } else if (request.getMethod().equals(HttpMethod.DELETE)) {
            // 删除
            response = this.processDeleteRequest(key, params);
        } else {
            response = new Response<>(RestStatus.BAD_REQUEST, "request not found");
        }
        return response;
    }

    /**
     * 处理转发请求
     *
     * @param request
     * @return
     */
    public Response doHandleRedirect(NettyRequest request) {
        Map<String, Object> params = request.getParams();
        // 序列化转发请求
        WrittenReq writtenReq = JSON.parseObject(params.get("obj").toString(), WrittenReq.class);
        Response response;
        if (CommandEnum.POST.getCode().equals(writtenReq.getCommand())) {
            // 新增
            response = this.processPostRequest(writtenReq.getKey(), writtenReq.getValue());
        } else if (CommandEnum.PUT.getCode().equals(writtenReq.getCommand())) {
            // 修改
            response = this.processPutRequest(writtenReq.getKey(), writtenReq.getValue());
        } else if (CommandEnum.GET.getCode().equals(writtenReq.getCommand())) {
            // 获取
            CacheValue cacheValue = cacheFacade.get(writtenReq.getKey());
            response = Response.success(cacheValue);
        } else if (CommandEnum.DELETE.getCode().equals(writtenReq.getCommand())) {
            // 删除
            response = this.processDeleteRequest(writtenReq.getKey(), writtenReq.getValue());
        } else {
            response = new Response<>(RestStatus.BAD_REQUEST, "request not found");
        }
        if (response.isSuccess()){
            WrittenResp writtenResp = new WrittenResp();
            writtenResp.setSuccess(response.isSuccess());
            response = Response.success(writtenResp);
        }
        return response;
    }

    /**
     * 处理post请求
     *
     * @param params
     * @return
     */
    private Response processPostRequest(String key, Map<String, Object> params) {
        WrittenReq writtenReq = new WrittenReq();
        writtenReq.setCommand(CommandEnum.POST.getCode());
        writtenReq.setKey(key);
        writtenReq.setValue(params);
        // 执行同步操作
        WrittenResp writtenResp = node.handleWrittenRequest(writtenReq);
        // 同步成功
        if (writtenResp.isSuccess()) {
            CacheValue cacheValue = cacheFacade.convertToCacheValue(key, params);
            // 设置到缓存
            cacheValue = cacheFacade.put(key, cacheValue);
            return Response.success(cacheValue);
        } else {
            return new Response<>(RestStatus.NOT_ACCEPTABLE, "修改失败");
        }
    }

    /**
     * 处理put请求
     *
     * @param key
     * @param params
     * @return
     */
    private Response processPutRequest(String key, Map<String, Object> params) {
        // 被替换的value值
        Object obj = params.get("preValue");
        if (obj == null) {
            // 可以看做是新增
            return this.processPostRequest(key, params);
        }

        // 获取缓存信息
        CacheValue cacheValue = cacheFacade.get(key);
        // 如果不相等，则新增失败
        if (cacheValue == null || !cacheValue.getValue().equals(obj.toString())) {
            return new Response<>(RestStatus.FORBIDDEN, "修改失败，preValue校验失败");
        }
        // 加锁
        synchronized (key.intern()) {
            // 再次获取缓存信息
            cacheValue = cacheFacade.get(key);
            // 如果不相等，则新增失败
            if (cacheValue == null || !cacheValue.getValue().equals(obj.toString())) {
                return new Response<>(RestStatus.FORBIDDEN, "修改失败，preValue校验失败");
            }

            // 记录日志
            WrittenReq writtenReq = new WrittenReq();
            writtenReq.setCommand(CommandEnum.PUT.getCode());
            writtenReq.setKey(key);
            writtenReq.setValue(params);
            WrittenResp writtenResp = node.handleWrittenRequest(writtenReq);
            // 同步成功
            if (writtenResp.isSuccess()) {
                cacheValue = cacheFacade.convertToCacheValue(key, params);
                // 设置到缓存
                cacheValue = cacheFacade.put(key, cacheValue);
                return Response.success(cacheValue);
            } else {
                return new Response<>(RestStatus.NOT_ACCEPTABLE, "修改失败");
            }
        }
    }

    /**
     * 处理delete请求
     *
     * @param key
     * @param params
     */
    private Response processDeleteRequest(String key, Map<String, Object> params) {
        // 被替换的value值
        Object obj = params.get("preValue");
        if (obj != null) {
            CacheValue cacheValue = cacheFacade.get(key);
            // 如果不相等，则删除失败
            if (cacheValue == null || !cacheValue.getValue().equals(obj.toString())) {
                return new Response<>(RestStatus.FORBIDDEN, "删除失败，key不存在");
            }
        }

        // 加锁
        synchronized (key.intern()) {
            if (obj != null) {
                CacheValue cacheValue = cacheFacade.get(key);
                // 如果不相等，则删除失败
                if (cacheValue == null || !cacheValue.getValue().equals(obj.toString())) {
                    return new Response<>(RestStatus.FORBIDDEN, "删除失败，key不存在");
                }
            }
            WrittenReq writtenReq = new WrittenReq();
            writtenReq.setCommand(CommandEnum.DELETE.getCode());
            writtenReq.setKey(key);
            writtenReq.setValue(params);
            // 执行同步操作
            WrittenResp writtenResp = node.handleWrittenRequest(writtenReq);
            // 同步成功
            if (writtenResp.isSuccess()) {
                cacheFacade.delete(key);
                return Response.success();
            } else {
                return new Response<>(RestStatus.NOT_ACCEPTABLE, "删除失败");
            }
        }
    }
}
