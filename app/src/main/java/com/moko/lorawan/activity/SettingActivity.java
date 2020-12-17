package com.moko.lorawan.activity;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.moko.lorawan.AppConstants;
import com.moko.lorawan.R;
import com.moko.lorawan.dialog.LoadingDialog;
import com.moko.lorawan.utils.ToastUtils;
import com.moko.support.MokoConstants;
import com.moko.support.MokoSupport;
import com.moko.support.entity.DeviceTypeEnum;
import com.moko.support.entity.OrderEnum;
import com.moko.support.event.ConnectStatusEvent;
import com.moko.support.event.OrderTaskResponseEvent;
import com.moko.support.task.OrderTask;
import com.moko.support.task.OrderTaskResponse;
import com.moko.support.task.ReadADRTask;
import com.moko.support.task.ReadAlarmGPSSwitchModeTask;
import com.moko.support.task.ReadAlarmReportNumberTask;
import com.moko.support.task.ReadAlarmSatelliteSearchTimeTask;
import com.moko.support.task.ReadAlarmTriggerModeTask;
import com.moko.support.task.ReadAlarmUploadIntervalTask;
import com.moko.support.task.ReadAlarmVibrationSwitchModeTask;
import com.moko.support.task.ReadAppEUITask;
import com.moko.support.task.ReadAppKeyTask;
import com.moko.support.task.ReadAppSKeyTask;
import com.moko.support.task.ReadBleTask;
import com.moko.support.task.ReadCHTask;
import com.moko.support.task.ReadClassTypeTask;
import com.moko.support.task.ReadDRTask;
import com.moko.support.task.ReadDevAddrTask;
import com.moko.support.task.ReadDevEUITask;
import com.moko.support.task.ReadFilterAdvRawData;
import com.moko.support.task.ReadFilterMacTask;
import com.moko.support.task.ReadFilterMajorTask;
import com.moko.support.task.ReadFilterMinorTask;
import com.moko.support.task.ReadFilterNameTask;
import com.moko.support.task.ReadFilterRSSITask;
import com.moko.support.task.ReadFilterUUIDTask;
import com.moko.support.task.ReadLowPowerPromptTask;
import com.moko.support.task.ReadMsgTypeTask;
import com.moko.support.task.ReadMulticastAddrTask;
import com.moko.support.task.ReadMulticastAppSKeyTask;
import com.moko.support.task.ReadMulticastNwkSKeyTask;
import com.moko.support.task.ReadMulticastSwitchTask;
import com.moko.support.task.ReadNetworkCheckTask;
import com.moko.support.task.ReadNwkSKeyTask;
import com.moko.support.task.ReadRegionTask;
import com.moko.support.task.ReadScanSwitchTask;
import com.moko.support.task.ReadScanUploadIntervalTask;
import com.moko.support.task.ReadUploadIntervalTask;
import com.moko.support.task.ReadUploadModeTask;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;

public class SettingActivity extends BaseActivity {


    @Bind(R.id.rl_device_setting)
    RelativeLayout rlDeviceSetting;
    @Bind(R.id.tv_lora_setting)
    TextView tvLoraSetting;
    @Bind(R.id.rl_multicast_setting)
    RelativeLayout rlMulticastSetting;
    @Bind(R.id.rl_alarm_setting)
    RelativeLayout rlAlarmSetting;
    @Bind(R.id.rl_ble_setting)
    RelativeLayout rlBleSetting;
    @Bind(R.id.rl_scan_setting)
    RelativeLayout rlScanSetting;
    @Bind(R.id.rl_gps_setting)
    RelativeLayout rlGpsSetting;

