package com.yuandragon.crispy.raft.util;

/**
 * @Author: yuanzhanzhenxing
 */
public class ConvertTypeUtil {


    public static Long byteToLong(byte[] param){
        return Long.valueOf(new String(param));
    }

    public static byte[] longToByte(Long param){
        return param.toString().getBytes();
    }

}
