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
import com.moko.lorawan.dialog.AlertMessageDialog;
import com.moko.lorawan.dialog.LoadingDialog;
import com.moko.support.MokoConstants;
import com.moko.support.MokoSupport;
import com.moko.support.entity.DeviceTypeEnum;
import com.moko.support.entity.OrderEnum;
import com.moko.support.event.ConnectStatusEvent;
import com.moko.support.event.OrderTaskResponseEvent;
import com.moko.support.task.OrderTask;
import com.moko.support.task.OrderTaskResponse;
import com.moko.support.task.Read9AxisATask;
import com.moko.support.task.Read9AxisAngleTask;
import com.moko.support.task.Read9AxisGTask;
import com.moko.support.task.Read9AxisMTask;
import com.moko.support.task.ReadAlarmStatusTask;
import com.moko.support.task.ReadBleFirmwareTask;
import com.moko.support.task.ReadClassTypeTask;
import com.moko.support.task.ReadCompanyNameTask;
import com.moko.support.task.ReadConnectStatusTask;
import com.moko.support.task.ReadGPSTask;
import com.moko.support.task.ReadHumiDataTask;
import com.moko.support.task.ReadI2CIntervalTask;
import com.moko.support.task.ReadLoraFirmwareTask;
import com.moko.support.task.ReadModelNameTask;
import com.moko.support.task.ReadRegionTask;
import com.moko.support.task.ReadTempDataTask;
import com.moko.support.task.ReadUploadModeTask;
import com.moko.support.task.WriteRTCTimeTask;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;

public class BasicInfoActivity extends BaseActivity {

    @Bind(R.id.tv_connect_status)
    TextView tvConnectStatus;
    @Bind(R.id.tv_device_name)
    TextView tvDeviceName;
    @Bind(R.id.rl_gps_axis)
    RelativeLayout rlGpsAxis;
    @Bind(R.id.rl_sensor_data)
    RelativeLayout rlSensorData;
    @Bind(R.id.rl_alarm_status)
    RelativeLayout rlAlarmStatus;
    @Bind(R.id.tv_alarm_status)
    TextView tvAlarmStatus;

