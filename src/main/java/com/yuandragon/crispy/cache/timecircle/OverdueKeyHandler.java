package com.yuandragon.crispy.cache.timecircle;

import com.yuandragon.crispy.cache.bean.CacheValue;
import com.yuandragon.crispy.cache.core.Cahce;
import com.yuandragon.crispy.cache.core.CahceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/**
 * @Author: yuanzhanzhenxing
 */
public class OverdueKeyHandler {
    private static Logger logger = LoggerFactory.getLogger(OverdueKeyHandler.class.getSimpleName());

    private OverdueKeyHandler() {
    }

    private final static OverdueKeyHandler overdueKeyHandler = new OverdueKeyHandler();

    public static OverdueKeyHandler getOverdueKeyHandler() {
        return overdueKeyHandler;
    }

    private final static ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    private final static ExecutorService processExpireKeyExecutor = Executors.newFixedThreadPool(3);
    private static Cahce cahce = CahceImpl.getCahce();
    private final static int TIME_CIRCLE_LENGHT = 60;
    private static OverdueNodeSentry[] overdueNodeSentries = new OverdueNodeSentry[TIME_CIRCLE_LENGHT];
    private static volatile AtomicInteger index = new AtomicInteger(1);
    private static volatile LongAdder count = new LongAdder();

    static {
        for (int i = 0; i < TIME_CIRCLE_LENGHT; i++) {
            overdueNodeSentries[i] = new OverdueNodeSentry();
        }

        scheduledExecutor.scheduleWithFixedDelay(() -> {
            // 获取下标
            int i = index.getAndIncrement();
            if (i == TIME_CIRCLE_LENGHT) {
                index.set(0);
                i = index.getAndIncrement();
            }
            // 获取索引值
            OverdueNodeSentry overdueNodeSentry = overdueNodeSentries[i];
            processExpireKeyExecutor.execute(() -> processExpireKey(overdueNodeSentry));
        }, 0, 1, TimeUnit.SECONDS);
    }

    private static void processExpireKey(OverdueNodeSentry sentryNode) {
        // 判断是否存在头节点
        if (sentryNode == null) {
            return;
        }
        // 记录当前获取的头节点的快照，因为可能在执行过期算法时，头节点被更新，
        // 而且根据猜测新插入的头结点应该不会这么快就需要执行过期算法，所以，从当前获取的头节点至尾节点执行过期算法。
        OverdueNode headNode;
        // 获取锁
        synchronized (sentryNode) {
            // 从哨兵节点获取头结点
            headNode = sentryNode.next;
            // 如果没有头结点，则直接返回
            if (headNode == null) {
                return;
            }
            // 拆分哨兵节点和头结点
            sentryNode.next = null;
        }

        // 使用一个存活节点，记录在过期算法中幸存的节点
        OverdueNode surviveNode = new OverdueNode();
        surviveNode.next = headNode;

        // 从头节点的上一个节点开始
        OverdueNode preNode = surviveNode;
        // 获取哨兵节点的下一个节点
        OverdueNode tmp;
        while ((tmp = preNode.next) != null) {
            // 到期，并且存活次数为0
            if (tmp.getSurvivalValue() <= 0) {
                // 删除缓存 通过key he 相同的 value
                cahce.delete(tmp.key, tmp.value);
                // 将该节点的下一个节点设置为前一个节点的下一个节点
                preNode.next = tmp.next;
                count.decrement();
                logger.info("键过期：{}:{}", tmp.key, tmp.getValue());
                continue;
            } else {
                // 获取缓存，判断是否相同
                CacheValue cacheValue = cahce.get(tmp.key);
                // 如果不存在或是值已更新，则删除
                if (cacheValue == null || !cacheValue.equals(tmp.value)) {
                    // 将该节点的下一个节点设置为前一个节点的下一个节点
                    preNode.next = tmp.next;
                    // 数量减一
                    count.decrement();
                    logger.info("键过期：{}:{}", tmp.key, tmp.getValue());
                    continue;
                } else {
                    // 相同则将年龄减一
                    tmp.setSurvivalValue(tmp.getSurvivalValue() - 1);
                }
            }
            // 设置当前节点为前一个节点
            preNode = tmp;
        }

        // 判断是否还有节点幸存，存在幸存节点才链接到尾部
        if (surviveNode.next != null) {
            // 获取锁
            synchronized (sentryNode) {
                // 将最新的头节点链到存活节点的尾部, preNode 没有下一个节点，则一定是尾节点
                preNode.next = sentryNode.next;
                // 设置最新的头结点
                sentryNode.next = surviveNode.next;
            }
        }
    }

    public static void put(String key, CacheValue cacheValue) {
        int circleIndex = index.get();
        // 存活秒数 ÷ 时间轮长度 = 存活轮数
        int survivalValue = cacheValue.getTtl() / TIME_CIRCLE_LENGHT;
        // (存活秒数 + 当前时间轮下标) % 时间轮长度 = 存在时间轮索引位
        int index = (cacheValue.getTtl() + circleIndex) % TIME_CIRCLE_LENGHT;

        OverdueNode overdueNode = new OverdueNode(key, cacheValue, survivalValue);
        // 获取哨兵节点
        OverdueNodeSentry sentryNode = overdueNodeSentries[index];
        synchronized (sentryNode) {
            // 加入到第一个
            overdueNode.next = sentryNode.next;
            sentryNode.next = overdueNode;
        }
        count.increment();
    }

    public static long getExpirationCount() {
        return count.sum();
    }

    /**
     * 哨兵节点
     */
    private static class OverdueNodeSentry {
        private OverdueNode next;
    }

    /**
     * 过期数据节点
     */
    private static class OverdueNode {
        private String key;
        private CacheValue value;
        private Integer survivalValue;
        private OverdueNode next;

        public OverdueNode() {
        }

        public OverdueNode(String key, CacheValue value, Integer survivalValue) {
            this.key = key;
            this.value = value;
            this.survivalValue = survivalValue;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public Integer getSurvivalValue() {
            return survivalValue;
        }

        public void setSurvivalValue(Integer survivalValue) {
            this.survivalValue = survivalValue;
        }

        public OverdueNode getNext() {
            return next;
        }

        public void setNext(OverdueNode next) {
            this.next = next;
        }

        public CacheValue getValue() {
            return value;
        }

        public void setValue(CacheValue value) {
            this.value = value;
        }
    }

}
