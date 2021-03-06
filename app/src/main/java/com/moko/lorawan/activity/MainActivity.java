package com.moko.lorawan.activity;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Message;
import android.util.SparseArray;
import android.view.View;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.moko.lorawan.AppConstants;
import com.moko.lorawan.R;
import com.moko.lorawan.adapter.LoRaDeviceAdapter;
import com.moko.lorawan.dialog.AlertMessageDialog;
import com.moko.lorawan.dialog.LoadingDialog;
import com.moko.lorawan.dialog.PasswordDialog;
import com.moko.lorawan.utils.OrderTaskAssembler;
import com.moko.lorawan.utils.SPUtiles;
import com.moko.lorawan.utils.ToastUtils;
import com.moko.support.MokoConstants;
import com.moko.support.MokoSupport;
import com.moko.support.callback.MokoScanDeviceCallback;
import com.moko.support.entity.DeviceInfo;
import com.moko.support.entity.DeviceTypeEnum;
import com.moko.support.entity.OrderEnum;
import com.moko.support.entity.OrderType;
import com.moko.support.event.ConnectStatusEvent;
import com.moko.support.event.OrderTaskResponseEvent;
import com.moko.support.handler.BaseMessageHandler;
import com.moko.support.log.LogModule;
import com.moko.support.task.OpenNotifyTask;
import com.moko.support.task.OrderTask;
import com.moko.support.task.OrderTaskResponse;
import com.moko.support.task.ReadAlarmStatusTask;
import com.moko.support.task.ReadConnectStatusTask;
import com.moko.support.task.ReadModelNameTask;
import com.moko.support.task.WriteRTCTimeTask;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.header.ClassicsHeader;
import com.scwang.smartrefresh.layout.listener.OnRefreshListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import no.nordicsemi.android.support.v18.scanner.ScanRecord;
import no.nordicsemi.android.support.v18.scanner.ScanResult;


public class MainActivity extends BaseActivity implements MokoScanDeviceCallback, BaseQuickAdapter.OnItemClickListener, OnRefreshListener {


    @BindView(R.id.rv_main)
    RecyclerView rvMain;
    @BindView(R.id.srl_main)
    SmartRefreshLayout srlMain;
    private boolean mReceiverTag = false;
    private LoRaDeviceAdapter mAdapter;
    private List<DeviceInfo> mDeviceInfos;
    private HashMap<String, DeviceInfo> mDeviceInfoHashMap;
    private String mSelectedDeviceName;
    private String mSelectedDeviceMac;
    private boolean isPasswordError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        mSavedPassword = SPUtiles.getStringValue(this, AppConstants.SP_KEY_SAVED_PASSWORD, "");
        mHandler = new CustomMessageHandler(this);
        mAdapter = new LoRaDeviceAdapter();
        mDeviceInfos = new ArrayList<>();
        mDeviceInfoHashMap = new HashMap<>();
        mAdapter.replaceData(mDeviceInfos);
        rvMain.setLayoutManager(new LinearLayoutManager(this));
        DividerItemDecoration itemDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        itemDecoration.setDrawable(ContextCompat.getDrawable(this, R.drawable.shape_recycleview_divider));
        rvMain.addItemDecoration(itemDecoration);
        rvMain.setAdapter(mAdapter);
        mAdapter.openLoadAnimation();
        mAdapter.setOnItemClickListener(this);
        srlMain.setOnRefreshListener(this);

