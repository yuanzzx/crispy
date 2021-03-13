package com.yuandragon.crispy.common.transport;

import com.alibaba.fastjson.annotation.JSONField;
import com.yuandragon.crispy.common.contans.RestStatus;

/**
 * @Author: yuanzhanzhenxing
 */
public class Response<T> {

    private Integer code;
    private RestStatus status;
    private T data;

    public Response(RestStatus status, T content) {
        this.status = status;
        this.code = status.getStatus();
        this.data = content;
    }

    public Response() {
    }

    public RestStatus getStatus() {
        return status;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public void setStatus(RestStatus status) {
        this.status = status;
    }

    public Integer getCode() {
        return code;
    }

    @JSONField(serialize = false)
    public boolean isSuccess() {
        return RestStatus.OK.getStatus() == this.code;
    }

    @JSONField(serialize = false)
    public boolean isFail() {
        return !isSuccess();
    }


    public static <T> Response<T> success() {
        return success(null);
    }

    public static <T> Response<T> success(T content) {
        return new Response<>(RestStatus.OK, content);
    }

    public static <T> Response<T> fail(T content) {
        return new Response<>(RestStatus.BAD_REQUEST, content);
    }

    public static <T> Response<T> fail() {
        return new Response<>(RestStatus.BAD_REQUEST, null);
    }


    @Override
    public String toString() {
        return "Response{" +
                "code=" + code +
                ", status=" + status +
                ", data=" + data +
                '}';
    }
}
