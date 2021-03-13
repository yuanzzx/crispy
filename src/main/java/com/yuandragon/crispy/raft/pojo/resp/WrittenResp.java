package com.yuandragon.crispy.raft.pojo.resp;

import java.io.Serializable;

/**
 * @Author: yuanzhanzhenxing
 */
public class WrittenResp implements Serializable {

    private boolean success;

    public WrittenResp() {
    }

    public WrittenResp(boolean success) {
        this.success = success;
    }


    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public static WrittenResp ok() {
        return new WrittenResp(true);
    }
    public static WrittenResp success() {
        return new WrittenResp(true);
    }

    public static WrittenResp fail() {
        return new WrittenResp(false);
    }

}
