package com.moko.lorawan.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;

import com.moko.lorawan.utils.OrderTaskCreator;
import com.moko.support.MokoConstants;
import com.moko.support.MokoSupport;
import com.moko.support.callback.MokoConnStateCallback;
import com.moko.support.callback.MokoOrderTaskCallback;
import com.moko.support.event.ConnectStatusEvent;
import com.moko.support.handler.BaseMessageHandler;
import com.moko.support.log.LogModule;
import com.moko.support.task.OrderTask;
import com.moko.support.task.OrderTaskResponse;
import com.moko.support.task.UpgradeMCUDetailTask;
import com.moko.support.task.UpgradeMCUTask;
import com.moko.support.task.WriteADRTask;
import com.moko.support.task.WriteAppEUITask;
import com.moko.support.task.WriteAppKeyTask;
import com.moko.support.task.WriteAppSKeyTask;
import com.moko.support.task.WriteCHTask;
import com.moko.support.task.WriteClassTypeTask;
import com.moko.support.task.WriteConnectTask;
import com.moko.support.task.WriteDRTask;
import com.moko.support.task.WriteDevAddrTask;
import com.moko.support.task.WriteDevEUITask;
import com.moko.support.task.WriteHumiDataTask;
import com.moko.support.task.WriteI2CIntervalTask;
import com.moko.support.task.WriteNwkSKeyTask;
import com.moko.support.task.WritePowerTask;
import com.moko.support.task.WriteRegionTask;
import com.moko.support.task.WriteResetTask;
import com.moko.support.task.WriteTempDataTask;
import com.moko.support.task.WriteUplinkDataTestTask;
import com.moko.support.task.WriteUploadIntervalTask;
import com.moko.support.task.WriteUploadModeTask;

import org.greenrobot.eventbus.EventBus;

import java.util.Calendar;


/**
 * @Date 2017/12/7 0007
 * @Author wenzheng.liu
 * @Description
 * @ClassPath com.moko.lorawan.service.MokoService
 */
public class MokoService extends Service implements MokoConnStateCallback, MokoOrderTaskCallback {

    @Override
    public void onConnectSuccess() {
        ConnectStatusEvent connectStatusEvent = new ConnectStatusEvent();
        connectStatusEvent.setAction(MokoConstants.ACTION_DISCOVER_SUCCESS);
        EventBus.getDefault().post(connectStatusEvent);
    }

    @Override
    public void onDisConnected() {
        ConnectStatusEvent connectStatusEvent = new ConnectStatusEvent();
        connectStatusEvent.setAction(MokoConstants.ACTION_CONN_STATUS_DISCONNECTED);
        EventBus.getDefault().post(connectStatusEvent);
    }

    @Override
    public void onOrderResult(OrderTaskResponse response) {
        Intent intent = new Intent(new Intent(MokoConstants.ACTION_ORDER_RESULT));
        intent.putExtra(MokoConstants.EXTRA_KEY_RESPONSE_ORDER_TASK, response);
        sendOrderedBroadcast(intent, null);
    }

    @Override
    public void onOrderTimeout(OrderTaskResponse response) {
        Intent intent = new Intent(new Intent(MokoConstants.ACTION_ORDER_TIMEOUT));
        intent.putExtra(MokoConstants.EXTRA_KEY_RESPONSE_ORDER_TASK, response);
        sendOrderedBroadcast(intent, null);
    }

    @Override
    public void onOrderFinish() {
        sendOrderedBroadcast(new Intent(MokoConstants.ACTION_ORDER_FINISH), null);
    }

    @Override
    public void onCreate() {
        LogModule.v("创建MokoService...onCreate");
        mHandler = new ServiceHandler(this);
        super.onCreate();
    }

    public void connectBluetoothDevice(String address) {
        MokoSupport.getInstance().connDevice(this, address, this);
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

    public void getBasicInfo() {
        MokoSupport.getInstance().sendOrder(OrderTaskCreator.getBasicInfo(this));
    }


    public void getDeviceInfo() {
        MokoSupport.getInstance().sendOrder(OrderTaskCreator.getDeviceInfo(this));
    }

    public void getGPSAndSensorData() {
        MokoSupport.getInstance().sendOrder(OrderTaskCreator.getGPSAndSensor(this));
    }

    public void getDeviceSetting() {
        MokoSupport.getInstance().sendOrder(OrderTaskCreator.getDeviceSetting(this));
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

    public OrderTask getUpgradeMCUOrderTask(byte[] indexCount, byte[] fileCount) {
        UpgradeMCUTask orderTask = new UpgradeMCUTask(this);
        orderTask.setOrderData(indexCount, fileCount);
        return orderTask;
    }

    public OrderTask getUpgradeMCUDetailOrderTask(byte[] packageIndex, byte[] fileBytes) {
        UpgradeMCUDetailTask orderTask = new UpgradeMCUDetailTask(this, packageIndex, fileBytes);
        return orderTask;
    }
}