    private String[] connectStatusStrs;
    private boolean mReceiverTag = false;
    private String mDeviceName;
    private String mDeviceMac;
    private int disConnectType;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_basic_info);
        ButterKnife.bind(this);
        mDeviceName = getIntent().getStringExtra(AppConstants.EXTRA_KEY_DEVICE_NAME);
        mDeviceMac = getIntent().getStringExtra(AppConstants.EXTRA_KEY_DEVICE_MAC);
        int connectStatus = MokoSupport.getInstance().getConnectStatus();
        int deviceType = MokoSupport.deviceTypeEnum.getDeviceType();
        rlGpsAxis.setVisibility(deviceType == 0 ? View.VISIBLE : View.GONE);
        rlSensorData.setVisibility(deviceType == 1 ? View.VISIBLE : View.GONE);
        rlAlarmStatus.setVisibility(deviceType == 3 ? View.VISIBLE : View.GONE);
        tvAlarmStatus.setText(MokoSupport.getInstance().alarmStatus == 0 ? "Off" : "On");
        String modelName = MokoSupport.getInstance().getModelName();
        connectStatusStrs = getResources().getStringArray(R.array.connect_status);
        tvConnectStatus.setText(connectStatusStrs[connectStatus]);
        tvDeviceName.setText(modelName);
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
            AlertMessageDialog dialog = new AlertMessageDialog();
            if (disConnectType == 3) {
                dialog.setTitle("Change Password");
                dialog.setMessage("Password changed successfully!Please reconnect the device.");
                dialog.setConfirm("OK");
            } else if (disConnectType == 2) {
                dialog.setMessage("No data communication for 2 minutes, the device is disconnected.");
                dialog.setConfirm("OK");
            } else {
                dialog.setTitle("Dismiss");
                dialog.setMessage("The Beacon disconnected!");
                dialog.setConfirm("Exit");
            }
            dialog.setCancelGone();
            dialog.setOnAlertConfirmListener(() -> {
                if (disConnectType == 3) {
                    setResult(RESULT_OK);
                }
                finish();
            });
            dialog.show(getSupportFragmentManager());
        }
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 100)
    public void onOrderTaskResponseEvent(OrderTaskResponseEvent event) {
        EventBus.getDefault().cancelEventDelivery(event);
        final String action = event.getAction();
        runOnUiThread(() -> {
            if (MokoConstants.ACTION_CURRENT_DATA.equals(action)) {
                OrderTaskResponse response = event.getResponse();
                OrderEnum order = response.order;
                int responseType = response.responseType;
                byte[] value = response.responseValue;
                switch (order) {
                    case DISCONNECT_TYPE:
                        if (value != null && value.length == 4) {
                            int type = value[3] & 0xFF;
                            disConnectType = type;
                            if (type == 1) {
                                // valid password timeout
                            } else if (type == 3) {
                                // change password success
                            } else if (type == 2) {
                                // no data exchange timeout
                            }
                        }
                        break;
                }
            }
            if (MokoConstants.ACTION_ORDER_TIMEOUT.equals(action)) {
            }
            if (MokoConstants.ACTION_ORDER_FINISH.equals(action)) {
                dismissLoadingProgressDialog();
            }
            if (MokoConstants.ACTION_ORDER_RESULT.equals(action)) {
                OrderTaskResponse response = event.getResponse();
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
                    case READ_UPLOAD_MODE:
                        // 跳转设置页面
                        Intent intent = new Intent(this, SettingActivity.class);
                        intent.putExtra(AppConstants.EXTRA_KEY_DEVICE_MAC, mDeviceMac);
                        startActivityForResult(intent, AppConstants.REQUEST_CODE_SETTING);
                        break;
                    case READ_HUMI:
                        // 跳转温湿度传感器页面
                        startActivityForResult(new Intent(BasicInfoActivity.this, SensorDataActivity.class), AppConstants.REQUEST_CODE_REFRESH);
                        break;
                    case READ_CONNECT_STATUS:
                        int connectStatus = MokoSupport.getInstance().getConnectStatus();
                        tvConnectStatus.setText(connectStatusStrs[connectStatus]);
                        tvAlarmStatus.setText(MokoSupport.getInstance().alarmStatus == 0 ? "Off" : "On");
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
                            backToHome();
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
        backToHome();
    }

    private void backToHome() {
        MokoSupport.getInstance().disConnectBle();
    }

    @Override
    public void onBackPressed() {
        backToHome();
    }

    public void thSensorData(View view) {
        showLoadingProgressDialog();
        ArrayList<OrderTask> orderTasks = new ArrayList<>();
        orderTasks.add(new ReadI2CIntervalTask());
        orderTasks.add(new ReadTempDataTask());
        orderTasks.add(new ReadHumiDataTask());
        MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }

    public void gpsAndSensorData(View view) {
        showLoadingProgressDialog();
        ArrayList<OrderTask> orderTasks = new ArrayList<>();
        orderTasks.add(new ReadGPSTask());
        orderTasks.add(new Read9AxisATask());
        orderTasks.add(new Read9AxisGTask());
        orderTasks.add(new Read9AxisMTask());
        orderTasks.add(new Read9AxisAngleTask());
        MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }

    public void uplinkTest(View view) {
        startActivityForResult(new Intent(this, UplinkDataTestActivity.class), AppConstants.REQUEST_CODE_REFRESH);
    }

    public void deviceInfo(View view) {
        showLoadingProgressDialog();
        ArrayList<OrderTask> orderTasks = new ArrayList<>();
        orderTasks.add(new ReadCompanyNameTask());
        orderTasks.add(new ReadModelNameTask());
        orderTasks.add(new ReadBleFirmwareTask());
        orderTasks.add(new ReadLoraFirmwareTask());
        MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
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
        if (requestCode == AppConstants.REQUEST_CODE_SETTING) {
            if (resultCode == RESULT_OK) {
                backToHome();
            } else {
                tvConnectStatus.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!MokoSupport.getInstance().isConnDevice(BasicInfoActivity.this, mDeviceMac))
                            return;
                        showLoadingProgressDialog();
                        ArrayList<OrderTask> orderTasks = new ArrayList<>();
                        if (MokoSupport.deviceTypeEnum == DeviceTypeEnum.LW004_BP)
                            orderTasks.add(new ReadAlarmStatusTask());
                        orderTasks.add(new ReadModelNameTask());

                        if (MokoSupport.deviceTypeEnum != DeviceTypeEnum.LW001_BG)
                            orderTasks.add(new WriteRTCTimeTask());
                        orderTasks.add(new ReadConnectStatusTask());
                        MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
                    }
                }, 500);
            }
        } else if (requestCode == AppConstants.REQUEST_CODE_REFRESH) {
            tvConnectStatus.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!MokoSupport.getInstance().isConnDevice(BasicInfoActivity.this, mDeviceMac))
                        return;
                    if (disConnectType == 0) {
                        showLoadingProgressDialog();
                        ArrayList<OrderTask> orderTasks = new ArrayList<>();
                        if (MokoSupport.deviceTypeEnum == DeviceTypeEnum.LW004_BP)
                            orderTasks.add(new ReadAlarmStatusTask());
                        orderTasks.add(new ReadModelNameTask());

                        if (MokoSupport.deviceTypeEnum != DeviceTypeEnum.LW001_BG)
                            orderTasks.add(new WriteRTCTimeTask());
                        orderTasks.add(new ReadConnectStatusTask());
                        MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
                    }
                }
            }, 500);
        }
    }

    public void setting(View view) {
        showLoadingProgressDialog();
        ArrayList<OrderTask> orderTasks = new ArrayList<>();
        orderTasks.add(new ReadRegionTask());
        orderTasks.add(new ReadClassTypeTask());
        orderTasks.add(new ReadUploadModeTask());
        MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }
}
