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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.moko.lorawan.R;
import com.moko.lorawan.dialog.AlertMessageDialog;
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

public class SensorDataActivity extends BaseActivity {


    @Bind(R.id.tv_temp_current)
    TextView tvTempCurrent;
    @Bind(R.id.tv_humi_current)
    TextView tvHumiCurrent;
    @Bind(R.id.et_i2c_interval)
    EditText etI2cInterval;
    @Bind(R.id.tv_temp_alarm)
    TextView tvTempAlarm;
    @Bind(R.id.et_temp_low)
    EditText etTempLow;
    @Bind(R.id.et_temp_high)
    EditText etTempHigh;
    @Bind(R.id.ll_temp_alarm)
    LinearLayout llTempAlarm;
    @Bind(R.id.tv_humi_alarm)
    TextView tvHumiAlarm;
    @Bind(R.id.et_humi_low)
    EditText etHumiLow;
    @Bind(R.id.et_humi_high)
    EditText etHumiHigh;
    @Bind(R.id.ll_humi_alarm)
    LinearLayout llHumiAlarm;
    @Bind(R.id.tv_save)
    TextView tvSave;
    private MokoService mMokoService;
    private boolean mReceiverTag = false;
    private String[] mAlarms;
    private int mTempSelected;
    private int mHumiSelected;
    private boolean mIsFailed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_data);
        ButterKnife.bind(this);
        mAlarms = getResources().getStringArray(R.array.alarm);

        String i2cInterval = String.valueOf(MokoSupport.getInstance().i2cInterval);
        etI2cInterval.setText(i2cInterval);
        etI2cInterval.setSelection(i2cInterval.length());

        mTempSelected = MokoSupport.getInstance().tempEnable;
        llTempAlarm.setVisibility(mTempSelected == 0 ? View.GONE : View.VISIBLE);
        etTempLow.setText(MokoSupport.getInstance().tempLow);
        etTempLow.setSelection(MokoSupport.getInstance().tempLow.length());
        etTempHigh.setText(MokoSupport.getInstance().tempHigh);
        etTempHigh.setSelection(MokoSupport.getInstance().tempHigh.length());
        tvTempCurrent.setText(MokoSupport.getInstance().tempCurrent);
        tvTempAlarm.setText(mAlarms[mTempSelected]);

        mHumiSelected = MokoSupport.getInstance().humiEnable;
        llHumiAlarm.setVisibility(mHumiSelected == 0 ? View.GONE : View.VISIBLE);
        etHumiLow.setText(MokoSupport.getInstance().humiLow);
        etHumiLow.setSelection(MokoSupport.getInstance().humiLow.length());
        etHumiHigh.setText(MokoSupport.getInstance().humiHigh);
        etHumiHigh.setSelection(MokoSupport.getInstance().humiHigh.length());
        tvHumiCurrent.setText(MokoSupport.getInstance().humiCurrent);
        tvHumiAlarm.setText(mAlarms[mHumiSelected]);

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
                        ToastUtils.showToast(SensorDataActivity.this, "Success");
                    } else {
                        ToastUtils.showToast(SensorDataActivity.this, "Error");
                    }
                }
                if (MokoConstants.ACTION_ORDER_RESULT.equals(action)) {
                    abortBroadcast();
                    OrderTaskResponse response = (OrderTaskResponse) intent.getSerializableExtra(MokoConstants.EXTRA_KEY_RESPONSE_ORDER_TASK);
                    OrderEnum orderEnum = response.order;
                    byte[] value = response.responseValue;
                    switch (orderEnum) {
                        case WRITE_I2C:
                        case WRITE_TEMP:
                        case WRITE_HUMI:
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

    public void selectTempAlarm(View view) {
        ArrayList<String> alarms = new ArrayList<>();
        for (int i = 0; i < mAlarms.length; i++) {
            alarms.add(mAlarms[i]);
        }
        BottomDialog bottomDialog = new BottomDialog();
        bottomDialog.setDatas(alarms, mTempSelected);
        bottomDialog.setListener(new BottomDialog.OnBottomListener() {
            @Override
            public void onValueSelected(int value) {
                mTempSelected = value;
                tvTempAlarm.setText(mAlarms[mTempSelected]);
                llTempAlarm.setVisibility(mTempSelected == 0 ? View.GONE : View.VISIBLE);
            }
        });
        bottomDialog.show(getSupportFragmentManager());
    }

    public void selectHumiAlarm(View view) {
        ArrayList<String> alarms = new ArrayList<>();
        for (int i = 0; i < mAlarms.length; i++) {
            alarms.add(mAlarms[i]);
        }
        BottomDialog bottomDialog = new BottomDialog();
        bottomDialog.setDatas(alarms, mHumiSelected);
        bottomDialog.setListener(new BottomDialog.OnBottomListener() {
            @Override
            public void onValueSelected(int value) {
                mHumiSelected = value;
                tvHumiAlarm.setText(mAlarms[mHumiSelected]);
                llHumiAlarm.setVisibility(mHumiSelected == 0 ? View.GONE : View.VISIBLE);
            }
        });
        bottomDialog.show(getSupportFragmentManager());
    }

    public void resetData(View view) {
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setTitle("Reset All Parameters");
        dialog.setMessage("Please confirm whether to reset all parameters?");
        dialog.setOnAlertConfirmListener(new AlertMessageDialog.OnAlertConfirmListener() {
            @Override
            public void onClick() {
                showLoadingProgressDialog();
                MokoSupport.getInstance().sendOrder(mMokoService.getResetOrderTask());
            }
        });
        dialog.show(getSupportFragmentManager());
    }

    public void onSave(View view) {
        ArrayList<OrderTask> orderTasks = new ArrayList<>();
        String i2cInterval = etI2cInterval.getText().toString();
        if (TextUtils.isEmpty(i2cInterval)) {
            ToastUtils.showToast(this, "Reporting Interval is empty");
            return;
        }
        long interval = Long.parseLong(i2cInterval);
        if (interval < 10 || interval > 864000) {
            ToastUtils.showToast(this, "Reporting Interval range 10~864000");
            return;
        }
        if (mTempSelected == 1) {
            String tempLow = etTempLow.getText().toString();
            if (TextUtils.isEmpty(tempLow)) {
                ToastUtils.showToast(this, "Low Temperature Threshold is empty");
                return;
            }
            float tempLowInt = Float.parseFloat(tempLow);
            if (tempLowInt < 0 || tempLowInt > 65) {
                ToastUtils.showToast(this, "Low Temperature Threshold range 0~65");
                return;
            }
            String tempHigh = etTempHigh.getText().toString();
            if (TextUtils.isEmpty(tempHigh)) {
                ToastUtils.showToast(this, "High Temperature Threshold is empty");
                return;
            }
            float tempHighInt = Float.parseFloat(tempHigh);
            if (tempHighInt < 0 || tempHighInt > 65) {
                ToastUtils.showToast(this, "High Temperature Threshold range 0~65");
                return;
            }
            if (tempLowInt >=  tempHighInt) {
                ToastUtils.showToast(this, "Temperature Threshold error");
                return;
            }
            orderTasks.add(mMokoService.getTempDataOrderTask(mTempSelected, (int) (tempLowInt * 100), (int) (tempHighInt * 100)));
        } else {
            orderTasks.add(mMokoService.getTempDataOrderTask(mTempSelected, 0, 0));
        }
        if (mHumiSelected == 1) {
            String humiLow = etHumiLow.getText().toString();
            if (TextUtils.isEmpty(humiLow)) {
                ToastUtils.showToast(this, "Low Humidity Threshold is empty");
                return;
            }
            float humiLowInt = Float.parseFloat(humiLow);
            if (humiLowInt < 10 || humiLowInt > 90) {
                ToastUtils.showToast(this, "Low Humidity Threshold range 10~90");
                return;
            }

            String humiHigh = etHumiHigh.getText().toString();
            if (TextUtils.isEmpty(humiHigh)) {
                ToastUtils.showToast(this, "High Humidity Threshold is empty");
                return;
            }
            float humiHighInt = Float.parseFloat(humiHigh);
            if (humiHighInt < 10 || humiHighInt > 90) {
                ToastUtils.showToast(this, "High Humidity Threshold range 0~90");
                return;
            }
            if (humiLowInt >=  humiHighInt) {
                ToastUtils.showToast(this, "Humidity Threshold error");
                return;
            }
            orderTasks.add(mMokoService.getHumiDataOrderTask(mHumiSelected, (int) (humiLowInt * 100), (int) (humiHighInt * 100)));
        } else {
            orderTasks.add(mMokoService.getHumiDataOrderTask(mHumiSelected, 0, 0));
        }
        mIsFailed = false;
        int intervalInt = Integer.parseInt(i2cInterval);
        orderTasks.add(mMokoService.getI2CIntervalOrderTask(intervalInt));
        MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
        showLoadingProgressDialog();
    }
}
