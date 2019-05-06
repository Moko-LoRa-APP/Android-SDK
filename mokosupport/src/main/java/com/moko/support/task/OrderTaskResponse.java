package com.moko.support.task;

import com.moko.support.entity.OrderEnum;

import java.io.Serializable;

/**
 * @Date 2018/1/23
 * @Author wenzheng.liu
 * @Description 任务反馈
 * @ClassPath com.moko.support.task.OrderTaskResponse
 */
public class OrderTaskResponse implements Serializable {
    public OrderEnum order;
    public int responseType;
    public byte[] responseValue;
}
