package com.moko.lorawan.activity;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.moko.lorawan.AppConstants;
import com.moko.lorawan.R;
import com.moko.lorawan.dialog.LoadingDialog;
import com.moko.lorawan.service.MokoService;
import com.moko.lorawan.utils.ToastUtils;
import com.moko.support.MokoConstants;
import com.moko.support.MokoSupport;
import com.moko.support.entity.DeviceTypeEnum;
import com.moko.support.entity.OrderEnum;
import com.moko.support.event.ConnectStatusEvent;
import com.moko.support.event.OrderTaskResponseEvent;
import com.moko.support.task.OrderTaskResponse;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.Bind;
import butterknife.ButterKnife;

public class SettingActivity extends BaseActivity {


    @Bind(R.id.tv_device_setting)
    TextView tvDeviceSetting;
    @Bind(R.id.tv_device_name)
    TextView tvDeviceName;
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
    private MokoService mMokoService;
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
        rlBleSetting.setVisibility(deviceType == 1 ? View.VISIBLE : View.GONE);
        rlScanSetting.setVisibility(deviceType == 2 || deviceType == 3 ? View.VISIBLE : View.GONE);
        rlMulticastSetting.setVisibility(deviceType != 0 && deviceType != 3 ? View.VISIBLE : View.GONE);
        rlAlarmSetting.setVisibility(deviceType == 3 ? View.VISIBLE : View.GONE);
        rlGpsSetting.setVisibility(deviceType == 3 ? View.VISIBLE : View.GONE);
        String modelName = MokoSupport.getInstance().getModelName();
        regions = getResources().getStringArray(R.array.region);
        classTypes = getResources().getStringArray(R.array.class_type);
        uploadModes = getResources().getStringArray(R.array.upload_mode);
        tvDeviceSetting.setText(String.format("%s/%s/%s", uploadMode > 2 ? "" : uploadModes[uploadMode - 1], regions[region], classTypes[classType - 1]));
        tvDeviceName.setText(modelName);
        bindService(new Intent(this, MokoService.class), mServiceConnection, BIND_AUTO_CREATE);
        EventBus.getDefault().register(this);
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mMokoService = ((MokoService.LocalBinder) service).getService();
            // 注册广播接收器
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mReceiver, filter);
            mReceiverTag = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

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
        EventBus.getDefault().cancelEventDelivery(event);
        final String action = event.getAction();
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
                    case READ_ADR:
                        // 跳转设置页面
                        startActivityForResult(new Intent(SettingActivity.this, DeviceSettingActivity.class), AppConstants.REQUEST_CODE_DEVICE_SETTING);
                        break;
                    case READ_UPLOAD_MODE:
                        int region = MokoSupport.getInstance().getRegion();
                        int classType = MokoSupport.getInstance().getClassType();
                        int uploadMode = MokoSupport.getInstance().getUploadMode();
                        tvDeviceSetting.setText(String.format("%s/%s/%s", uploadMode > 2 ? "" : uploadModes[uploadMode - 1], regions[region], classTypes[classType - 1]));
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
        unbindService(mServiceConnection);
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
        mMokoService.getDeviceSetting();
    }


    public void bleSetting(View view) {
        showLoadingProgressDialog();
        mMokoService.getBleInfo();
    }

    public void scanSetting(View view) {
        showLoadingProgressDialog();
        mMokoService.getScanSetting();
    }

    public void multicastSetting(View view) {
        showLoadingProgressDialog();
        mMokoService.getMulticastSetting();
    }

    public void alarmSetting(View view) {
        showLoadingProgressDialog();
        mMokoService.getAlarmSetting();
    }

    public void gpsSetting(View view) {
        showLoadingProgressDialog();
        mMokoService.getGPSSetting();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AppConstants.REQUEST_CODE_DEVICE_SETTING) {
            if (resultCode == RESULT_OK) {
                setResult(RESULT_OK);
                finish();
            } else {
                showLoadingProgressDialog();
                tvDeviceName.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mMokoService.getDeviceSettingType();
                    }
                }, 500);
            }
        }
    }


}
