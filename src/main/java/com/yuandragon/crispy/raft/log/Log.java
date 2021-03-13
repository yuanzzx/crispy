package com.yuandragon.crispy.raft.log;

import com.yuandragon.crispy.raft.pojo.bean.LogEntry;
import org.rocksdb.RocksDBException;

import java.util.List;

/**
 * @Author: yuanzhanzhenxing
 */
public interface Log {

    /**
     * 写入日志
     *
     * @param logEntry
     */
    void write(LogEntry logEntry) throws RocksDBException;

    /**
     * 读取日志
     * @param index
     * @return
     */
    LogEntry read(Long index);

    /**
     * 获取最后一条日志
     * @return
     */
    LogEntry getLastLog();

    /**
     * 获取最后一条日志的索引
     * @return
     */
    Long getLastLogIndex();

    /**
     * 从指定索引处获取日志
     * @param startIndex
     */
    List<LogEntry> getOnStartIndex(long startIndex);

    /**
     * 从指定索引处删除日志
     * @param startIndex
     */
    void removeOnStartIndex(Long startIndex);
}
