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
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.moko.lorawan.AppConstants;
import com.moko.lorawan.R;
import com.moko.lorawan.dialog.LoadingDialog;
import com.moko.lorawan.service.MokoService;
import com.moko.lorawan.utils.ToastUtils;
import com.moko.support.MokoConstants;
import com.moko.support.MokoSupport;
import com.moko.support.entity.OrderEnum;
import com.moko.support.event.ConnectStatusEvent;
import com.moko.support.task.OrderTaskResponse;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.Bind;
import butterknife.ButterKnife;

public class BasicInfoActivity extends BaseActivity {

    @Bind(R.id.tv_connect_status)
    TextView tvConnectStatus;
    @Bind(R.id.tv_device_setting)
    TextView tvDeviceSetting;
    @Bind(R.id.tv_device_name)
    TextView tvDeviceName;
    @Bind(R.id.rl_gps_axis)
    RelativeLayout rlGpsAxis;
    @Bind(R.id.rl_ble_setting)
    RelativeLayout rlBleSetting;
    @Bind(R.id.rl_sensor_data)
    RelativeLayout rlSensorData;

    private String[] connectStatusStrs;
    private String[] regions;
    private String[] classTypes;
    private String[] uploadModes;
    private MokoService mMokoService;
    private boolean mReceiverTag = false;
    private String mDeviceName;
    private String mDeviceMac;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_basic_info);
        ButterKnife.bind(this);
        mDeviceName = getIntent().getStringExtra(AppConstants.EXTRA_KEY_DEVICE_NAME);
        mDeviceMac = getIntent().getStringExtra(AppConstants.EXTRA_KEY_DEVICE_MAC);
        int connectStatus = MokoSupport.getInstance().getConnectStatus();
        int region = MokoSupport.getInstance().getRegion();
        int classType = MokoSupport.getInstance().getClassType();
        int uploadMode = MokoSupport.getInstance().getUploadMode();
        int deviceType = MokoSupport.deviceTypeEnum.getDeviceType();
        rlGpsAxis.setVisibility(deviceType == 1 ? View.GONE : View.VISIBLE);
        rlBleSetting.setVisibility(deviceType == 1 ? View.VISIBLE : View.GONE);
        rlSensorData.setVisibility(deviceType == 1 ? View.VISIBLE : View.GONE);
        String modelName = MokoSupport.getInstance().getModelName();
        connectStatusStrs = getResources().getStringArray(R.array.connect_status);
        regions = getResources().getStringArray(R.array.region);
        classTypes = getResources().getStringArray(R.array.class_type);
        uploadModes = getResources().getStringArray(R.array.upload_mode);
        tvConnectStatus.setText(connectStatusStrs[connectStatus]);
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
            filter.addAction(MokoConstants.ACTION_ORDER_RESULT);
            filter.addAction(MokoConstants.ACTION_ORDER_TIMEOUT);
            filter.addAction(MokoConstants.ACTION_ORDER_FINISH);
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            filter.setPriority(200);
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

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                    switch (blueState) {
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            backToHome();
                            break;
                    }
                }
                if (MokoConstants.ACTION_ORDER_TIMEOUT.equals(action)) {
                    OrderTaskResponse response = (OrderTaskResponse) intent.getSerializableExtra(MokoConstants.EXTRA_KEY_RESPONSE_ORDER_TASK);
                    OrderEnum orderEnum = response.order;
                    switch (orderEnum) {
                        case READ_BLE:
                            ToastUtils.showToast(BasicInfoActivity.this, "Error");
                            break;
                    }
                }
                if (MokoConstants.ACTION_ORDER_FINISH.equals(action)) {
                    dismissLoadingProgressDialog();
                }
                if (MokoConstants.ACTION_ORDER_RESULT.equals(action)) {
                    abortBroadcast();
                    OrderTaskResponse response = (OrderTaskResponse) intent.getSerializableExtra(MokoConstants.EXTRA_KEY_RESPONSE_ORDER_TASK);
                    OrderEnum orderEnum = response.order;
                    switch (orderEnum) {
                        case READ_LORA_FIRMWARE:
                            // 跳转设备信息页面
                            startActivityForResult(new Intent(BasicInfoActivity.this, DeviceInfoActivity.class), AppConstants.REQUEST_CODE_REFRESH);
                            break;
                        case READ_9_AXIS_ANGLE:
                            // 跳转9轴和传感器页面
                            startActivityForResult(new Intent(BasicInfoActivity.this, GPSAndSensorDataActivity.class), AppConstants.REQUEST_CODE_REFRESH);
                            break;
                        case READ_ADR:
                            // 跳转设置页面
                            startActivityForResult(new Intent(BasicInfoActivity.this, DeviceSettingActivity.class), AppConstants.REQUEST_CODE_DEVICE_SETTING);
                            break;
                        case READ_HUMI:
                            // 跳转温湿度传感器页面
                            startActivityForResult(new Intent(BasicInfoActivity.this, SensorDataActivity.class), AppConstants.REQUEST_CODE_REFRESH);
                            break;
                        case READ_UPLOAD_MODE:
                            int connectStatus = MokoSupport.getInstance().getConnectStatus();
                            int region = MokoSupport.getInstance().getRegion();
                            int classType = MokoSupport.getInstance().getClassType();
                            int uploadMode = MokoSupport.getInstance().getUploadMode();
                            tvConnectStatus.setText(connectStatusStrs[connectStatus]);
                            tvDeviceSetting.setText(String.format("%s/%s/%s", uploadMode > 2 ? "" : uploadModes[uploadMode - 1], regions[region], classTypes[classType - 1]));
                            break;
                        case READ_BLE:
                            // 跳转蓝牙设置页面
                            startActivityForResult(new Intent(BasicInfoActivity.this, BleSettingActivity.class), AppConstants.REQUEST_CODE_REFRESH);
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
        backToHome();
    }

    private void backToHome() {
        MokoSupport.getInstance().disConnectBle();
        finish();
    }

    @Override
    public void onBackPressed() {
        backToHome();
    }

    public void deviceSetting(View view) {
        showLoadingProgressDialog();
        mMokoService.getDeviceSetting();
    }

    public void thSensorData(View view) {
        showLoadingProgressDialog();
        mMokoService.getSensorData();
    }

    public void gpsAndSensorData(View view) {
        showLoadingProgressDialog();
        mMokoService.getGPSAndSensorData();
    }

    public void uplinkTest(View view) {
        startActivityForResult(new Intent(this, UplinkDataTestActivity.class), AppConstants.REQUEST_CODE_REFRESH);
    }

    public void deviceInfo(View view) {
        showLoadingProgressDialog();
        mMokoService.getDeviceInfo();
    }

    public void ota(View view) {
        Intent intent = new Intent(this, OTAActivity.class);
        intent.putExtra(AppConstants.EXTRA_KEY_DEVICE_NAME, mDeviceName);
        intent.putExtra(AppConstants.EXTRA_KEY_DEVICE_MAC, mDeviceMac);
        startActivityForResult(intent, AppConstants.REQUEST_CODE_REFRESH);
    }

    public void log(View view) {
        startActivityForResult(new Intent(this, LogActivity.class), AppConstants.REQUEST_CODE_REFRESH);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AppConstants.REQUEST_CODE_DEVICE_SETTING) {
            if (resultCode == RESULT_OK) {
                backToHome();
            } else {
                showLoadingProgressDialog();
                tvConnectStatus.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mMokoService.getBasicInfo();
                    }
                }, 500);
            }
        } else if (requestCode == AppConstants.REQUEST_CODE_REFRESH) {
            showLoadingProgressDialog();
            tvConnectStatus.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mMokoService.getBasicInfo();
                }
            }, 500);
        }
    }

    public void bleSetting(View view) {
        showLoadingProgressDialog();
        mMokoService.getBleInfo();
    }
}
