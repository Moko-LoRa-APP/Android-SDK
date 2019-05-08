package com.moko.support.entity;

import java.io.Serializable;

/**
 * @Date 2017/5/11
 * @Author wenzheng.liu
 * @Description 命令枚举
 * @ClassPath com.fitpolo.support.entity.OrderEnum
 */
public enum OrderEnum implements Serializable {
    OPEN_NOTIFY("打开设备通知", 0),
    READ_CONNECT_STATUS("读取连接状态", 0x11),
    READ_REGION("读取Region", 0x0D),
    READ_CLASS_TYPE("读取Class类型", 0x0E),
    READ_UPLOAD_MODE("读取上传模式", 0x0F),
    READ_COMPANY_NAME("读取公司名称", 0x02),
    READ_MANUFACTURE_DATE("读取生产日期", 0x03),
    READ_MODEL_NAME("读取设备名称", 0x04),
    READ_BLE_FIRMWARE("读取固件版本", 0x05),
    READ_LORA_FIRMWARE("读取Lora固件版本", 0x06),
    READ_GPS("读取GPS数据", 0x16),
    READ_9_AXIS_A("读取9轴A数据", 0x17),
    READ_9_AXIS_G("读取9轴G数据", 0x18),
    READ_9_AXIS_M("读取9轴M数据", 0x19),
    READ_9_AXIS_ANGLE("读取9轴角度数据", 0x1A),
    WRITE_UPLINK_DATA_TEST("设置上行测试数据", 0xBB),
    ;


    private String orderName;
    private int orderHeader;

    OrderEnum(String orderName, int orderHeader) {
        this.orderName = orderName;
        this.orderHeader = orderHeader;
    }

    public int getOrderHeader() {
        return orderHeader;
    }

    public String getOrderName() {
        return orderName;
    }
}
