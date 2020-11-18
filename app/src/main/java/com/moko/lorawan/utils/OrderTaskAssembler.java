package com.moko.lorawan.utils;

import com.moko.support.task.OrderTask;
import com.moko.support.task.UpgradeMCUDetailTask;
import com.moko.support.task.UpgradeMCUTask;
import com.moko.support.task.VerifyPasswordTask;
import com.moko.support.task.WriteADRTask;
import com.moko.support.task.WriteAlarmGPSSwitchTask;
import com.moko.support.task.WriteAlarmSatelliteSearchTimeTask;
import com.moko.support.task.WriteAlarmStatusTask;
import com.moko.support.task.WriteAlarmTriggerModeTask;
import com.moko.support.task.WriteAlarmUploadIntervalTask;
import com.moko.support.task.WriteAlarmVibrationSwitchTask;
import com.moko.support.task.WriteAppEUITask;
import com.moko.support.task.WriteAppKeyTask;
import com.moko.support.task.WriteAppSKeyTask;
import com.moko.support.task.WriteBleOpeningTimeTask;
import com.moko.support.task.WriteCHTask;
import com.moko.support.task.WriteClassTypeTask;
import com.moko.support.task.WriteConnectTask;
import com.moko.support.task.WriteDRTask;
import com.moko.support.task.WriteDevAddrTask;
import com.moko.support.task.WriteDevEUITask;
import com.moko.support.task.WriteFilterNameTask;
import com.moko.support.task.WriteFilterRSSITask;
import com.moko.support.task.WriteHumiDataTask;
import com.moko.support.task.WriteI2CIntervalTask;
import com.moko.support.task.WriteMsgTypeTask;
import com.moko.support.task.WriteMulticastAddrTask;
import com.moko.support.task.WriteMulticastAppSKeyTask;
import com.moko.support.task.WriteMulticastNwkSKeyTask;
import com.moko.support.task.WriteMulticastSwitchTask;
import com.moko.support.task.WriteNwkSKeyTask;
import com.moko.support.task.WritePowerTask;
import com.moko.support.task.WriteRegionTask;
import com.moko.support.task.WriteResetTask;
import com.moko.support.task.WriteScanSwitchTask;
import com.moko.support.task.WriteScanUploadIntervalTask;
import com.moko.support.task.WriteTempDataTask;
import com.moko.support.task.WriteUploadIntervalTask;
import com.moko.support.task.WriteUploadModeTask;

public class OrderTaskAssembler {

    public static OrderTask setDevAddrOrderTask(String devAddr) {
        WriteDevAddrTask orderTask = new WriteDevAddrTask();
        orderTask.setOrderData(devAddr);
        return orderTask;
    }

    public static OrderTask setNwkSKeyOrderTask(String nwkSkey) {
        WriteNwkSKeyTask orderTask = new WriteNwkSKeyTask();
        orderTask.setOrderData(nwkSkey);
        return orderTask;
    }

    public static OrderTask setAppSKeyOrderTask(String appSkey) {
        WriteAppSKeyTask orderTask = new WriteAppSKeyTask();
        orderTask.setOrderData(appSkey);
        return orderTask;
    }

    public static OrderTask setDevEUIOrderTask(String devEUI) {
        WriteDevEUITask orderTask = new WriteDevEUITask();
        orderTask.setOrderData(devEUI);
        return orderTask;
    }

    public static OrderTask setAppEUIOrderTask(String appEUI) {
        WriteAppEUITask orderTask = new WriteAppEUITask();
        orderTask.setOrderData(appEUI);
        return orderTask;
    }

    public static OrderTask setAppKeyOrderTask(String appKey) {
        WriteAppKeyTask orderTask = new WriteAppKeyTask();
        orderTask.setOrderData(appKey);
        return orderTask;
    }

    public static OrderTask setRegionOrderTask(int region) {
        WriteRegionTask orderTask = new WriteRegionTask();
        orderTask.setOrderData(region);
        return orderTask;
    }

    public static OrderTask setClassTypeOrderTask(int classType) {
        WriteClassTypeTask orderTask = new WriteClassTypeTask();
        orderTask.setOrderData(classType);
        return orderTask;
    }

    public static OrderTask setMsgTypeOrderTask(int msgType) {
        WriteMsgTypeTask orderTask = new WriteMsgTypeTask();
        orderTask.setOrderData(msgType);
        return orderTask;
    }

    public static OrderTask setUploadModeOrderTask(int uploadMode) {
        WriteUploadModeTask orderTask = new WriteUploadModeTask();
        orderTask.setOrderData(uploadMode);
        return orderTask;
    }

    public static OrderTask setUploadIntervalOrderTask(int uploadInterval) {
        WriteUploadIntervalTask orderTask = new WriteUploadIntervalTask();
        orderTask.setOrderData(uploadInterval);
        return orderTask;
    }

    public static OrderTask setScanUploadIntervalOrderTask(int uploadInterval) {
        WriteScanUploadIntervalTask orderTask = new WriteScanUploadIntervalTask();
        orderTask.setOrderData(uploadInterval);
        return orderTask;
    }

    public static OrderTask setBleOpeningTimeOrderTask(int bleOpeningTime) {
        WriteBleOpeningTimeTask orderTask = new WriteBleOpeningTimeTask();
        orderTask.setOrderData(bleOpeningTime);
        return orderTask;
    }

    public static OrderTask setCHOrderTask(int ch1, int ch2) {
        WriteCHTask orderTask = new WriteCHTask();
        orderTask.setOrderData(ch1, ch2);
        return orderTask;
    }

