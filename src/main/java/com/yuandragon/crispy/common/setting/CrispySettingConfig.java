package com.yuandragon.crispy.common.setting;

import java.util.concurrent.ThreadLocalRandom;

/**
 * @Author: yuanzhanzhenxing
 */
public class CrispySettingConfig {

    /**
     * 端口号
     */
    public static int SERVER_PORT = 7111;

    /**
     * 日志地址
     */
    public static String LOG_DIR = ".\\";

    /**
     * 单机
     */
    public static String STANDALONE = "standalone";

    /**
     * 集群
     */
    public static String CLUSTER = "cluster";

    /**
     * 模式 单机 or 集群
     */
    public static String NODE_MODEL = STANDALONE;

    /**
     * 服务关闭
     */
    public static final int OFF = 0;

    /**
     * 服务开启
     */
    public static final int ON = 1;

    /**
     * 服务开关
     */
    public static volatile int SERVICE_SWITCH = OFF;

    /**
     * 心跳超时阈值
     */
    public static long HEARTBEAT_TIMEOUT_THRESHOLD = 9 * 1000;

    /**
     * 选举间隔时间，由于启动时需要把该时间错开，所以取一个随机值
     */
    public static long ELECTION_INTERVAL_TIME = 9 * 1000 + ThreadLocalRandom.current().nextInt(350);


}
