package com.yuandragon.crispy.common.transport;

import java.io.Serializable;

/**
 * @Author: yuanzhanzhenxing
 */
public class Request implements Serializable {

    /**
     * 请求类型
     */
    private int cmd;

    /**
     * 请求内容
     */
    private Object obj;

    /**
     * 请求地址
     */
    private String url;

    public Request() {
    }

    public Request(Object obj) {
        this.obj = obj;
    }

    public Request(int cmd, Object obj, String url) {
        this.cmd = cmd;
        this.obj = obj;
        this.url = url;
    }

    public int getCmd() {
        return cmd;
    }

    public void setCmd(int cmd) {
        this.cmd = cmd;
    }

    public Object getObj() {
        return obj;
    }

    public void setObj(Object obj) {
        this.obj = obj;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "Request{" +
                "cmd=" + cmd +
                ", obj=" + obj +
                ", url='" + url + '\'' +
                '}';
    }
}
