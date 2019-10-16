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
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.moko.lorawan.R;
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

public class ScanSettingActivity extends BaseActivity {


    @Bind(R.id.cb_scan_switch)
    CheckBox cbScanSwitch;
    @Bind(R.id.et_filter_name)
    EditText etFilterName;
    @Bind(R.id.et_filter_rssi)
    EditText etFilterRssi;
    @Bind(R.id.et_scan_time)
    EditText etScanTime;
    @Bind(R.id.et_report_interval)
    EditText etReportInterval;
    @Bind(R.id.ll_scan_filter)
    LinearLayout llScanFilter;
    @Bind(R.id.tv_save)
    TextView tvSave;
    private MokoService mMokoService;
    private boolean mReceiverTag = false;
    private boolean mIsFailed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_setting);
        ButterKnife.bind(this);
        llScanFilter.setVisibility(MokoSupport.getInstance().scanTime == 0 ? View.GONE : View.VISIBLE);
        cbScanSwitch.setChecked(MokoSupport.getInstance().scanTime != 0);
        etFilterName.setText(MokoSupport.getInstance().filterName);
        etFilterRssi.setText(String.format("-%d", MokoSupport.getInstance().filterRssi));
        etScanTime.setText(MokoSupport.getInstance().scanTime + "");
        etReportInterval.setText(MokoSupport.getInstance().uploadInterval + "");
        bindService(new Intent(this, MokoService.class), mServiceConnection, BIND_AUTO_CREATE);
        EventBus.getDefault().register(this);
        cbScanSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                llScanFilter.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });
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
                        ToastUtils.showToast(ScanSettingActivity.this, "Success");
                    } else {
                        ToastUtils.showToast(ScanSettingActivity.this, "Error");
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
                        case WRITE_UPLOAD_INTERVAL:
                        case WRITE_SCAN_TIME:
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
        if (cbScanSwitch.isChecked()) {
            String reportInterval = etReportInterval.getText().toString();
            if (TextUtils.isEmpty(reportInterval)) {
                ToastUtils.showToast(this, "Report Interval is empty");
                return;
            }
            long interval = Long.parseLong(reportInterval);
            if (interval < 1 || interval > 65535) {
                ToastUtils.showToast(this, "Report Interval range 1~65535");
                return;
            }
            int intervalInt = Integer.parseInt(reportInterval);

            String filterName = etFilterName.getText().toString();
            if (TextUtils.isEmpty(filterName)) {
                ToastUtils.showToast(this, "Filter Name is empty");
                return;
            }

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

            String scanTime = etScanTime.getText().toString();
            if (TextUtils.isEmpty(scanTime)) {
                ToastUtils.showToast(this, "Scan Time is empty");
                return;
            }
            long scanTimeLong = Long.parseLong(scanTime);
            if (scanTimeLong < 1 || scanTimeLong > 65535) {
                ToastUtils.showToast(this, "Scan Time range 1~65535");
                return;
            }
            int scanTimeInt = Integer.parseInt(scanTime);

            orderTasks.add(mMokoService.getUploadIntervalOrderTask(intervalInt));
            orderTasks.add(mMokoService.getFilterNameOrderTask(filterName));
            orderTasks.add(mMokoService.getFilterRssiOrderTask(filterRssiInt));
            orderTasks.add(mMokoService.getScanTimeOrderTask(scanTimeInt));
            MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
        } else {
            orderTasks.add(mMokoService.getScanTimeOrderTask(0));
            MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
        }
        showLoadingProgressDialog();
    }
}
