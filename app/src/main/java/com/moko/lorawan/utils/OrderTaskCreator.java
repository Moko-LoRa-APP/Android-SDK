package com.moko.lorawan.utils;

import com.moko.support.callback.MokoOrderTaskCallback;
import com.moko.support.task.OrderTask;
import com.moko.support.task.Read9AxisATask;
import com.moko.support.task.Read9AxisAngleTask;
import com.moko.support.task.Read9AxisGTask;
import com.moko.support.task.Read9AxisMTask;
import com.moko.support.task.ReadADRTask;
import com.moko.support.task.ReadAppEUITask;
import com.moko.support.task.ReadAppKeyTask;
import com.moko.support.task.ReadAppSKeyTask;
import com.moko.support.task.ReadBleFirmwareTask;
import com.moko.support.task.ReadCHTask;
import com.moko.support.task.ReadClassTypeTask;
import com.moko.support.task.ReadCompanyNameTask;
import com.moko.support.task.ReadConnectStatusTask;
import com.moko.support.task.ReadDRTask;
import com.moko.support.task.ReadDevAddrTask;
import com.moko.support.task.ReadDevEUITask;
import com.moko.support.task.ReadGPSTask;
import com.moko.support.task.ReadLoraFirmwareTask;
import com.moko.support.task.ReadManufactureDateTask;
import com.moko.support.task.ReadModelNameTask;
import com.moko.support.task.ReadNwkSKeyTask;
import com.moko.support.task.ReadPowerTask;
import com.moko.support.task.ReadRegionTask;
import com.moko.support.task.ReadUploadIntervalTask;
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

    public static OrderTask[] getGPSAndSensor(MokoOrderTaskCallback callback) {
        ArrayList<OrderTask> orderTasks = new ArrayList<>();
        orderTasks.add(new ReadGPSTask(callback));
        orderTasks.add(new Read9AxisATask(callback));
        orderTasks.add(new Read9AxisGTask(callback));
        orderTasks.add(new Read9AxisMTask(callback));
        orderTasks.add(new Read9AxisAngleTask(callback));
        return orderTasks.toArray(new OrderTask[]{});
    }

    public static OrderTask[] getDeviceSetting(MokoOrderTaskCallback callback) {
        ArrayList<OrderTask> orderTasks = new ArrayList<>();
        orderTasks.add(new ReadDevEUITask(callback));
        orderTasks.add(new ReadAppEUITask(callback));
        orderTasks.add(new ReadAppKeyTask(callback));
        orderTasks.add(new ReadDevAddrTask(callback));
        orderTasks.add(new ReadNwkSKeyTask(callback));
        orderTasks.add(new ReadAppSKeyTask(callback));
        orderTasks.add(new ReadUploadIntervalTask(callback));
        orderTasks.add(new ReadCHTask(callback));
        orderTasks.add(new ReadDRTask(callback));
        orderTasks.add(new ReadPowerTask(callback));
        orderTasks.add(new ReadADRTask(callback));
        return orderTasks.toArray(new OrderTask[]{});
    }
}

