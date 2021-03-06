package com.moko.support;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.text.TextUtils;

import com.moko.support.callback.MokoResponseCallback;
import com.moko.support.callback.MokoScanDeviceCallback;
import com.moko.support.entity.DeviceTypeEnum;
import com.moko.support.entity.MokoCharacteristic;
import com.moko.support.entity.OrderEnum;
import com.moko.support.entity.OrderType;
import com.moko.support.event.ConnectStatusEvent;
import com.moko.support.event.OrderTaskResponseEvent;
import com.moko.support.handler.MokoCharacteristicHandler;
import com.moko.support.handler.MokoLeScanHandler;
import com.moko.support.log.LogModule;
import com.moko.support.task.OrderTask;
import com.moko.support.task.OrderTaskResponse;
import com.moko.support.utils.MokoUtils;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import androidx.annotation.NonNull;
import no.nordicsemi.android.ble.BleManagerCallbacks;
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
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private BlockingQueue<OrderTask> mQueue;

    private Context mContext;

    private MokoBleManager mokoBleManager;

    private MokoLeScanHandler mMokoLeScanHandler;
    private MokoScanDeviceCallback mMokoScanDeviceCallback;

    private HashMap<OrderType, MokoCharacteristic> mCharacteristicMap;
    private static final UUID DESCRIPTOR_UUID_NOTIFY = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final UUID SERVICE_UUID = UUID.fromString("0000ffc3-0000-1000-8000-00805f9b34fb");

    private static volatile MokoSupport INSTANCE;

    public static DeviceTypeEnum deviceTypeEnum;

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
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mHandler = new Handler(Looper.getMainLooper());
        mokoBleManager = MokoBleManager.getMokoBleManager(context);
        mokoBleManager.setBeaconResponseCallback(this);
        mokoBleManager.setGattCallbacks(new BleManagerCallbacks() {
            @Override
            public void onDeviceConnecting(@NonNull BluetoothDevice device) {

            }

            @Override
            public void onDeviceConnected(@NonNull BluetoothDevice device) {
            }

            @Override
            public void onDeviceDisconnecting(@NonNull BluetoothDevice device) {

            }

            @Override
            public void onDeviceDisconnected(@NonNull BluetoothDevice device) {
                if (isSyncData()) {
                    mQueue.clear();
                }
                ConnectStatusEvent connectStatusEvent = new ConnectStatusEvent();
                connectStatusEvent.setAction(MokoConstants.ACTION_CONN_STATUS_DISCONNECTED);
                EventBus.getDefault().post(connectStatusEvent);
            }

            @Override
            public void onLinkLossOccurred(@NonNull BluetoothDevice device) {

            }

            @Override
            public void onServicesDiscovered(@NonNull BluetoothDevice device, boolean optionalServicesFound) {

            }

            @Override
            public void onDeviceReady(@NonNull BluetoothDevice device) {

            }

            @Override
            public void onBondingRequired(@NonNull BluetoothDevice device) {

            }

            @Override
            public void onBonded(@NonNull BluetoothDevice device) {

            }

            @Override
            public void onBondingFailed(@NonNull BluetoothDevice device) {

            }

            @Override
            public void onError(@NonNull BluetoothDevice device, @NonNull String message, int errorCode) {

            }

            @Override
            public void onDeviceNotSupported(@NonNull BluetoothDevice device) {

            }
        });
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

    public void connDevice(final Context context, final String address) {
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
            disConnectBle();
            return;
        }
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    LogModule.i("start connect");
                    mokoBleManager.connect(device)
                            .retry(5, 200)
                            .timeout(50000)
                            .enqueue();
                }
            });
        } else {
            LogModule.i("the device is null");
        }
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
//                LogModule.w("添加" + ordertask.order.getOrderName());
            }
            executeTask();
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
     * @Date 2020/8/6
     * @Author wenzheng.liu
     * @Description
     * @ClassPath com.moko.support.MokoSupport
     */
    public synchronized void executeTask() {
        if (!isSyncData()) {
            OrderTaskResponseEvent event = new OrderTaskResponseEvent();
            event.setAction(MokoConstants.ACTION_ORDER_FINISH);
            EventBus.getDefault().post(event);
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
            disConnectBle();
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
        mokoBleManager.disconnect().enqueue();
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
        if (characteristic.getUuid().toString().equals(OrderType.CHARACTERISTIC.getUuid())
                || characteristic.getUuid().toString().equals(OrderType.CHARACTERISTIC_MCU.getUuid())
                || characteristic.getUuid().toString().equals(OrderType.CHARACTERISTIC_NOTIFY.getUuid())) {
            if (isSyncData()) {
                // 非延时应答
                OrderTask orderTask = mQueue.peek();
                if (value != null && value.length > 0 && orderTask != null) {
                    orderTask.parseValue(value);
                }
            } else {
                OrderEnum order = null;
                if (characteristic.getUuid().toString().equals(OrderType.CHARACTERISTIC_NOTIFY.getUuid())) {
                    order = OrderEnum.DISCONNECT_TYPE;
                }
                if (order != null) {
                    LogModule.i(order.getOrderName());
                    OrderTaskResponse response = new OrderTaskResponse();
                    response.order = order;
                    response.responseValue = value;
                    OrderTaskResponseEvent event = new OrderTaskResponseEvent();
                    event.setAction(MokoConstants.ACTION_CURRENT_DATA);
                    event.setResponse(response);
                    EventBus.getDefault().post(event);
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
        executeTask();
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt) {
        mBluetoothGatt = gatt;
        mCharacteristicMap = MokoCharacteristicHandler.getInstance().getCharacteristics(gatt);
        ConnectStatusEvent connectStatusEvent = new ConnectStatusEvent();
        connectStatusEvent.setAction(MokoConstants.ACTION_DISCOVER_SUCCESS);
        EventBus.getDefault().post(connectStatusEvent);
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

    private Handler mHandler;

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
    // device type
    ///////////////////////////////////////////////////////////////////////////

//    private int  deviceType;
//
//    public void setDeviceType(int deviceType) {
//        this.deviceType = deviceType;
//    }
//
//    public int getDeviceType() {
//        return deviceType;
//    }
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
    // MCU firmware
    ///////////////////////////////////////////////////////////////////////////

    private String MCUFirmware;

    public void setMCUFirmware(String MCUFirmware) {
        this.MCUFirmware = MCUFirmware;
    }

    public String getMCUFirmware() {
        return MCUFirmware;
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

    public int lowPowerPrompt;
    public int networkCheck;
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
    public int msgType;
    public long bleOpeningTime;
    ///////////////////////////////////////////////////////////////////////////
    // TH
    ///////////////////////////////////////////////////////////////////////////
    public long i2cInterval;
    public int tempEnable;
    public String tempLow;
    public String tempHigh;
    public String tempCurrent;

    public int humiEnable;
    public String humiLow;
    public String humiHigh;
    public String humiCurrent;
    ///////////////////////////////////////////////////////////////////////////
    // B
    ///////////////////////////////////////////////////////////////////////////
    public int scanSwitch;
    public String filterName;
    public int scanUploadInterval;
    public int filterRssi;
    public String filterMac;
    public String filterMajorMin;
    public String filterMajorMax;
    public String filterMinorMin;
    public String filterMinorMax;
    public String filterUUID;
    public String filterRawData;
    ///////////////////////////////////////////////////////////////////////////
    // Multicast
    ///////////////////////////////////////////////////////////////////////////
    public int multicastSwitch;
    public String multicastAddr;
    public String multicastNwkSKey;
    public String multicastAppSKey;
    ///////////////////////////////////////////////////////////////////////////
    // Alarm
    ///////////////////////////////////////////////////////////////////////////
    public int alarmStatus;
    public int alarmUploadInterval;
    public int alarmScanTime;
    public int alarmReportNumer;
    public int alarmTriggerMode;
    public int alamrVibrationSwitch;
    public int alarmGpsSwitch;
    public int alarmSatelliteSearchTime;
}

