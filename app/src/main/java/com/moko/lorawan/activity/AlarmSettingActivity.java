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
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.moko.lorawan.R;
import com.moko.lorawan.dialog.BottomDialog;
import com.moko.lorawan.dialog.LoadingDialog;
import com.moko.lorawan.service.MokoService;
import com.moko.lorawan.utils.ToastUtils;
import com.moko.support.MokoConstants;
import com.moko.support.MokoSupport;
import com.moko.support.entity.OrderEnum;
import com.moko.support.event.ConnectStatusEvent;
import com.moko.support.task.OrderTask;
import com.moko.support.task.OrderTaskResponse;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;

public class AlarmSettingActivity extends BaseActivity {


    private final String FILTER_ASCII = "\\A\\p{ASCII}*\\z";

    @Bind(R.id.cb_alarm_switch)
    CheckBox cbAlarmSwitch;
    @Bind(R.id.cb_gps_switch)
    CheckBox cbGpsSwitch;
    @Bind(R.id.cb_vibration_switch)
    CheckBox cbVibrationSwitch;
    @Bind(R.id.et_filter_name)
    EditText etFilterName;
    @Bind(R.id.et_filter_rssi)
    EditText etFilterRssi;
    @Bind(R.id.tv_trigger_mode)
    TextView tvTriggerMode;
    @Bind(R.id.et_report_interval)
    EditText etReportInterval;
    @Bind(R.id.tv_save)
    TextView tvSave;

    private MokoService mMokoService;
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
        cbAlarmSwitch.setChecked(MokoSupport.getInstance().alarmStatus != 0);
        cbAlarmSwitch.setEnabled(MokoSupport.getInstance().alarmStatus != 0);

        cbGpsSwitch.setChecked(MokoSupport.getInstance().alarmGpsSwitch != 0);
        cbVibrationSwitch.setChecked(MokoSupport.getInstance().alamrVibrationSwitch != 0);

        etFilterName.setText(MokoSupport.getInstance().filterName);
        mTriggerMode = getResources().getStringArray(R.array.trigger_mode);
        tvTriggerMode.setText(mTriggerMode[mModeSelected]);
        int filterRssi = MokoSupport.getInstance().filterRssi;
        etFilterRssi.setText(filterRssi == 0 ? "0" : String.format("-%d", MokoSupport.getInstance().filterRssi));
        etReportInterval.setText(MokoSupport.getInstance().alarmUploadInterval + "");
        bindService(new Intent(this, MokoService.class), mServiceConnection, BIND_AUTO_CREATE);
        EventBus.getDefault().register(this);
        cbAlarmSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked)
                    cbAlarmSwitch.setEnabled(false);
            }
        });
        InputFilter inputFilter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                if (!(source + "").matches(FILTER_ASCII)) {
                    return "";
                }

                return null;
            }
        };
        etFilterName.setFilters(new InputFilter[]{new InputFilter.LengthFilter(11), inputFilter});
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
            filter.setPriority(300);
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
                            finish();
                            break;
                    }
                }
                if (MokoConstants.ACTION_ORDER_TIMEOUT.equals(action)) {

                }
                if (MokoConstants.ACTION_ORDER_FINISH.equals(action)) {
                    dismissLoadingProgressDialog();
                    if (!mIsFailed) {
                        ToastUtils.showToast(AlarmSettingActivity.this, "Execute after bluetooth disconnect");
                    } else {
                        ToastUtils.showToast(AlarmSettingActivity.this, "Error");
                    }
                }
                if (MokoConstants.ACTION_ORDER_RESULT.equals(action)) {
                    abortBroadcast();
                    OrderTaskResponse response = (OrderTaskResponse) intent.getSerializableExtra(MokoConstants.EXTRA_KEY_RESPONSE_ORDER_TASK);
                    OrderEnum orderEnum = response.order;
                    byte[] value = response.responseValue;
                    switch (orderEnum) {
                        case WRITE_FILTER_NAME:
                        case WRITE_FILTER_RSSI:
                        case WRITE_ALARM_STATUS:
                        case WRITE_ALARM_GPS_SWITCH:
                        case WRITE_ALARM_VIBRATION_SWITCH:
                        case WRITE_ALARM_TRIGGER_MODE:
                        case WRITE_ALARM_UPLOAD_INTERVAL:
                            if ((value[3] & 0xff) != 0xAA) {
                                mIsFailed = true;
                            }
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
        String filterName = etFilterName.getText().toString();

        String filterRssi = etFilterRssi.getText().toString();
        if (TextUtils.isEmpty(filterRssi)) {
            ToastUtils.showToast(this, "Filter RSSI is empty");
            return;
        }
        int filterRssiInt = Integer.parseInt(filterRssi);
        if (filterRssiInt < -100 || filterRssiInt > 0) {
            ToastUtils.showToast(this, "Filter RSSI range -100~0");
            return;
        }

        orderTasks.add(mMokoService.getFilterNameOrderTask(filterName));
        orderTasks.add(mMokoService.getFilterRssiOrderTask(filterRssiInt));
        orderTasks.add(mMokoService.getAlarmStatusOrderTask(cbAlarmSwitch.isChecked() ? 1 : 0));
        orderTasks.add(mMokoService.getAlarmGPSSwitchOrderTask(cbGpsSwitch.isChecked() ? 1 : 0));
        orderTasks.add(mMokoService.getAlarmTriggerModeOrderTask(mModeSelected + 1));
        orderTasks.add(mMokoService.getAlarmUploadIntervalOrderTask(intervalInt));
        orderTasks.add(mMokoService.getAlarmVibrationSwitchOrderTask(cbVibrationSwitch.isChecked() ? 1 : 0));
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