    private String[] regions;
    private String[] classTypes;
    private String[] uploadModes;
    private String mDeviceMac;
    private boolean mReceiverTag = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        ButterKnife.bind(this);
        int region = MokoSupport.getInstance().getRegion();
        int classType = MokoSupport.getInstance().getClassType();
        int uploadMode = MokoSupport.getInstance().getUploadMode();
        int deviceType = MokoSupport.deviceTypeEnum.getDeviceType();
        rlDeviceSetting.setVisibility(deviceType == 3 ? View.VISIBLE : View.GONE);
        rlBleSetting.setVisibility(deviceType == 1 ? View.VISIBLE : View.GONE);
        rlScanSetting.setVisibility(deviceType == 2 || deviceType == 3 ? View.VISIBLE : View.GONE);
        rlMulticastSetting.setVisibility(deviceType != 0 && deviceType != 3 ? View.VISIBLE : View.GONE);
        rlAlarmSetting.setVisibility(deviceType == 3 ? View.VISIBLE : View.GONE);
        rlGpsSetting.setVisibility(deviceType == 3 ? View.VISIBLE : View.GONE);
        regions = getResources().getStringArray(R.array.region);
        classTypes = getResources().getStringArray(R.array.class_type);
        uploadModes = getResources().getStringArray(R.array.upload_mode);
        tvLoraSetting.setText(String.format("%s/%s/%s", uploadMode > 2 ? "" : uploadModes[uploadMode - 1], regions[region], classTypes[classType - 1]));
        mDeviceMac = getIntent().getStringExtra(AppConstants.EXTRA_KEY_DEVICE_MAC);
        EventBus.getDefault().register(this);
        // 注册广播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);
        mReceiverTag = true;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConnectStatusEvent(ConnectStatusEvent event) {
        String action = event.getAction();
        if (MokoConstants.ACTION_CONN_STATUS_DISCONNECTED.equals(action)) {
            // 设备断开
            dismissLoadingProgressDialog();
            finish();
        }
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 200)
    public void onOrderTaskResponseEvent(OrderTaskResponseEvent event) {
        final String action = event.getAction();
        if (!MokoConstants.ACTION_CURRENT_DATA.equals(action)) {
            EventBus.getDefault().cancelEventDelivery(event);
        }
        runOnUiThread(() -> {
            if (MokoConstants.ACTION_ORDER_TIMEOUT.equals(action)) {
                OrderTaskResponse response = event.getResponse();
                OrderEnum orderEnum = response.order;
                switch (orderEnum) {
                    case READ_BLE:
                        ToastUtils.showToast(SettingActivity.this, "Error");
                        break;
                }
            }
            if (MokoConstants.ACTION_ORDER_FINISH.equals(action)) {
                dismissLoadingProgressDialog();

            }
            if (MokoConstants.ACTION_ORDER_RESULT.equals(action)) {
                OrderTaskResponse response = event.getResponse();
                OrderEnum orderEnum = response.order;
                switch (orderEnum) {
                    case READ_LOW_POWER_PROMPT:
                        // 跳转组播设置页面
                        startActivity(new Intent(SettingActivity.this, DeviceSettingActivity.class));
                        break;
                    case READ_ADR:
                        // 跳转设置页面
                        startActivityForResult(new Intent(SettingActivity.this, LoRaSettingActivity.class), AppConstants.REQUEST_CODE_LORA_SETTING);
                        break;
                    case READ_UPLOAD_MODE:
                        int region = MokoSupport.getInstance().getRegion();
                        int classType = MokoSupport.getInstance().getClassType();
                        int uploadMode = MokoSupport.getInstance().getUploadMode();
                        tvLoraSetting.setText(String.format("%s/%s/%s", uploadMode > 2 ? "" : uploadModes[uploadMode - 1], regions[region], classTypes[classType - 1]));
                        break;
                    case READ_BLE:
                        // 跳转蓝牙设置页面
                        startActivity(new Intent(SettingActivity.this, BleSettingActivity.class));
                        break;
                    case READ_FILTER_RSSI:
                        if (MokoSupport.deviceTypeEnum == DeviceTypeEnum.LW004_BP) {
                            // 跳转扫描设置页面
                            startActivity(new Intent(SettingActivity.this, ScanSettingFilterActivity.class));
                        } else {
                            // 跳转扫描设置页面
                            startActivity(new Intent(SettingActivity.this, ScanSettingActivity.class));
                        }
                        break;
                    case READ_MULTICAST_APPSKEY:
                        // 跳转组播设置页面
                        startActivity(new Intent(SettingActivity.this, MulticastSettingActivity.class));
                        break;
                    case READ_ALARM_TRIGGER_MODE:
                        // 跳转报警设置页面
                        startActivity(new Intent(SettingActivity.this, AlarmSettingActivity.class));
                        break;
                    case READ_ALARM_GPS_SWITCH:
                        // 跳转GPS设置页面
                        startActivity(new Intent(SettingActivity.this, GPSSettingActivity.class));
                        break;
                }
            }
        });
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
                            finish();
                            break;
                    }
                }
            }
        }
    };

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

    private LoadingDialog mLoadingDialog;

    private void showLoadingProgressDialog() {
        mLoadingDialog = new LoadingDialog();
        mLoadingDialog.show(getSupportFragmentManager());

    }

    private void dismissLoadingProgressDialog() {
        if (mLoadingDialog != null)
            mLoadingDialog.dismissAllowingStateLoss();
    }


    public void back(View view) {
        finish();
    }

    public void deviceSetting(View view) {
        showLoadingProgressDialog();
        ArrayList<OrderTask> orderTasks = new ArrayList<>();
        orderTasks.add(new ReadNetworkCheckTask());
        orderTasks.add(new ReadLowPowerPromptTask());
        MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }

    public void loraSetting(View view) {
        showLoadingProgressDialog();
        ArrayList<OrderTask> orderTasks = new ArrayList<>();
        orderTasks.add(new ReadDevEUITask());
        orderTasks.add(new ReadAppEUITask());
        orderTasks.add(new ReadAppKeyTask());
        orderTasks.add(new ReadDevAddrTask());
        orderTasks.add(new ReadNwkSKeyTask());
        orderTasks.add(new ReadAppSKeyTask());
        orderTasks.add(new ReadCHTask());
        orderTasks.add(new ReadDRTask());
        if (MokoSupport.deviceTypeEnum != DeviceTypeEnum.LW002_TH) {
            orderTasks.add(new ReadUploadIntervalTask());
        }
        orderTasks.add(new ReadMsgTypeTask());
        if (MokoSupport.deviceTypeEnum == DeviceTypeEnum.LW004_BP) {
            orderTasks.add(new ReadAlarmSatelliteSearchTimeTask());
        }
        orderTasks.add(new ReadADRTask());
        MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }


    public void bleSetting(View view) {
        showLoadingProgressDialog();
        ArrayList<OrderTask> orderTasks = new ArrayList<>();
        orderTasks.add(new ReadBleTask());
        MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }

    public void scanSetting(View view) {
        showLoadingProgressDialog();
        ArrayList<OrderTask> orderTasks = new ArrayList<>();
        if (MokoSupport.deviceTypeEnum == DeviceTypeEnum.LW003_B) {
            orderTasks.add(new ReadScanUploadIntervalTask());
            orderTasks.add(new ReadScanSwitchTask());
        }
        orderTasks.add(new ReadFilterNameTask());
        if (MokoSupport.deviceTypeEnum == DeviceTypeEnum.LW004_BP) {
            orderTasks.add(new ReadFilterMacTask());
            orderTasks.add(new ReadFilterUUIDTask());
            orderTasks.add(new ReadFilterMajorTask());
            orderTasks.add(new ReadFilterMinorTask());
            orderTasks.add(new ReadFilterAdvRawData());
        }
        orderTasks.add(new ReadFilterRSSITask());
        MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }

    public void multicastSetting(View view) {
        showLoadingProgressDialog();
        ArrayList<OrderTask> orderTasks = new ArrayList<>();
        orderTasks.add(new ReadMulticastSwitchTask());
        orderTasks.add(new ReadMulticastAddrTask());
        orderTasks.add(new ReadMulticastNwkSKeyTask());
        orderTasks.add(new ReadMulticastAppSKeyTask());
        MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }

    public void alarmSetting(View view) {
        showLoadingProgressDialog();
        ArrayList<OrderTask> orderTasks = new ArrayList<>();
        orderTasks.add(new ReadAlarmUploadIntervalTask());
        orderTasks.add(new ReadAlarmVibrationSwitchModeTask());
        orderTasks.add(new ReadAlarmReportNumberTask());
        orderTasks.add(new ReadAlarmTriggerModeTask());
        MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }

    public void gpsSetting(View view) {
        showLoadingProgressDialog();
        ArrayList<OrderTask> orderTasks = new ArrayList<>();
        orderTasks.add(new ReadAlarmSatelliteSearchTimeTask());
        orderTasks.add(new ReadAlarmGPSSwitchModeTask());
        MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AppConstants.REQUEST_CODE_LORA_SETTING) {
            if (resultCode == RESULT_OK) {
                setResult(RESULT_OK);
                finish();
            } else {
                tvLoraSetting.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!MokoSupport.getInstance().isConnDevice(SettingActivity.this, mDeviceMac))
                            return;
                        showLoadingProgressDialog();
                        ArrayList<OrderTask> orderTasks = new ArrayList<>();
                        orderTasks.add(new ReadRegionTask());
                        orderTasks.add(new ReadClassTypeTask());
                        orderTasks.add(new ReadUploadModeTask());
                        MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
                    }
                }, 500);
            }
        }
    }
}
