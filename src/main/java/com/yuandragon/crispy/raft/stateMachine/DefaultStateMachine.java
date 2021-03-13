package com.yuandragon.crispy.raft.stateMachine;

import com.alibaba.fastjson.JSON;
import com.yuandragon.crispy.cache.bean.CacheValue;
import com.yuandragon.crispy.common.setting.CrispySettingConfig;
import com.yuandragon.crispy.raft.enums.CommandEnum;
import com.yuandragon.crispy.raft.pojo.bean.StateMachineLog;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author: yuanzhanzhenxing
 */
public class DefaultStateMachine implements StateMachine {
    private static final Logger LOGGER = LoggerFactory.getLogger(StateMachine.class);

    /**
     * 状态机日志存储
     */
    private static RocksDB stateMachineDB;

    /**
     * 应用到状态机需要加锁
     */
    private static ReentrantLock lock = new ReentrantLock();

    private DefaultStateMachine() {
        String stateMachineDir = CrispySettingConfig.LOG_DIR + CrispySettingConfig.SERVER_PORT + "/stateMachine";
        File file = new File(stateMachineDir);
        // 判断文件是否存在
        if (!file.exists()) {
            // 不存在在创建
            if (file.mkdirs()) {
                LOGGER.info("状态机文件夹创建成功，文件url：{}", stateMachineDir);
            }
        }

        RocksDB.loadLibrary();
        Options options = new Options();
        options.setCreateIfMissing(true);
        try {
            stateMachineDB = RocksDB.open(options, stateMachineDir);
        } catch (RocksDBException e) {
            e.printStackTrace();
        }

        LOGGER.info("初始化状态机成功");
    }

    @Override
    public boolean apply(StateMachineLog stateMachineLog) throws RocksDBException {
        try {
            lock.lock();
            if (stateMachineLog.getCommand().equals(CommandEnum.DELETE.getCode())) {
                stateMachineDB.delete(stateMachineLog.getKey().getBytes());
            } else {
                stateMachineDB.put(stateMachineLog.getKey().getBytes(), JSON.toJSONString(stateMachineLog.getValue()).getBytes());
            }
        } finally {
            lock.unlock();
        }
        return true;
    }

    @Override
    public boolean batchApply(List<StateMachineLog> stateMachineLogs) throws RocksDBException {
        try {
            lock.lock();
            for (StateMachineLog stateMachineLog : stateMachineLogs) {
                if (stateMachineLog.getCommand().equals(CommandEnum.DELETE.getCode())) {
                    stateMachineDB.delete(stateMachineLog.getKey().getBytes());
                } else {
                    stateMachineDB.put(stateMachineLog.getKey().getBytes(), JSON.toJSONString(stateMachineLog.getValue()).getBytes());
                }
            }
        } finally {
            lock.unlock();
        }
        return true;
    }

    @Override
    public RocksIterator newIterator() {
        return stateMachineDB.newIterator();
    }

    @Override
    public CacheValue get(String key) {
        try {
            byte[] bytes = stateMachineDB.get(key.getBytes());
            if (bytes != null) {
                return JSON.parseObject(bytes, CacheValue.class);
            }
            return null;
        } catch (RocksDBException e) {
            e.printStackTrace();
            return null;
        }
    }

    private final static DefaultStateMachine INSTANCE = new DefaultStateMachine();

    public static DefaultStateMachine getInstance() {
        return INSTANCE;
    }
}
