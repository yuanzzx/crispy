package com.yuandragon.crispy.raft.stateMachine;

import com.yuandragon.crispy.cache.bean.CacheValue;
import com.yuandragon.crispy.raft.pojo.bean.StateMachineLog;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;

import java.util.List;

/**
 * @Author: yuanzhanzhenxing
 */
public interface StateMachine {


    /**
     * 应用到状态机
     *
     * @param stateMachineLog
     * @return
     */
    boolean apply(StateMachineLog stateMachineLog) throws RocksDBException;

    /**
     * 应用到状态机
     *
     * @param stateMachineLogs
     * @return
     */
    boolean batchApply(List<StateMachineLog> stateMachineLogs) throws RocksDBException;

    /**
     * 获取状态机的迭代器
     *
     * @return
     */
    RocksIterator newIterator();

    /**
     * 获取持久化信息
     */
    CacheValue get(String key);
}
