package com.yuandragon.crispy.common.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @Author: yuanzhanzhenxing
 */
public class IpUtil {

    private static String LOCAL_ADDRESS;

    static {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            LOCAL_ADDRESS = localHost.getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public static String getHostAddress(){
        return LOCAL_ADDRESS;
    }

}
