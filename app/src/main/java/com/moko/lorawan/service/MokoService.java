package com.moko.lorawan.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;

import com.moko.lorawan.utils.OrderTaskCreator;
import com.moko.support.MokoConstants;
import com.moko.support.MokoSupport;
import com.moko.support.callback.MokoOrderTaskCallback;
import com.moko.support.handler.BaseMessageHandler;
import com.moko.support.log.LogModule;
import com.moko.support.task.OrderTask;
import com.moko.support.task.OrderTaskResponse;
import com.moko.support.task.UpgradeMCUDetailTask;
import com.moko.support.task.UpgradeMCUTask;
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
import com.moko.support.task.WriteUplinkDataTestTask;
import com.moko.support.task.WriteUploadIntervalTask;
import com.moko.support.task.WriteUploadModeTask;

import java.util.Calendar;


/**
 * @Date 2017/12/7 0007
 * @Author wenzheng.liu
 * @Description
 * @ClassPath com.moko.lorawan.service.MokoService
 */
public class MokoService extends Service implements MokoOrderTaskCallback {

    @Override
    public void onOrderResult(OrderTaskResponse response) {}

    @Override
    public void onOrderTimeout(OrderTaskResponse response) {}

    @Override
    public void onOrderFinish() {}

    @Override
    public void onCreate() {
        LogModule.v("创建MokoService...onCreate");
        mHandler = new ServiceHandler(this);
        super.onCreate();
    }

    public void connectBluetoothDevice(String address) {
        MokoSupport.getInstance().connDevice(this, address);
    }

