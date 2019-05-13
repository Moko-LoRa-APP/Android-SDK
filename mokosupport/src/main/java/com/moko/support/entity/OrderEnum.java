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
    READ_DEV_ADDR("读取DevAddr", 0x07),
    READ_NWK_SKEY("读取读取NwkSKey", 0x08),
    READ_APP_SKEY("读取AppSKey", 0x09),
    READ_DEV_EUI("读取DevEUI", 0x0A),
    READ_APP_EUI("读取AppEUI", 0x0B),
    READ_APP_KEY("读取AppKey", 0x0C),
    READ_UPLOAD_INTERVAL("读取上传间隔", 0x10),
    READ_CH("读取CH", 0x12),
    READ_DR("读取DR", 0x13),
    READ_POWER("读取Power", 0x14),
    READ_ADR("读取ADR状态", 0x15),

    WRITE_UPLINK_DATA_TEST("设置上行测试数据", 0xBB),
    WRITE_DEV_ADDR("设置DevAddr", 0x37),
    WRITE_NWK_SKEY("设置NwkSKey", 0x38),
    WRITE_APP_SKEY("设置AppSKey", 0x39),
    WRITE_DEV_EUI("设置DevEUI", 0x3A),
    WRITE_APP_EUI("设置AppEUI", 0x3B),
    WRITE_APP_KEY("设置AppKey", 0x3C),
    WRITE_REGION("设置Region", 0x3D),
    WRITE_CLASS_TYPE("设置Class类型", 0x3E),
    WRITE_UPLOAD_MODE("设置上传模式", 0x3F),
    WRITE_UPLOAD_INTERVAL("设置上传间隔", 0x40),
    WRITE_CH("设置CH", 0x42),
    WRITE_DR("设置DR", 0x43),
    WRITE_POWER("设置Power", 0x44),
    WRITE_ADR("设置ADR", 0x45),
    WRITE_CONNECT("设置连接", 0x50),
    WRITE_RESET("设置复位", 0x51),
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
