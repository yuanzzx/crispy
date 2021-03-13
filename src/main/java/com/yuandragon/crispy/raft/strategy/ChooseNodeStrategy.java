package com.yuandragon.crispy.raft.strategy;

import com.yuandragon.crispy.common.setting.CrispySettingConfig;
import com.yuandragon.crispy.raft.node.ClusterNode;
import com.yuandragon.crispy.raft.node.Node;
import com.yuandragon.crispy.raft.node.StandaloneNode;

/**
 * @Author: yuanzhanzhenxing
 */
public class ChooseNodeStrategy {

    public static Node getNode() {
        // 根据节点模式创建节点对象
        if (CrispySettingConfig.NODE_MODEL.equals(CrispySettingConfig.STANDALONE)) {
            return StandaloneNode.getInstance();
        } else {
            return ClusterNode.getInstance();
        }
    }

}
