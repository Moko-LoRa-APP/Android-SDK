package com.moko.lorawan.utils;

import com.moko.support.callback.MokoOrderTaskCallback;
import com.moko.support.task.OrderTask;
import com.moko.support.task.ReadBleFirmwareTask;
import com.moko.support.task.ReadClassTypeTask;
import com.moko.support.task.ReadCompanyNameTask;
import com.moko.support.task.ReadConnectStatusTask;
import com.moko.support.task.ReadLoraFirmwareTask;
import com.moko.support.task.ReadManufactureDateTask;
import com.moko.support.task.ReadModelNameTask;
import com.moko.support.task.ReadRegionTask;
import com.moko.support.task.ReadUploadModeTask;

import java.util.ArrayList;

public class OrderTaskCreator {

    /**
     * 获取基本内心戏
     */
    public static OrderTask[] getBasicInfo(MokoOrderTaskCallback callback) {
        ArrayList<OrderTask> orderTasks = new ArrayList<>();
        orderTasks.add(new ReadConnectStatusTask(callback));
        orderTasks.add(new ReadRegionTask(callback));
        orderTasks.add(new ReadClassTypeTask(callback));
        orderTasks.add(new ReadUploadModeTask(callback));
        return orderTasks.toArray(new OrderTask[]{});
    }

    public static OrderTask[] getDeviceInfo(MokoOrderTaskCallback callback) {
        ArrayList<OrderTask> orderTasks = new ArrayList<>();
        orderTasks.add(new ReadCompanyNameTask(callback));
        orderTasks.add(new ReadManufactureDateTask(callback));
        orderTasks.add(new ReadModelNameTask(callback));
        orderTasks.add(new ReadBleFirmwareTask(callback));
        orderTasks.add(new ReadLoraFirmwareTask(callback));
        return orderTasks.toArray(new OrderTask[]{});
    }
}

