package com.yuandragon.crispy.common.utils;

import java.util.Collection;

/**
 * @Author: yuanzhanzhenxing
 */
public class CollectionUtil {


    /**
     * 集合是否为空
     *
     * @param collection
     * @return
     */
    public static boolean isEmpty(Collection collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * 集合是否不为空
     *
     * @param collection
     * @return
     */
    public static boolean isNotEmpty(Collection collection) {
        return !isEmpty(collection);
    }

}
