package com.yuandragon.crispy.raft.rpc;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.yuandragon.crispy.common.transport.Request;
import com.yuandragon.crispy.common.transport.Response;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @Author: yuanzhanzhenxing
 */
public class HttpTransport implements TcpTransport {
    public static Logger logger = LoggerFactory.getLogger(HttpTransport.class.getName());

    private CloseableHttpClient httpClient = HttpClientBuilder.create().build();

    @Override
    public Response sendMessage(Request request) throws IOException {
        return sendMessage(request, 1000);
    }

    @Override
    public Response sendMessage(Request request, int timeout) throws IOException {
        String result = doSendMessage(request, timeout);
        return JSON.parseObject(result, Response.class);
    }

    /**
     * 指定返回值的请求
     *
     * @param request
     * @param clazz
     * @param <T>
     * @return
     * @throws IOException
     */
    @Override
    public <T> Response<T> sendMessage(Request request, Class<T> clazz) {
        try {
            String result = doSendMessage(request, 1000);
            if (result == null) {
                return Response.fail();
            }
            Response<T> response = JSONObject.parseObject(result, new TypeReference<Response<T>>(clazz) {
            });
            return response;
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        return Response.fail();
    }

    /**
     * 指定网络请求操作
     *
     * @param request
     * @param timeout
     * @return
     * @throws IOException
     */
    private String doSendMessage(Request request, int timeout) throws IOException {
        // 设置请求路径
        HttpPost httpPost = new HttpPost(request.getUrl());
        // 设置请求头
        httpPost.setHeader("Content-Type", "application/json;charset=utf8");
        httpPost.setHeader("Connection", "close");
        RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(timeout).setConnectTimeout(timeout).build();
        httpPost.setConfig(requestConfig);
        // 请求参数
        StringEntity entity = new StringEntity(JSON.toJSONString(request), "UTF-8");
        httpPost.setEntity(entity);
        CloseableHttpResponse response = null;
        try {
            // 发送请求
            response = httpClient.execute(httpPost);
            // 从响应模型中获取响应实体
            HttpEntity responseEntity = response.getEntity();
            // 转换为string
            return EntityUtils.toString(responseEntity);
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
