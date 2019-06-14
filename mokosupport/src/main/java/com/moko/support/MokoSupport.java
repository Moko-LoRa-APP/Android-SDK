package com.moko.support;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Message;
import android.os.ParcelUuid;
import android.text.TextUtils;

import com.moko.support.callback.MokoConnStateCallback;
import com.moko.support.callback.MokoOrderTaskCallback;
import com.moko.support.callback.MokoResponseCallback;
import com.moko.support.callback.MokoScanDeviceCallback;
import com.moko.support.entity.MokoCharacteristic;
import com.moko.support.entity.OrderEnum;
import com.moko.support.entity.OrderType;
import com.moko.support.handler.BaseMessageHandler;
import com.moko.support.handler.MokoCharacteristicHandler;
import com.moko.support.handler.MokoConnStateHandler;
import com.moko.support.handler.MokoLeScanHandler;
import com.moko.support.log.LogModule;
import com.moko.support.task.OpenNotifyTask;
import com.moko.support.task.OrderTask;
import com.moko.support.utils.BleConnectionCompat;
import com.moko.support.utils.MokoUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

/**
 * @Date 2017/12/7 0007
 * @Author wenzheng.liu
 * @Description
 * @ClassPath com.moko.support.MokoSupport
 */
public class MokoSupport implements MokoResponseCallback {
    public static final int HANDLER_MESSAGE_WHAT_CONNECTED = 1;
    public static final int HANDLER_MESSAGE_WHAT_DISCONNECTED = 2;
    public static final int HANDLER_MESSAGE_WHAT_SERVICES_DISCOVERED = 3;
    public static final int HANDLER_MESSAGE_WHAT_DISCONNECT = 4;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private BlockingQueue<OrderTask> mQueue;

    private Context mContext;
    private MokoLeScanHandler mMokoLeScanHandler;
    private MokoScanDeviceCallback mMokoScanDeviceCallback;
    private MokoConnStateCallback mMokoConnStateCallback;
    private HashMap<OrderType, MokoCharacteristic> mCharacteristicMap;
    private static final UUID DESCRIPTOR_UUID_NOTIFY = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final UUID SERVICE_UUID = UUID.fromString("0000ffc3-0000-1000-8000-00805f9b34fb");

    private static volatile MokoSupport INSTANCE;

    private MokoSupport() {
        //no instance
        mQueue = new LinkedBlockingQueue<>();
    }

    public static MokoSupport getInstance() {
        if (INSTANCE == null) {
            synchronized (MokoSupport.class) {
                if (INSTANCE == null) {
                    INSTANCE = new MokoSupport();
                }
            }
        }
        return INSTANCE;
    }

    public void init(Context context) {
        LogModule.init(context);
        mContext = context;
        mHandler = new ServiceMessageHandler(this);
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
    }

    public void startScanDevice(MokoScanDeviceCallback mokoScanDeviceCallback) {
        LogModule.w("开始扫描");
        final BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                // Hardware filtering has some issues on selected devices
                .setUseHardwareFilteringIfSupported(false)
                .build();
        List<ScanFilter> scanFilterList = new ArrayList<>();
        ScanFilter.Builder builder = new ScanFilter.Builder();
        builder.setServiceUuid(new ParcelUuid(SERVICE_UUID));
        scanFilterList.add(builder.build());
        mMokoLeScanHandler = new MokoLeScanHandler(mokoScanDeviceCallback);
        scanner.startScan(scanFilterList, settings, mMokoLeScanHandler);
        mMokoScanDeviceCallback = mokoScanDeviceCallback;
        mokoScanDeviceCallback.onStartScan();
    }

    public void stopScanDevice() {
        if (isBluetoothOpen() && mMokoLeScanHandler != null && mMokoScanDeviceCallback != null) {
            LogModule.w("结束扫描");
            final BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
            scanner.stopScan(mMokoLeScanHandler);
            mMokoScanDeviceCallback.onStopScan();
            mMokoLeScanHandler = null;
            mMokoScanDeviceCallback = null;
        }
    }

