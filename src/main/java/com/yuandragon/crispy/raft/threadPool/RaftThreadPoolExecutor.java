package com.yuandragon.crispy.raft.threadPool;

import java.util.concurrent.*;

/**
 * @Author: yuanzhanzhenxing
 */
public class RaftThreadPoolExecutor {

    /**
     * 获取当前cpu数量
     */
    private static int cpu = Runtime.getRuntime().availableProcessors();

    /**
     * 最大线程数
     */
    private static int maxPoolSize = cpu * 2;

    /**
     * 队列数量
     */
    private static final int queueSize = 1024;

    /**
     * 存活时间
     */
    private static final long keepTime = 1000 * 60;

    /**
     * 时间类型
     */
    private static TimeUnit keepTimeUnit = TimeUnit.MILLISECONDS;

    /**
     * 延迟线程池
     */
    private static ScheduledExecutorService scheduledThreadPool = new ScheduledThreadPoolExecutor(cpu, new NameThreadFactory());

    /**
     * 执行线程池
     */
    private static ThreadPoolExecutor executeThreadPool = new ThreadPoolExecutor(
            cpu,
            maxPoolSize,
            keepTime,
            keepTimeUnit,
            new LinkedBlockingQueue<>(queueSize),
            new NameThreadFactory());


    public static void scheduleAtFixedRate(Runnable r, long initDelay, long delay) {
        scheduledThreadPool.scheduleAtFixedRate(r, initDelay, delay, TimeUnit.MILLISECONDS);
    }


    public static void scheduleWithFixedDelay(Runnable r, long delay) {
        scheduledThreadPool.scheduleWithFixedDelay(r, 0, delay, TimeUnit.MILLISECONDS);
    }

    /**
     * 有返回值
     * @param r
     * @param <T>
     * @return
     */
    public static <T> Future<T> submit(Callable<T> r) {
        return executeThreadPool.submit(r);
    }

    /**
     * 无返回值
     * @param r
     */
    public static void execute(Runnable r) {
        execute(r, false);
    }

    public static void execute(Runnable r, boolean sync) {
        if (sync) {
            r.run();
        } else {
            executeThreadPool.execute(r);
        }
    }

    private static class NameThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "crispy raft thread");
            t.setDaemon(true);
            t.setPriority(3);
            return t;
        }
    }


}