    public static OrderTask setDROrderTask(int dr1, int dr2) {
        WriteDRTask orderTask = new WriteDRTask();
        orderTask.setOrderData(dr1, dr2);
        return orderTask;
    }

    public static OrderTask setPowerOrderTask(int power) {
        WritePowerTask orderTask = new WritePowerTask();
        orderTask.setOrderData(power);
        return orderTask;
    }

    public static OrderTask setADROrderTask(int dar) {
        WriteADRTask orderTask = new WriteADRTask();
        orderTask.setOrderData(dar);
        return orderTask;
    }

    public static OrderTask setConnectOrderTask() {
        WriteConnectTask orderTask = new WriteConnectTask();
        return orderTask;
    }

    public static OrderTask setResetOrderTask() {
        WriteResetTask orderTask = new WriteResetTask();
        return orderTask;
    }

    public static OrderTask setI2CIntervalOrderTask(int i2cInterval) {
        WriteI2CIntervalTask orderTask = new WriteI2CIntervalTask();
        orderTask.setOrderData(i2cInterval);
        return orderTask;
    }

    public static OrderTask setTempDataOrderTask(int onoff, int tempLow, int tempHigh) {
        WriteTempDataTask orderTask = new WriteTempDataTask();
        orderTask.setOrderData(onoff, tempLow, tempHigh);
        return orderTask;
    }

    public static OrderTask setHumiDataOrderTask(int onoff, int humiLow, int humiHigh) {
        WriteHumiDataTask orderTask = new WriteHumiDataTask();
        orderTask.setOrderData(onoff, humiLow, humiHigh);
        return orderTask;
    }

    public static OrderTask setFilterNameOrderTask(String filterName) {
        WriteFilterNameTask orderTask = new WriteFilterNameTask();
        orderTask.setOrderData(filterName);
        return orderTask;
    }

    public static OrderTask setFilterRssiOrderTask(int filterRsssi) {
        WriteFilterRSSITask orderTask = new WriteFilterRSSITask();
        orderTask.setOrderData(filterRsssi);
        return orderTask;
    }


    public static OrderTask setScanSwitchOrderTask(int scanSwitch) {
        WriteScanSwitchTask orderTask = new WriteScanSwitchTask();
        orderTask.setOrderData(scanSwitch);
        return orderTask;
    }

    public static OrderTask setUpgradeMCUOrderTask(byte[] indexCount, byte[] fileCount) {
        UpgradeMCUTask orderTask = new UpgradeMCUTask();
        orderTask.setOrderData(indexCount, fileCount);
        return orderTask;
    }

    public static OrderTask setUpgradeMCUDetailOrderTask(byte[] packageIndex, byte[] fileBytes) {
        UpgradeMCUDetailTask orderTask = new UpgradeMCUDetailTask(packageIndex, fileBytes);
        return orderTask;
    }

    public static OrderTask setMulticastSwitchOrderTask(int multicastSwitch) {
        WriteMulticastSwitchTask orderTask = new WriteMulticastSwitchTask();
        orderTask.setOrderData(multicastSwitch);
        return orderTask;
    }

    public static OrderTask setMulticastAddrOrderTask(String multicastAddr) {
        WriteMulticastAddrTask orderTask = new WriteMulticastAddrTask();
        orderTask.setOrderData(multicastAddr);
        return orderTask;
    }

    public static OrderTask setMulticastNwkSKeyOrderTask(String multicastNwkSkey) {
        WriteMulticastNwkSKeyTask orderTask = new WriteMulticastNwkSKeyTask();
        orderTask.setOrderData(multicastNwkSkey);
        return orderTask;
    }

    public static OrderTask setMulticastAppSKeyOrderTask(String multicastAppSkey) {
        WriteMulticastAppSKeyTask orderTask = new WriteMulticastAppSKeyTask();
        orderTask.setOrderData(multicastAppSkey);
        return orderTask;
    }

    public static OrderTask setAlarmStatusOrderTask(int alarmStatus) {
        WriteAlarmStatusTask orderTask = new WriteAlarmStatusTask();
        orderTask.setOrderData(alarmStatus);
        return orderTask;
    }

    public static OrderTask setAlarmGPSSwitchOrderTask(int gpsSwitch) {
        WriteAlarmGPSSwitchTask orderTask = new WriteAlarmGPSSwitchTask();
        orderTask.setOrderData(gpsSwitch);
        return orderTask;
    }

    public static OrderTask setAlarmVibrationSwitchOrderTask(int vibrationSwitch) {
        WriteAlarmVibrationSwitchTask orderTask = new WriteAlarmVibrationSwitchTask();
        orderTask.setOrderData(vibrationSwitch);
        return orderTask;
    }

    public static OrderTask setAlarmTriggerModeOrderTask(int triggerMode) {
        WriteAlarmTriggerModeTask orderTask = new WriteAlarmTriggerModeTask();
        orderTask.setOrderData(triggerMode);
        return orderTask;
    }

    public static OrderTask setAlarmUploadIntervalOrderTask(int uploadInterval) {
        WriteAlarmUploadIntervalTask orderTask = new WriteAlarmUploadIntervalTask();
        orderTask.setOrderData(uploadInterval);
        return orderTask;
    }

    public static OrderTask setAlarmSatelliteSearchTimeOrderTask(int searchTime) {
        WriteAlarmSatelliteSearchTimeTask orderTask = new WriteAlarmSatelliteSearchTimeTask();
        orderTask.setOrderData(searchTime);
        return orderTask;
    }

    public static OrderTask verifyPassword(String password) {
        VerifyPasswordTask verifyPasswordTask = new VerifyPasswordTask();
        verifyPasswordTask.setData(password);
        return verifyPasswordTask;
    }
}

