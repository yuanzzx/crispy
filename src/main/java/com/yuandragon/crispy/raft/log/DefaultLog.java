package com.yuandragon.crispy.raft.log;

import com.alibaba.fastjson.JSON;
import com.yuandragon.crispy.common.setting.CrispySettingConfig;
import com.yuandragon.crispy.common.utils.CollectionUtil;
import com.yuandragon.crispy.raft.pojo.bean.LogEntry;
import com.yuandragon.crispy.raft.util.ConvertTypeUtil;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * @Author: yuanzhanzhenxing
 */
public class DefaultLog implements Log {

    private static final Logger LOGGER = LoggerFactory.getLogger(Log.class);

    /**
     * 日志数据库对象
     */
    private RocksDB LOG_DB;

    /**
     * 记录最后索引值的记录
     */
    private final static byte[] LAST_INDEX_KEY = "LAST_INDEX_KEY".getBytes();

    /**
     * 记录最后一个索引值
     */
    private volatile AtomicLong LAST_INDEX;

    /**
     * 日志锁
     */
    private ReentrantLock LOG_LOCK = new ReentrantLock();

    private DefaultLog() {
        String logsDir = CrispySettingConfig.LOG_DIR + CrispySettingConfig.SERVER_PORT + "/log";
        File file = new File(logsDir);
        // 判断文件是否存在
        if (!file.exists()) {
            // 不存在在创建
            if (file.mkdirs()) {
                LOGGER.info("日志文件夹创建成功，文件url：{}", logsDir);
            }
        }
        RocksDB.loadLibrary();

        Options options = new Options();
        options.setCreateIfMissing(true);
        try {
            LOG_DB = RocksDB.open(options, logsDir);
            // 获取最后一个索引
            byte[] bytes = LOG_DB.get(LAST_INDEX_KEY);
            if (bytes != null) {
                LAST_INDEX = new AtomicLong(ConvertTypeUtil.byteToLong(bytes));
            } else {
                LAST_INDEX = new AtomicLong(-1);
                // 如果为空则保存一条记录
                LOG_DB.put(LAST_INDEX_KEY, ConvertTypeUtil.longToByte(LAST_INDEX.get()));
                // 保存第一条默认日志
                LogEntry logEntry = new LogEntry();
                logEntry.setIndex(-1L);
                logEntry.setTerm(0);
                write(logEntry);
            }
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
        LOGGER.info("初始化日志成功");
    }

    /**
     * 写入日志
     *
     * @param logEntry
     */
    @Override
    public void write(LogEntry logEntry) throws RocksDBException {
        try {
            LOG_LOCK.tryLock(3000, MILLISECONDS);
            if (logEntry.getIndex() == null) {
                // 设置id
                logEntry.setIndex(indexIncrease());
            } else {
                // 更新索引
                updateLastIndex(logEntry.getIndex());
            }
            LOG_DB.put(logEntry.getIndex().toString().getBytes(), JSON.toJSONBytes(logEntry));
        } catch (RocksDBException | InterruptedException e) {
            LOGGER.warn(e.getMessage());
            indexDecrease();
        } finally {
            LOG_LOCK.unlock();
        }
    }

    /**
     * 读取日志
     *
     * @param index
     * @return
     */
    @Override
    public LogEntry read(Long index) {
        try {
            byte[] result = LOG_DB.get(ConvertTypeUtil.longToByte(index));
            if (result == null) {
                return null;
            }
            return JSON.parseObject(result, LogEntry.class);
        } catch (RocksDBException e) {
            LOGGER.warn(e.getMessage(), e);
        }
        return null;
    }

    /**
     * 获取最后一条日志
     *
     * @return
     */
    @Override
    public LogEntry getLastLog() {
        try {
            byte[] result = LOG_DB.get(ConvertTypeUtil.longToByte(getLastLogIndex()));
            if (result == null) {
                return null;
            }
            return JSON.parseObject(result, LogEntry.class);
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取最后一条日志的索引
     *
     * @return
     */
    @Override
    public Long getLastLogIndex() {
        return LAST_INDEX.get();
    }

    /**
     * 从指定索引锁获取之后的所有日志，不包头
     *
     * @param startIndex
     * @return
     */
    @Override
    public List<LogEntry> getOnStartIndex(long startIndex) {

        List<byte[]> keys = new ArrayList<>();
        for (long i = startIndex; i <= this.getLastLogIndex(); i++) {
            keys.add(String.valueOf(i).getBytes());
        }
        if (CollectionUtil.isEmpty(keys)) {
            return null;
        }
        try {
            Map<byte[], byte[]> map = LOG_DB.multiGet(keys);
            List<LogEntry> logEntries = map.entrySet().stream()
                    .sorted(Comparator.comparing(entry -> ConvertTypeUtil.byteToLong(entry.getKey())))
                    .map(entry -> JSON.<LogEntry>parseObject(entry.getValue(), LogEntry.class))
                    .collect(Collectors.toList());

            return logEntries;
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 从指定索引处删除日志
     *
     * @param startIndex
     */
    @Override
    public void removeOnStartIndex(Long startIndex) {
        boolean success = false;
        int count = 0;
        try {
            LOG_LOCK.tryLock(3000, MILLISECONDS);
            for (long i = startIndex; i <= getLastLogIndex(); i++) {
                LOG_DB.delete(String.valueOf(i).getBytes());
                ++count;
            }
            success = true;
        } catch (InterruptedException | RocksDBException e) {
            LOGGER.warn(e.getMessage());
        } finally {
            if (success) {
                updateLastIndex(getLastLogIndex() - count);
            }
            LOG_LOCK.unlock();
        }
    }

    /**
     * 主键增加
     *
     * @return
     */
    public Long indexIncrease() throws RocksDBException {
        return indexIncrease(1);
    }

    /**
     * 主键减少
     *
     * @return
     */
    public Long indexDecrease() throws RocksDBException {
        return indexDecrease(1);
    }

    /**
     * 主键增加
     *
     * @return
     */
    public Long indexIncrease(long step) throws RocksDBException {
        Long index = LAST_INDEX.addAndGet(step);
        LOG_DB.put(LAST_INDEX_KEY, ConvertTypeUtil.longToByte(index));
        return index;
    }

    /**
     * 主键减少
     *
     * @return
     */
    public Long indexDecrease(long step) throws RocksDBException {
        Long index = LAST_INDEX.addAndGet(-step);
        LOG_DB.put(LAST_INDEX_KEY, String.valueOf(index).getBytes());
        return index;
    }

    /**
     * 新增
     *
     * @param key
     * @param value
     * @throws RocksDBException
     */
    private void put(Long key, Object value) throws RocksDBException {
        LOG_DB.put(ConvertTypeUtil.longToByte(key), value.toString().getBytes());
    }

    /**
     * 更新索引
     *
     * @param index
     */
    private void updateLastIndex(Long index) {
        try {
            LAST_INDEX.set(index);
            // overWrite
            LOG_DB.put(LAST_INDEX_KEY, index.toString().getBytes());
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
    }

    private static final DefaultLog INSTANCE = new DefaultLog();

    public static DefaultLog getInstance() {
        return INSTANCE;
    }
}
