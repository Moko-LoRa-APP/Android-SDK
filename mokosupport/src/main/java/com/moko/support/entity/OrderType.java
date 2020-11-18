package com.moko.support.entity;

import java.io.Serializable;

/**
 * @Date 2017/12/14 0014
 * @Author wenzheng.liu
 * @Description
 * @ClassPath com.moko.support.entity.OrderType
 */
public enum OrderType implements Serializable {
    CHARACTERISTIC("CHARACTERISTIC", "0000ff01-0000-1000-8000-00805f9b34fb"),
    CHARACTERISTIC_LOG("CHARACTERISTIC_LOG", "0000ff02-0000-1000-8000-00805f9b34fb"),
    CHARACTERISTIC_PERIPHERAL("CHARACTERISTIC_PERIPHERAL", "0000ff03-0000-1000-8000-00805f9b34fb"),
    CHARACTERISTIC_MCU("CHARACTERISTIC_MCU", "0000ff04-0000-1000-8000-00805f9b34fb"),
    CHARACTERISTIC_NOTIFY("CHARACTERISTIC_MCU", "0000ff05-0000-1000-8000-00805f9b34fb"),
    ;


    private String uuid;
    private String name;

    OrderType(String name, String uuid) {
        this.name = name;
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }
}
