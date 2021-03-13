package com.yuandragon.crispy.raft.enums;

/**
 * @Author: yuanzhanzhenxing
 */
public enum CommandEnum {

    /**
     * 新增操作
     */
    POST(1, "POST"),

    /**
     * 删除操作
     */
    DELETE(2, "DELETE"),

    /**
     * 修改操作
     */
    PUT(3, "PUT"),

    /**
     * 查询操作
     */
    GET(4, "GET"),

    ;

    private Integer code;

    private String desc;

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    CommandEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String getDescByCode(Integer code) {
        for (CommandEnum commandEnum : values()) {
            if (code.equals(commandEnum.code)) {
                return commandEnum.getDesc();
            }
        }
        return null;
    }
}