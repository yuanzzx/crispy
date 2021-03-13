package com.yuandragon.crispy;

import com.yuandragon.crispy.common.id.Sequence;
import com.yuandragon.crispy.common.setting.CrispySettingConfig;
import com.yuandragon.crispy.netty.server.NettyServer;
import com.yuandragon.crispy.raft.meta.Peer;
import com.yuandragon.crispy.raft.meta.PeersManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


/**
 * @Author: yuanzhanzhenxing
 */
public class CrispyBoot {
    private static final Logger LOGGER = LoggerFactory.getLogger(CrispyBoot.class);

    public static void main(String[] args) {
        LOGGER.info("crispy boot start");
        startUp();
    }

    /**
     * 启动服务
     * @throws IOException
     */
    private static void startUp() {
        // 配置id生成器
        Sequence.setId(CrispySettingConfig.SERVER_PORT % 31, CrispySettingConfig.SERVER_PORT % 31);
        try {
            // 加载配置
            load();
            // 初始化 netty
            NettyServer server = new NettyServer();
            server.init();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("exception: " + e.getMessage());
        }
        LOGGER.warn("crispy close!");
    }

    /**
     * 加载配置
     * @throws IOException 文件不存在
     */
    private static void load() throws IOException {
        // 获取当前路径
        String path = System.getProperty("user.dir");
        Properties properties = new Properties();
        properties.load(new FileInputStream(new File(path + "/crispyConfig.properties")));

        HashMap<String, String> configMap = new HashMap<>();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            configMap.put(key.toString(), value.toString());
        }

        CrispySettingConfig.LOG_DIR = configMap.get("log.dir");
        CrispySettingConfig.SERVER_PORT = Integer.parseInt(configMap.get("server.port"));
        CrispySettingConfig.NODE_MODEL = configMap.get("model");
        // 处理节点
        PeersManager instance = PeersManager.getInstance();

        String servers = configMap.get("service");
        String[] split = servers.split(",");
        for (String s : split) {
            instance.addPeer(new Peer(s.trim()));
        }
    }
}