    public void connDevice(final Context context, final String address, final MokoConnStateCallback mokoConnStateCallback) {
        setConnStateCallback(mokoConnStateCallback);
        if (TextUtils.isEmpty(address)) {
            LogModule.w("connDevice: 地址为空");
            return;
        }
        if (!isBluetoothOpen()) {
            LogModule.w("connDevice: 蓝牙未打开");
            return;
        }
        if (isConnDevice(context, address)) {
            LogModule.w("connDevice: 设备已连接");
            return;
        }
        final MokoConnStateHandler gattCallback = MokoConnStateHandler.getInstance();
        gattCallback.setMokoResponseCallback(this);
        gattCallback.setMessageHandler(mHandler);
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    LogModule.i("开始尝试连接");
                    mBluetoothGatt = (new BleConnectionCompat(context)).connectGatt(device, false, gattCallback);
                }
            });
        } else {
            LogModule.w("获取蓝牙设备失败");
        }
    }

    public void setConnStateCallback(final MokoConnStateCallback mokoConnStateCallback) {
        mHandler.setMokoConnStateCallback(mokoConnStateCallback);
        mMokoConnStateCallback = mokoConnStateCallback;
    }

    public void sendOrder(OrderTask... orderTasks) {
        if (orderTasks.length == 0) {
            return;
        }
        if (!isSyncData()) {
            for (OrderTask ordertask : orderTasks) {
                if (ordertask == null) {
                    continue;
                }
                mQueue.offer(ordertask);
            }
            executeTask(null);
        } else {
            for (OrderTask ordertask : orderTasks) {
                if (ordertask == null) {
                    continue;
                }
                mQueue.offer(ordertask);
            }
        }
    }

    /**
     * @param callback
     * @Date 2017/5/11
     * @Author wenzheng.liu
     * @Description 执行命令
     */
    public void executeTask(MokoOrderTaskCallback callback) {
        if (callback != null && !isSyncData()) {
            callback.onOrderFinish();
            return;
        }
        if (mQueue.isEmpty()) {
            return;
        }
        final OrderTask orderTask = mQueue.peek();
        if (mBluetoothGatt == null) {
            LogModule.i("executeTask : BluetoothGatt is null");
            return;
        }
        if (orderTask == null) {
            LogModule.i("executeTask : orderTask is null");
            return;
        }
        if (mCharacteristicMap == null || mCharacteristicMap.isEmpty()) {
            LogModule.i("executeTask : characteristicMap is null");
            disConnectBle();
            return;
        }
        final MokoCharacteristic mokoCharacteristic = mCharacteristicMap.get(orderTask.orderType);
        if (mokoCharacteristic == null) {
            LogModule.i("executeTask : mokoCharacteristic is null");
            return;
        }
        if (orderTask.response.responseType == OrderTask.RESPONSE_TYPE_READ) {
            sendReadOrder(orderTask, mokoCharacteristic);
        }
        if (orderTask.response.responseType == OrderTask.RESPONSE_TYPE_WRITE) {
            sendWriteOrder(orderTask, mokoCharacteristic);
        }
        if (orderTask.response.responseType == OrderTask.RESPONSE_TYPE_WRITE_NO_RESPONSE) {
            sendWriteNoResponseOrder(orderTask, mokoCharacteristic);
        }
        if (orderTask.response.responseType == OrderTask.RESPONSE_TYPE_NOTIFY) {
            sendNotifyOrder(orderTask, mokoCharacteristic);
        }
        timeoutHandler(orderTask);
    }

    /**
     * @Date 2017/5/10
     * @Author wenzheng.liu
     * @Description 是否连接设备
     */
    public boolean isConnDevice(Context context, String address) {
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        int connState = bluetoothManager.getConnectionState(mBluetoothAdapter.getRemoteDevice(address), BluetoothProfile.GATT);
        return connState == BluetoothProfile.STATE_CONNECTED;
    }

    public synchronized boolean isSyncData() {
        return mQueue != null && !mQueue.isEmpty();
    }

    /**
     * @Date 2017/12/12 0012
     * @Author wenzheng.liu
     * @Description 蓝牙是否打开
     */
    public boolean isBluetoothOpen() {
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
    }

    /**
     * @Date 2017/12/13 0013
     * @Author wenzheng.liu
     * @Description 断开连接
     */
    public void disConnectBle() {
        mHandler.sendEmptyMessage(MokoSupport.HANDLER_MESSAGE_WHAT_DISCONNECT);
    }

    public void enableBluetooth() {
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.enable();
        }
    }

    public void disableBluetooth() {
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.disable();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onCharacteristicChanged(BluetoothGattCharacteristic characteristic, byte[] value) {
        if (characteristic.getUuid().toString().equals(OrderType.CHARACTERISTIC.getUuid())) {
            if (isSyncData()) {
                // 非延时应答
                OrderTask orderTask = mQueue.peek();
                if (value != null && value.length > 0 && orderTask != null) {
                    orderTask.parseValue(value);
                }
            }
        } else if (characteristic.getUuid().toString().equals(OrderType.CHARACTERISTIC_LOG.getUuid())) {
            if (value != null && value.length > 0) {
                String log = new String(value);
                LogModule.d(log);
            }
        }

    }

    @Override
    public void onCharacteristicWrite(byte[] value) {

    }

    @Override
    public void onCharacteristicRead(byte[] value) {

    }

    @Override
    public void onDescriptorWrite() {
        if (!isSyncData()) {
            return;
        }
        OrderTask orderTask = mQueue.peek();
        LogModule.v("device to app NOTIFY : " + orderTask.orderType.getName());
        LogModule.i(orderTask.order.getOrderName());
        orderTask.orderStatus = OrderTask.ORDER_STATUS_SUCCESS;
        mQueue.poll();
        executeTask(orderTask.callback);
        if (mQueue.isEmpty()) {
            mMokoConnStateCallback.onConnectSuccess();
        }
    }

    private void formatCommonOrder(OrderTask task, byte[] value) {
        task.orderStatus = OrderTask.ORDER_STATUS_SUCCESS;
        task.response.responseValue = value;
        mQueue.poll();
        task.callback.onOrderResult(task.response);
        executeTask(task.callback);
    }

    public void onOpenNotifyTimeout() {
        if (!mQueue.isEmpty()) {
            mQueue.clear();
        }
        disConnectBle();
    }


    public void pollTask() {
        if (mQueue != null && !mQueue.isEmpty()) {
            OrderTask orderTask = mQueue.peek();
            LogModule.i("移除" + orderTask.order.getOrderName());
            mQueue.poll();
        }
    }

    public void timeoutHandler(OrderTask orderTask) {
        mHandler.postDelayed(orderTask.timeoutRunner, orderTask.delayTime);
    }


    ///////////////////////////////////////////////////////////////////////////
    // handler
    ///////////////////////////////////////////////////////////////////////////

    private ServiceMessageHandler mHandler;

    public class ServiceMessageHandler extends BaseMessageHandler<MokoSupport> {
        private MokoConnStateCallback mokoConnStateCallback;

        public ServiceMessageHandler(MokoSupport module) {
            super(module);
        }

        @Override
        protected void handleMessage(MokoSupport module, Message msg) {
            switch (msg.what) {
                case HANDLER_MESSAGE_WHAT_CONNECTED:
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            synchronized (INSTANCE) {
                                LogModule.e("discoverServices!!!");
                                mBluetoothGatt.discoverServices();
                            }
                        }
                    }, 2000);
                    break;
                case HANDLER_MESSAGE_WHAT_DISCONNECTED:
                    disConnectBle();
                    break;
                case HANDLER_MESSAGE_WHAT_SERVICES_DISCOVERED:
                    LogModule.i("连接成功！");
                    try {
                        synchronized (MokoSupport.class) {
                            mCharacteristicMap = MokoCharacteristicHandler.getInstance().getCharacteristics(mBluetoothGatt);
                        }
                        if (mCharacteristicMap == null || mCharacteristicMap.isEmpty()) {
                            LogModule.e("打开服务：特征为空！！！");
                            disConnectBle();
                            return;
                        }
                    } catch (Exception e) {
                        LogModule.e("打开服务：发生异常！！！");
                        LogModule.e(e.toString());
                        disConnectBle();
                        return;
                    }
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            LogModule.i("开启特征通知");
                            sendOrder(new OpenNotifyTask(OrderType.CHARACTERISTIC, OrderEnum.OPEN_NOTIFY, null));
                            sendOrder(new OpenNotifyTask(OrderType.CHARACTERISTIC_LOG, OrderEnum.OPEN_NOTIFY, null));
                        }
                    }, 2000);
                    break;
                case HANDLER_MESSAGE_WHAT_DISCONNECT:
                    if (mQueue != null && !mQueue.isEmpty()) {
                        mQueue.clear();
                    }
                    if (mBluetoothGatt != null) {
                        if (refreshDeviceCache()) {
                            LogModule.i("清理GATT层蓝牙缓存");
                        }
                        LogModule.e("断开连接");
                        mBluetoothGatt.close();
                        mBluetoothGatt.disconnect();
                        mMokoConnStateCallback.onDisConnected();
                    }
                    break;
            }
        }

        public void setMokoConnStateCallback(MokoConnStateCallback mokoConnStateCallback) {
            this.mokoConnStateCallback = mokoConnStateCallback;
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    ///////////////////////////////////////////////////////////////////////////

    // 发送可监听命令
    private void sendNotifyOrder(OrderTask orderTask, final MokoCharacteristic mokoCharacteristic) {
        LogModule.i("app set device notify : " + orderTask.orderType.getName());
        mokoCharacteristic.characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        final BluetoothGattDescriptor descriptor = mokoCharacteristic.characteristic.getDescriptor(DESCRIPTOR_UUID_NOTIFY);
        if (descriptor == null) {
            return;
        }
        if ((mokoCharacteristic.characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        } else if ((mokoCharacteristic.characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mBluetoothGatt.writeDescriptor(descriptor);
            }
        });
    }

    // 发送可写命令
    private void sendWriteOrder(OrderTask orderTask, final MokoCharacteristic mokoCharacteristic) {
        LogModule.i("app to device write : " + orderTask.orderType.getName());
        LogModule.i(MokoUtils.bytesToHexString(orderTask.assemble()));
        mokoCharacteristic.characteristic.setValue(orderTask.assemble());
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mBluetoothGatt.writeCharacteristic(mokoCharacteristic.characteristic);
            }
        });
    }

    // 发送可写无应答命令
    private void sendWriteNoResponseOrder(OrderTask orderTask, final MokoCharacteristic mokoCharacteristic) {
        LogModule.i("app to device write no response : " + orderTask.orderType.getName());
        LogModule.i(MokoUtils.bytesToHexString(orderTask.assemble()));
        mokoCharacteristic.characteristic.setValue(orderTask.assemble());
        mokoCharacteristic.characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mBluetoothGatt.writeCharacteristic(mokoCharacteristic.characteristic);
            }
        });
    }

    // 发送可读命令
    private void sendReadOrder(OrderTask orderTask, final MokoCharacteristic mokoCharacteristic) {
        LogModule.i("app to device read : " + orderTask.orderType.getName());
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mBluetoothGatt.readCharacteristic(mokoCharacteristic.characteristic);
            }
        });
    }

    // 发送自定义命令（无队列）
    public void sendCustomOrder(OrderTask orderTask) {
        final MokoCharacteristic mokoCharacteristic = mCharacteristicMap.get(orderTask.orderType);
        if (mokoCharacteristic == null) {
            LogModule.i("executeTask : mokoCharacteristic is null");
            return;
        }
        if (orderTask.response.responseType == OrderTask.RESPONSE_TYPE_WRITE_NO_RESPONSE) {
            sendWriteNoResponseOrder(orderTask, mokoCharacteristic);
        }
    }

    // 直接发送命令(升级专用)
    public void sendDirectOrder(OrderTask orderTask) {
        final MokoCharacteristic mokoCharacteristic = mCharacteristicMap.get(orderTask.orderType);
        if (mokoCharacteristic == null) {
            LogModule.i("executeTask : mokoCharacteristic is null");
            return;
        }
        LogModule.i("app to device write no response : " + orderTask.orderType.getName());
        LogModule.i(MokoUtils.bytesToHexString(orderTask.assemble()));
        mokoCharacteristic.characteristic.setValue(orderTask.assemble());
        mokoCharacteristic.characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mBluetoothGatt.writeCharacteristic(mokoCharacteristic.characteristic);
            }
        });
    }

    /**
     * @Date 2017/12/13 0013
     * @Author wenzheng.liu
     * @Description Clears the internal cache and forces a refresh of the services from the
     * remote device.
     */
    private boolean refreshDeviceCache() {
        if (mBluetoothGatt != null) {
            try {
                BluetoothGatt localBluetoothGatt = mBluetoothGatt;
                Method localMethod = localBluetoothGatt.getClass().getMethod("refresh", new Class[0]);
                if (localMethod != null) {
                    boolean bool = ((Boolean) localMethod.invoke(localBluetoothGatt, new Object[0])).booleanValue();
                    return bool;
                }
            } catch (Exception localException) {
                LogModule.i("An exception occured while refreshing device");
            }
        }
        return false;
    }

    ///////////////////////////////////////////////////////////////////////////
    // connect status
    ///////////////////////////////////////////////////////////////////////////
    private int connectStatus;

    public void setConnectStatus(int connectStatus) {
        this.connectStatus = connectStatus;
    }

    public int getConnectStatus() {
        return connectStatus;
    }

    ///////////////////////////////////////////////////////////////////////////
    // region
    ///////////////////////////////////////////////////////////////////////////
    private int region;

    public void setRegion(int region) {
        this.region = region;
    }

    public int getRegion() {
        return region;
    }

    ///////////////////////////////////////////////////////////////////////////
    // class type
    ///////////////////////////////////////////////////////////////////////////
    private int classType;

    public void setClassType(int classType) {
        this.classType = classType;
    }

    public int getClassType() {
        return classType;
    }

    ///////////////////////////////////////////////////////////////////////////
    // upload mode
    ///////////////////////////////////////////////////////////////////////////

    private int uploadMode;

    public void setUploadMode(int uploadMode) {
        this.uploadMode = uploadMode;
    }

    public int getUploadMode() {
        return uploadMode;
    }

    ///////////////////////////////////////////////////////////////////////////
    // company name
    ///////////////////////////////////////////////////////////////////////////

    private String companyName;

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCompanyName() {
        return companyName;
    }

    ///////////////////////////////////////////////////////////////////////////
    // manufacture date
    ///////////////////////////////////////////////////////////////////////////

    private String manufacureDate;

    public void setManufacureDate(String manufacureDate) {
        this.manufacureDate = manufacureDate;
    }

    public String getManufacureDate() {
        return manufacureDate;
    }

    ///////////////////////////////////////////////////////////////////////////
    // model name
    ///////////////////////////////////////////////////////////////////////////

    private String modelName;

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getModelName() {
        return modelName;
    }

    ///////////////////////////////////////////////////////////////////////////
    // ble firmware
    ///////////////////////////////////////////////////////////////////////////

    private String bleFirmware;

    public void setBleFirmware(String bleFirmware) {
        this.bleFirmware = bleFirmware;
    }

    public String getBleFirmware() {
        return bleFirmware;
    }

    ///////////////////////////////////////////////////////////////////////////
    // lora firmware
    ///////////////////////////////////////////////////////////////////////////

    private String loraFirmware;

    public void setLoraFirmware(String loraFirmware) {
        this.loraFirmware = loraFirmware;
    }

    public String getLoraFirmware() {
        return loraFirmware;
    }

    ///////////////////////////////////////////////////////////////////////////
    // gps
    ///////////////////////////////////////////////////////////////////////////

    private String latitude;
    private String longitude;
    private String altitude;
    private String speed;

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setAltitude(String altitude) {
        this.altitude = altitude;
    }

    public String getAltitude() {
        return altitude;
    }

    public void setSpeed(String speed) {
        this.speed = speed;
    }

    public String getSpeed() {
        return speed;
    }

    ///////////////////////////////////////////////////////////////////////////
    // sensor data
    ///////////////////////////////////////////////////////////////////////////
    public String ax;
    public String ay;
    public String az;

    public String gx;
    public String gy;
    public String gz;

    public String mx;
    public String my;
    public String mz;

    public String x_angle;
    public String y_angle;

    ///////////////////////////////////////////////////////////////////////////
    // setting
    ///////////////////////////////////////////////////////////////////////////

    public String devAddr;
    public String nwkSKey;
    public String appSKey;
    public String devEUI;
    public String appEUI;
    public String appKey;
    public long uploadInterval;
    public int ch_1;
    public int ch_2;
    public int dr_1;
    public int dr_2;
    public int power;
    public int adr;
}

