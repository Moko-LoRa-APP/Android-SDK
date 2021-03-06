package com.moko.lorawan.activity;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.moko.lorawan.R;
import com.moko.lorawan.dialog.AlertMessageDialog;
import com.moko.lorawan.dialog.BottomDialog;
import com.moko.lorawan.dialog.LoadingDialog;
import com.moko.lorawan.utils.OrderTaskAssembler;
import com.moko.lorawan.utils.ToastUtils;
import com.moko.support.MokoConstants;
import com.moko.support.MokoSupport;
import com.moko.support.entity.OrderEnum;
import com.moko.support.event.ConnectStatusEvent;
import com.moko.support.event.OrderTaskResponseEvent;
import com.moko.support.task.OrderTask;
import com.moko.support.task.OrderTaskResponse;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class AlarmSettingActivity extends BaseActivity {

    @BindView(R.id.cb_vibration_switch)
    CheckBox cbVibrationSwitch;
    @BindView(R.id.tv_trigger_mode)
    TextView tvTriggerMode;
    @BindView(R.id.et_report_interval)
    EditText etReportInterval;
    @BindView(R.id.tv_save)
    TextView tvSave;
    @BindView(R.id.et_ble_scan_time)
    EditText etBleScanTime;
    @BindView(R.id.et_reported_num)
    EditText etReportedNum;

    private boolean mReceiverTag = false;
    private boolean mIsFailed;
    private String[] mTriggerMode;
    private int mModeSelected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_setting);
        ButterKnife.bind(this);
        final int triggerMode = MokoSupport.getInstance().alarmTriggerMode;
        if (triggerMode < 1 || triggerMode > 3) {
            finish();
            return;
        }
        mModeSelected = triggerMode - 1;
        cbVibrationSwitch.setChecked(MokoSupport.getInstance().alamrVibrationSwitch != 0);
        mTriggerMode = getResources().getStringArray(R.array.trigger_mode);
        tvTriggerMode.setText(mTriggerMode[mModeSelected]);
        etReportInterval.setText(MokoSupport.getInstance().alarmUploadInterval + "");
        etBleScanTime.setText(MokoSupport.getInstance().alarmScanTime + "");
        etReportedNum.setText(MokoSupport.getInstance().alarmReportNumer + "");
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
            finish();
        }
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 300)
    public void onOrderTaskResponseEvent(OrderTaskResponseEvent event) {
        final String action = event.getAction();
        if (!MokoConstants.ACTION_CURRENT_DATA.equals(action)) {
            EventBus.getDefault().cancelEventDelivery(event);
        }
        runOnUiThread(() -> {
            if (MokoConstants.ACTION_ORDER_TIMEOUT.equals(action)) {

            }
            if (MokoConstants.ACTION_ORDER_FINISH.equals(action)) {
                dismissLoadingProgressDialog();
                if (!mIsFailed) {
                    ToastUtils.showToast(AlarmSettingActivity.this, "Success");
                } else {
                    ToastUtils.showToast(AlarmSettingActivity.this, "Error");
                }

            }
            if (MokoConstants.ACTION_ORDER_RESULT.equals(action)) {
                OrderTaskResponse response = event.getResponse();
                OrderEnum orderEnum = response.order;
                byte[] value = response.responseValue;
                switch (orderEnum) {
                    case WRITE_ALARM_GPS_SWITCH:
                    case WRITE_ALARM_VIBRATION_SWITCH:
                    case WRITE_ALARM_TRIGGER_MODE:
                    case WRITE_ALARM_UPLOAD_INTERVAL:
                    case WRITE_ALARM_REPORT_NUMBER:
                        if ((value[3] & 0xff) != 0xAA) {
                            mIsFailed = true;
                        }
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

    public void onSave(View view) {
        ArrayList<OrderTask> orderTasks = new ArrayList<>();
        String reportInterval = etReportInterval.getText().toString();
        if (TextUtils.isEmpty(reportInterval)) {
            ToastUtils.showToast(this, "Report Interval is empty");
            return;
        }
        int intervalInt = Integer.parseInt(reportInterval);
        if (intervalInt < 10 || intervalInt > 600) {
            ToastUtils.showToast(this, "Report Interval range 10~600");
            return;
        }
        String scanTime = etBleScanTime.getText().toString();
        if (TextUtils.isEmpty(scanTime)) {
            ToastUtils.showToast(this, "Scan Time is empty");
            return;
        }
        int timeInt = Integer.parseInt(scanTime);
        if (timeInt < 10 || timeInt > 600) {
            ToastUtils.showToast(this, "Scan Time range 10~600");
            return;
        }
        if (timeInt > intervalInt) {
            AlertMessageDialog dialog = new AlertMessageDialog();
            dialog.setMessage("Alarm Report Interval  should be no less than Valid BLE Data Filter Interval");
            dialog.setConfirm("OK");
            dialog.setCancelGone();
            dialog.show(getSupportFragmentManager());
            return;
        }
        String reportNum = etReportedNum.getText().toString();
        if (TextUtils.isEmpty(reportNum)) {
            ToastUtils.showToast(this, "Quantity of Reported Device is empty");
            return;
        }
        int reportNumInt = Integer.parseInt(reportNum);
        if (reportNumInt < 1 || reportNumInt > 4) {
            ToastUtils.showToast(this, "Quantity of Reported Device range 1~4");
            return;
        }
        orderTasks.add(OrderTaskAssembler.setAlarmTriggerModeOrderTask(mModeSelected + 1));
        orderTasks.add(OrderTaskAssembler.setAlarmUploadIntervalOrderTask(intervalInt, timeInt));
        orderTasks.add(OrderTaskAssembler.setAlarmVibrationSwitchOrderTask(cbVibrationSwitch.isChecked() ? 1 : 0));
        orderTasks.add(OrderTaskAssembler.setAlarmReportNumberOrderTask(reportNumInt));
        MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
        showLoadingProgressDialog();
    }

    public void selectTriggerMode(View view) {
        ArrayList<String> modes = new ArrayList<>();
        for (int i = 0; i < mTriggerMode.length; i++) {
            modes.add(mTriggerMode[i]);
        }
        BottomDialog bottomDialog = new BottomDialog();
        bottomDialog.setDatas(modes, mModeSelected);
        bottomDialog.setListener(new BottomDialog.OnBottomListener() {
            @Override
            public void onValueSelected(int value) {
                mModeSelected = value;
                tvTriggerMode.setText(mTriggerMode[mModeSelected]);
            }
        });
        bottomDialog.show(getSupportFragmentManager());
    }
}