    /**
     * @Date 2017/5/23
     * @Author wenzheng.liu
     * @Description 断开手环
     */
    public void disConnectBle() {
        MokoSupport.getInstance().disConnectBle();
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogModule.v("启动MokoService...onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    private IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        LogModule.v("绑定MokoService...onBind");
        return mBinder;
    }

    @Override
    public void onLowMemory() {
        LogModule.v("内存吃紧，销毁MokoService...onLowMemory");
        disConnectBle();
        super.onLowMemory();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        LogModule.v("解绑MokoService...onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        LogModule.v("销毁MokoService...onDestroy");
        disConnectBle();
        super.onDestroy();
    }

    public class LocalBinder extends Binder {
        public MokoService getService() {
            return MokoService.this;
        }
    }

    public ServiceHandler mHandler;

    public class ServiceHandler extends BaseMessageHandler<MokoService> {

        public ServiceHandler(MokoService service) {
            super(service);
        }

        @Override
        protected void handleMessage(MokoService service, Message msg) {
        }
    }

    public void openNotify() {
        MokoSupport.getInstance().sendOrder(OrderTaskCreator.openNotify());
    }

    public void getBasicInfo() {
        MokoSupport.getInstance().sendOrder(OrderTaskCreator.getBasicInfo(this));
    }


    public void getDeviceInfo() {
        MokoSupport.getInstance().sendOrder(OrderTaskCreator.getDeviceInfo(this));
    }

    public void getGPSAndSensorData() {
        MokoSupport.getInstance().sendOrder(OrderTaskCreator.getGPSAndSensor(this));
    }

    public void getDeviceSettingType() {
        MokoSupport.getInstance().sendOrder(OrderTaskCreator.getDeviceSettingType(this));
    }

    public void getDeviceSetting() {
        MokoSupport.getInstance().sendOrder(OrderTaskCreator.getDeviceSetting(this));
    }

    public void getBleInfo() {
        MokoSupport.getInstance().sendOrder(OrderTaskCreator.getBleInfo(this));
    }

    public void getScanSetting() {
        MokoSupport.getInstance().sendOrder(OrderTaskCreator.getScanSetting(this));
    }

    public void getMulticastSetting() {
        MokoSupport.getInstance().sendOrder(OrderTaskCreator.getMulticastSetting(this));
    }

    public void getAlarmSetting() {
        MokoSupport.getInstance().sendOrder(OrderTaskCreator.getAlarmSetting(this));
    }

    public void getGPSSetting() {
        MokoSupport.getInstance().sendOrder(OrderTaskCreator.getGPSSetting(this));
    }

    public void getCHDR() {
        MokoSupport.getInstance().sendOrder(OrderTaskCreator.getCHDR(this));
    }

    public void getSensorData() {
        MokoSupport.getInstance().sendOrder(OrderTaskCreator.getSensorData(this));
    }


    public void setUplinkDataTest(Calendar calendar) {
        MokoSupport.getInstance().sendOrder(new WriteUplinkDataTestTask(this, calendar));
    }

    public OrderTask getDevAddrOrderTask(String devAddr) {
        WriteDevAddrTask orderTask = new WriteDevAddrTask(this);
        orderTask.setOrderData(devAddr);
        return orderTask;
    }

    public OrderTask getNwkSKeyOrderTask(String nwkSkey) {
        WriteNwkSKeyTask orderTask = new WriteNwkSKeyTask(this);
        orderTask.setOrderData(nwkSkey);
        return orderTask;
    }

    public OrderTask getAppSKeyOrderTask(String appSkey) {
        WriteAppSKeyTask orderTask = new WriteAppSKeyTask(this);
        orderTask.setOrderData(appSkey);
        return orderTask;
    }

    public OrderTask getDevEUIOrderTask(String devEUI) {
        WriteDevEUITask orderTask = new WriteDevEUITask(this);
        orderTask.setOrderData(devEUI);
        return orderTask;
    }

    public OrderTask getAppEUIOrderTask(String appEUI) {
        WriteAppEUITask orderTask = new WriteAppEUITask(this);
        orderTask.setOrderData(appEUI);
        return orderTask;
    }

    public OrderTask getAppKeyOrderTask(String appKey) {
        WriteAppKeyTask orderTask = new WriteAppKeyTask(this);
        orderTask.setOrderData(appKey);
        return orderTask;
    }

    public OrderTask getRegionOrderTask(int region) {
        WriteRegionTask orderTask = new WriteRegionTask(this);
        orderTask.setOrderData(region);
        return orderTask;
    }

    public OrderTask getClassTypeOrderTask(int classType) {
        WriteClassTypeTask orderTask = new WriteClassTypeTask(this);
        orderTask.setOrderData(classType);
        return orderTask;
    }

    public OrderTask getMsgTypeOrderTask(int msgType) {
        WriteMsgTypeTask orderTask = new WriteMsgTypeTask(this);
        orderTask.setOrderData(msgType);
        return orderTask;
    }

    public OrderTask getUploadModeOrderTask(int uploadMode) {
        WriteUploadModeTask orderTask = new WriteUploadModeTask(this);
        orderTask.setOrderData(uploadMode);
        return orderTask;
    }

    public OrderTask getUploadIntervalOrderTask(int uploadInterval) {
        WriteUploadIntervalTask orderTask = new WriteUploadIntervalTask(this);
        orderTask.setOrderData(uploadInterval);
        return orderTask;
    }

    public OrderTask getScanUploadIntervalOrderTask(int uploadInterval) {
        WriteScanUploadIntervalTask orderTask = new WriteScanUploadIntervalTask(this);
        orderTask.setOrderData(uploadInterval);
        return orderTask;
    }

    public OrderTask getBleOpeningTimeOrderTask(int bleOpeningTime) {
        WriteBleOpeningTimeTask orderTask = new WriteBleOpeningTimeTask(this);
        orderTask.setOrderData(bleOpeningTime);
        return orderTask;
    }

    public OrderTask getCHOrderTask(int ch1, int ch2) {
        WriteCHTask orderTask = new WriteCHTask(this);
        orderTask.setOrderData(ch1, ch2);
        return orderTask;
    }

    public OrderTask getDROrderTask(int dr1, int dr2) {
        WriteDRTask orderTask = new WriteDRTask(this);
        orderTask.setOrderData(dr1, dr2);
        return orderTask;
    }

    public OrderTask getPowerOrderTask(int power) {
        WritePowerTask orderTask = new WritePowerTask(this);
        orderTask.setOrderData(power);
        return orderTask;
    }

    public OrderTask getADROrderTask(int dar) {
        WriteADRTask orderTask = new WriteADRTask(this);
        orderTask.setOrderData(dar);
        return orderTask;
    }

    public OrderTask getConnectOrderTask() {
        WriteConnectTask orderTask = new WriteConnectTask(this);
        return orderTask;
    }

    public OrderTask getResetOrderTask() {
        WriteResetTask orderTask = new WriteResetTask(this);
        return orderTask;
    }

    public OrderTask getI2CIntervalOrderTask(int i2cInterval) {
        WriteI2CIntervalTask orderTask = new WriteI2CIntervalTask(this);
        orderTask.setOrderData(i2cInterval);
        return orderTask;
    }

    public OrderTask getTempDataOrderTask(int onoff, int tempLow, int tempHigh) {
        WriteTempDataTask orderTask = new WriteTempDataTask(this);
        orderTask.setOrderData(onoff, tempLow, tempHigh);
        return orderTask;
    }

    public OrderTask getHumiDataOrderTask(int onoff, int humiLow, int humiHigh) {
        WriteHumiDataTask orderTask = new WriteHumiDataTask(this);
        orderTask.setOrderData(onoff, humiLow, humiHigh);
        return orderTask;
    }

    public OrderTask getFilterNameOrderTask(String filterName) {
        WriteFilterNameTask orderTask = new WriteFilterNameTask(this);
        orderTask.setOrderData(filterName);
        return orderTask;
    }

    public OrderTask getFilterRssiOrderTask(int filterRsssi) {
        WriteFilterRSSITask orderTask = new WriteFilterRSSITask(this);
        orderTask.setOrderData(filterRsssi);
        return orderTask;
    }


    public OrderTask getScanSwitchOrderTask(int scanSwitch) {
        WriteScanSwitchTask orderTask = new WriteScanSwitchTask(this);
        orderTask.setOrderData(scanSwitch);
        return orderTask;
    }

    public OrderTask getUpgradeMCUOrderTask(byte[] indexCount, byte[] fileCount) {
        UpgradeMCUTask orderTask = new UpgradeMCUTask(this);
        orderTask.setOrderData(indexCount, fileCount);
        return orderTask;
    }

    public OrderTask getUpgradeMCUDetailOrderTask(byte[] packageIndex, byte[] fileBytes) {
        UpgradeMCUDetailTask orderTask = new UpgradeMCUDetailTask(this, packageIndex, fileBytes);
        return orderTask;
    }

    public OrderTask getMulticastSwitchOrderTask(int multicastSwitch) {
        WriteMulticastSwitchTask orderTask = new WriteMulticastSwitchTask(this);
        orderTask.setOrderData(multicastSwitch);
        return orderTask;
    }

    public OrderTask getMulticastAddrOrderTask(String multicastAddr) {
        WriteMulticastAddrTask orderTask = new WriteMulticastAddrTask(this);
        orderTask.setOrderData(multicastAddr);
        return orderTask;
    }

    public OrderTask getMulticastNwkSKeyOrderTask(String multicastNwkSkey) {
        WriteMulticastNwkSKeyTask orderTask = new WriteMulticastNwkSKeyTask(this);
        orderTask.setOrderData(multicastNwkSkey);
        return orderTask;
    }

    public OrderTask getMulticastAppSKeyOrderTask(String multicastAppSkey) {
        WriteMulticastAppSKeyTask orderTask = new WriteMulticastAppSKeyTask(this);
        orderTask.setOrderData(multicastAppSkey);
        return orderTask;
    }

    public OrderTask getAlarmStatusOrderTask(int alarmStatus) {
        WriteAlarmStatusTask orderTask = new WriteAlarmStatusTask(this);
        orderTask.setOrderData(alarmStatus);
        return orderTask;
    }

    public OrderTask getAlarmGPSSwitchOrderTask(int gpsSwitch) {
        WriteAlarmGPSSwitchTask orderTask = new WriteAlarmGPSSwitchTask(this);
        orderTask.setOrderData(gpsSwitch);
        return orderTask;
    }

    public OrderTask getAlarmVibrationSwitchOrderTask(int vibrationSwitch) {
        WriteAlarmVibrationSwitchTask orderTask = new WriteAlarmVibrationSwitchTask(this);
        orderTask.setOrderData(vibrationSwitch);
        return orderTask;
    }

    public OrderTask getAlarmTriggerModeOrderTask(int triggerMode) {
        WriteAlarmTriggerModeTask orderTask = new WriteAlarmTriggerModeTask(this);
        orderTask.setOrderData(triggerMode);
        return orderTask;
    }

    public OrderTask getAlarmUploadIntervalOrderTask(int uploadInterval) {
        WriteAlarmUploadIntervalTask orderTask = new WriteAlarmUploadIntervalTask(this);
        orderTask.setOrderData(uploadInterval);
        return orderTask;
    }

    public OrderTask getAlarmSatelliteSearchTimeOrderTask(int searchTime) {
        WriteAlarmSatelliteSearchTimeTask orderTask = new WriteAlarmSatelliteSearchTimeTask(this);
        orderTask.setOrderData(searchTime);
        return orderTask;
    }
}
