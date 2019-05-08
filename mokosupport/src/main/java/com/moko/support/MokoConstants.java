package com.moko.support;

public class MokoConstants {
    // 发送头
    public static final int HEADER_SEND = 0xED;
    // 读取发送尾
    public static final int END_READ = 0xEE;
    // 应答头
    public static final int HEADER_REPLAY = 0xED;
    // 应答成功
    public static final int SUCCESS_REPLAY = 0xAA;
    // 应答失败
    public static final int FAILED_REPLAY = 0xFF;
    // 发现状态
    public static final String ACTION_DISCOVER_SUCCESS = "com.moko.lorawan.ACTION_DISCOVER_SUCCESS";
    public static final String ACTION_DISCOVER_TIMEOUT = "com.moko.lorawan.ACTION_DISCOVER_TIMEOUT";
    // 断开连接
    public static final String ACTION_CONN_STATUS_DISCONNECTED = "com.moko.lorawan.ACTION_CONN_STATUS_DISCONNECTED";
    // 命令结果
    public static final String ACTION_ORDER_RESULT = "com.moko.lorawan.ACTION_ORDER_RESULT";
    public static final String ACTION_ORDER_TIMEOUT = "com.moko.lorawan.ACTION_ORDER_TIMEOUT";
    public static final String ACTION_ORDER_FINISH = "com.moko.lorawan.ACTION_ORDER_FINISH";
    public static final String ACTION_CURRENT_DATA = "com.moko.lorawan.ACTION_CURRENT_DATA";

    // extra_key
    public static final String EXTRA_KEY_RESPONSE_ORDER_TASK = "EXTRA_KEY_RESPONSE_ORDER_TASK";
    public static final String EXTRA_KEY_CURRENT_DATA_TYPE = "EXTRA_KEY_CURRENT_DATA_TYPE";
}