        srlMain.setRefreshHeader(new ClassicsHeader(this));
        EventBus.getDefault().register(this);
        // 注册广播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);
        mReceiverTag = true;
        if (!MokoSupport.getInstance().isBluetoothOpen()) {
            // 蓝牙未打开，开启蓝牙
            MokoSupport.getInstance().enableBluetooth();
        } else {
            srlMain.postDelayed(new Runnable() {
                @Override
                public void run() {
                    srlMain.autoRefresh();
                }
            }, 500);
        }
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                    switch (blueState) {
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            srlMain.finishRefresh();
                            srlMain.setEnableRefresh(false);
                            MokoSupport.getInstance().stopScanDevice();
                            break;
                        case BluetoothAdapter.STATE_ON:
                            srlMain.setEnableRefresh(true);
                            srlMain.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    srlMain.autoRefresh();
                                }
                            }, 500);
                            break;
                    }
                }
            }
        }
    };

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConnectStatusEvent(ConnectStatusEvent event) {
        String action = event.getAction();
        if (MokoConstants.ACTION_CONN_STATUS_DISCONNECTED.equals(action)) {
            // 设备断开
            dismissLoadingProgressDialog();
            if (isPasswordError) {
                isPasswordError = false;
            } else {
                ToastUtils.showToast(MainActivity.this, "Disconnected");
            }
        }
        if (MokoConstants.ACTION_DISCOVER_SUCCESS.equals(action)) {
            // 设备连接成功
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    ArrayList<OrderTask> orderTasks = new ArrayList<>();
                    switch (MokoSupport.deviceTypeEnum) {
                        case LW001_BG:
                            orderTasks.add(new OpenNotifyTask(OrderType.CHARACTERISTIC));
                            orderTasks.add(new OpenNotifyTask(OrderType.CHARACTERISTIC_LOG));
                            orderTasks.add(new ReadModelNameTask());
                            orderTasks.add(new ReadConnectStatusTask());
                            break;
                        case LW003_B:
                            orderTasks.add(new OpenNotifyTask(OrderType.CHARACTERISTIC));
                            orderTasks.add(new OpenNotifyTask(OrderType.CHARACTERISTIC_LOG));
                            orderTasks.add(new ReadModelNameTask());
                            orderTasks.add(new WriteRTCTimeTask());
                            orderTasks.add(new ReadConnectStatusTask());
                            break;
                        case LW002_TH:
                            orderTasks.add(new OpenNotifyTask(OrderType.CHARACTERISTIC));
                            orderTasks.add(new OpenNotifyTask(OrderType.CHARACTERISTIC_LOG));
                            orderTasks.add(new OpenNotifyTask(OrderType.CHARACTERISTIC_MCU));
                            orderTasks.add(new OpenNotifyTask(OrderType.CHARACTERISTIC_PERIPHERAL));
                            orderTasks.add(new ReadModelNameTask());
                            orderTasks.add(new WriteRTCTimeTask());
                            orderTasks.add(new ReadConnectStatusTask());
                            break;
                        case LW004_BP:
                            orderTasks.add(new OpenNotifyTask(OrderType.CHARACTERISTIC_NOTIFY));
                            orderTasks.add(OrderTaskAssembler.verifyPassword(mPassword));
                            break;
                    }
                    MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
                }
            }, 500);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onOrderTaskResponseEvent(OrderTaskResponseEvent event) {
        final String action = event.getAction();
        if (MokoConstants.ACTION_ORDER_TIMEOUT.equals(action)) {

        }
        if (MokoConstants.ACTION_ORDER_FINISH.equals(action)) {
            dismissLoadingProgressDialog();
        }
        if (MokoConstants.ACTION_ORDER_RESULT.equals(action)) {
            OrderTaskResponse response = event.getResponse();
            OrderEnum orderEnum = response.order;
            byte[] value = response.responseValue;
            switch (orderEnum) {
                case READ_CONNECT_STATUS:
                    LogModule.clearInfoForFile();
                    // 跳转基础信息页面
                    Intent i = new Intent(MainActivity.this, BasicInfoActivity.class);
                    i.putExtra(AppConstants.EXTRA_KEY_DEVICE_NAME, mSelectedDeviceName);
                    i.putExtra(AppConstants.EXTRA_KEY_DEVICE_MAC, mSelectedDeviceMac);
                    startActivityForResult(i, AppConstants.REQUEST_CODE_BASIC);
                    break;
                case PASSWORD:
                    if ((value[3] & 0xFF) == 0xAA) {
                        showLoadingProgressDialog();
                        mSavedPassword = mPassword;
                        SPUtiles.setStringValue(MainActivity.this, AppConstants.SP_KEY_SAVED_PASSWORD, mSavedPassword);
                        LogModule.i("Success");
                        ArrayList<OrderTask> orderTasks = new ArrayList<>();
                        orderTasks.add(new ReadAlarmStatusTask());
                        orderTasks.add(new ReadModelNameTask());
                        orderTasks.add(new WriteRTCTimeTask());
                        orderTasks.add(new ReadConnectStatusTask());
                        MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
                    } else {
                        isPasswordError = true;
                        ToastUtils.showToast(MainActivity.this, "Password Error");
                        MokoSupport.getInstance().disConnectBle();
                    }
                    break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mReceiverTag) {
            mReceiverTag = false;
            // 注销广播
            unregisterReceiver(mReceiver);
        }
        EventBus.getDefault().unregister(this);
    }


    @Override
    public void onStartScan() {
        mDeviceInfoHashMap.clear();
    }

    @Override
    public void onScanDevice(DeviceInfo deviceInfo) {
        ScanResult result = deviceInfo.scanResult;
        ScanRecord record = result.getScanRecord();
        SparseArray<byte[]> manufacturer = result.getScanRecord().getManufacturerSpecificData();
        if (manufacturer != null && manufacturer.size() != 0) {
            byte[] manufacturerSpecificDataByte = record.getManufacturerSpecificData(manufacturer.keyAt(0));
            if (manufacturerSpecificDataByte.length > 5) {
                int deviceType = manufacturerSpecificDataByte[5] & 0xff;
                if (deviceType == 1) {
                    deviceInfo.deviceTypeEnum = DeviceTypeEnum.LW002_TH;
                } else if (deviceType == 2) {
                    deviceInfo.deviceTypeEnum = DeviceTypeEnum.LW003_B;
                } else if (deviceType == 3) {
                    deviceInfo.deviceTypeEnum = DeviceTypeEnum.LW004_BP;
                } else {
                    deviceInfo.deviceTypeEnum = DeviceTypeEnum.LW001_BG;
                }
            } else {
                deviceInfo.deviceTypeEnum = DeviceTypeEnum.LW001_BG;
            }
        }
        mDeviceInfoHashMap.put(deviceInfo.mac, deviceInfo);
        updateDevices();
    }

    @Override
    public void onStopScan() {
        updateDevices();
    }

    private void updateDevices() {
        mDeviceInfos.clear();
        mDeviceInfos.addAll(mDeviceInfoHashMap.values());
        if (!mDeviceInfos.isEmpty()) {
            Collections.sort(mDeviceInfos, new Comparator<DeviceInfo>() {
                @Override
                public int compare(DeviceInfo lhs, DeviceInfo rhs) {
                    if (lhs.rssi > rhs.rssi) {
                        return -1;
                    } else if (lhs.rssi < rhs.rssi) {
                        return 1;
                    }
                    return 0;
                }
            });
        }
        mAdapter.replaceData(mDeviceInfos);
    }

    private LoadingDialog mLoadingDialog;

    private void showLoadingProgressDialog() {
        mLoadingDialog = new LoadingDialog();
        mLoadingDialog.show(getSupportFragmentManager());

    }

    private void dismissLoadingProgressDialog() {
        if (mLoadingDialog != null)
            mLoadingDialog.dismissAllowingStateLoss();
    }

    @Override
    public void onBackPressed() {
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setMessage(R.string.main_exit_tips);
        dialog.setOnAlertConfirmListener(new AlertMessageDialog.OnAlertConfirmListener() {
            @Override
            public void onClick() {
                MainActivity.this.finish();
            }
        });
        dialog.show(getSupportFragmentManager());
    }

    private String mPassword;
    private String mSavedPassword;

    @Override
    public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
        srlMain.finishRefresh();
        MokoSupport.getInstance().stopScanDevice();
        if (!MokoSupport.getInstance().isBluetoothOpen()) {
            MokoSupport.getInstance().enableBluetooth();
            return;
        }
        // 跳转
        final DeviceInfo deviceInfo = (DeviceInfo) adapter.getItem(position);
        if (deviceInfo == null && !isFinishing()) {
            return;
        }
        MokoSupport.deviceTypeEnum = deviceInfo.deviceTypeEnum;
        mSelectedDeviceName = deviceInfo.name;
        mSelectedDeviceMac = deviceInfo.mac;
        if (deviceInfo.deviceTypeEnum == DeviceTypeEnum.LW004_BP) {
            // show password
            final PasswordDialog dialog = new PasswordDialog(MainActivity.this);
            dialog.setData(mSavedPassword);
            dialog.setOnPasswordClicked(new PasswordDialog.PasswordClickListener() {
                @Override
                public void onEnsureClicked(String password) {
                    if (!MokoSupport.getInstance().isBluetoothOpen()) {
                        MokoSupport.getInstance().enableBluetooth();
                        return;
                    }
                    LogModule.i(password);
                    mPassword = password;
                    showLoadingProgressDialog();
                    mHandler.postDelayed(() -> MokoSupport.getInstance().connDevice(MainActivity.this, deviceInfo.mac), 500);
                }

                @Override
                public void onDismiss() {

                }
            });
            dialog.show();
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {

                @Override
                public void run() {
                    runOnUiThread(() -> dialog.showKeyboard());
                }
            }, 200);
        } else {
            showLoadingProgressDialog();
            mHandler.postDelayed(() -> MokoSupport.getInstance().connDevice(MainActivity.this, deviceInfo.mac), 500);
        }
    }

    @Override
    public void onRefresh(@NonNull final RefreshLayout refreshLayout) {
        MokoSupport.getInstance().startScanDevice(MainActivity.this);
        srlMain.postDelayed(new Runnable() {
            @Override
            public void run() {
                refreshLayout.finishRefresh();
                MokoSupport.getInstance().stopScanDevice();
            }
        }, 5000);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AppConstants.REQUEST_CODE_BASIC) {
            if (resultCode == RESULT_OK) {
                srlMain.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        srlMain.autoRefresh();
                    }
                }, 500);
            }
        }
    }

    public void about(View view) {
        // 关于
        startActivity(new Intent(this, AboutActivity.class));
    }

    public CustomMessageHandler mHandler;

    public class CustomMessageHandler extends BaseMessageHandler<MainActivity> {

        public CustomMessageHandler(MainActivity activity) {
            super(activity);
        }

        @Override
        protected void handleMessage(MainActivity activity, Message msg) {
        }
    }
}
