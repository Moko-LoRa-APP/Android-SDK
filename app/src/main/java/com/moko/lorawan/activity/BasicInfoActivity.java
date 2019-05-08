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
import android.view.View;
import android.widget.TextView;

import com.moko.lorawan.AppConstants;
import com.moko.lorawan.R;
import com.moko.lorawan.dialog.LoadingDialog;
import com.moko.lorawan.service.MokoService;
import com.moko.support.MokoConstants;
import com.moko.support.MokoSupport;
import com.moko.support.entity.OrderEnum;
import com.moko.support.task.OrderTaskResponse;

import butterknife.Bind;
import butterknife.ButterKnife;

public class BasicInfoActivity extends BaseActivity {

    @Bind(R.id.tv_device_name)
    TextView tvDeviceName;
    @Bind(R.id.tv_connect_status)
    TextView tvConnectStatus;
    @Bind(R.id.tv_device_setting)
    TextView tvDeviceSetting;

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
        tvDeviceName.setText(mDeviceName);
        int connectStatus = MokoSupport.getInstance().getConnectStatus();
        int region = MokoSupport.getInstance().getRegion();
        int classType = MokoSupport.getInstance().getClassType();
        int uploadMode = MokoSupport.getInstance().getUploadMode();
        connectStatusStrs = getResources().getStringArray(R.array.connect_status);
        regions = getResources().getStringArray(R.array.region);
        classTypes = getResources().getStringArray(R.array.class_type);
        uploadModes = getResources().getStringArray(R.array.upload_mode);
        tvConnectStatus.setText(connectStatusStrs[connectStatus]);
        tvDeviceSetting.setText(String.format("%s/%s/%s", uploadModes[uploadMode - 1], regions[region], classTypes[classType - 1]));
        bindService(new Intent(this, MokoService.class), mServiceConnection, BIND_AUTO_CREATE);
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

                }
                if (MokoConstants.ACTION_ORDER_FINISH.equals(action)) {
                    dismissLoadingProgressDialog();
                }
                if (MokoConstants.ACTION_ORDER_RESULT.equals(action)) {
                    OrderTaskResponse response = (OrderTaskResponse) intent.getSerializableExtra(MokoConstants.EXTRA_KEY_RESPONSE_ORDER_TASK);
                    OrderEnum orderEnum = response.order;
                    switch (orderEnum) {
                        case READ_LORA_FIRMWARE:
                            // 跳转设备信息页面
                            startActivity(new Intent(BasicInfoActivity.this, DeviceInfoActivity.class));
                            break;
                        case READ_9_AXIS_ANGLE:
                            // 跳转9轴和传感器页面
                            startActivity(new Intent(BasicInfoActivity.this, GPSAndSensorDataActivity.class));
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
    }

    public void gpsAndSensorData(View view) {
        showLoadingProgressDialog();
        mMokoService.getGPSAndSensorData();
    }

    public void uplinkTest(View view) {
    }

    public void deviceInfo(View view) {
        showLoadingProgressDialog();
        mMokoService.getDeviceInfo();
    }

    public void ota(View view) {
        Intent intent = new Intent(this, OTAActivity.class);
        intent.putExtra(AppConstants.EXTRA_KEY_DEVICE_NAME, mDeviceName);
        intent.putExtra(AppConstants.EXTRA_KEY_DEVICE_MAC, mDeviceMac);
        startActivity(intent);
    }

    public void log(View view) {
    }
}
