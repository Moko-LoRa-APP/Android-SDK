package com.moko.lorawan.activity;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.moko.lorawan.R;
import com.moko.lorawan.dialog.AlertMessageDialog;
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

import butterknife.Bind;
import butterknife.ButterKnife;

public class SensorDataActivity extends BaseActivity implements RadioGroup.OnCheckedChangeListener {


    @Bind(R.id.tv_temp_current)
    TextView tvTempCurrent;
    @Bind(R.id.tv_humi_current)
    TextView tvHumiCurrent;
    @Bind(R.id.et_i2c_interval)
    EditText etI2cInterval;
    @Bind(R.id.et_temp_low)
    EditText etTempLow;
    @Bind(R.id.et_temp_high)
    EditText etTempHigh;
    @Bind(R.id.ll_temp_alarm)
    LinearLayout llTempAlarm;
    @Bind(R.id.et_humi_low)
    EditText etHumiLow;
    @Bind(R.id.et_humi_high)
    EditText etHumiHigh;
    @Bind(R.id.ll_humi_alarm)
    LinearLayout llHumiAlarm;
    @Bind(R.id.tv_save)
    TextView tvSave;
    @Bind(R.id.rb_temp_disable)
    RadioButton rbTempDisable;
    @Bind(R.id.rb_temp_enable)
    RadioButton rbTempEnable;
    @Bind(R.id.rg_temp)
    RadioGroup rgTemp;
    @Bind(R.id.rb_humi_disable)
    RadioButton rbHumiDisable;
    @Bind(R.id.rb_humi_enable)
    RadioButton rbHumiEnable;
    @Bind(R.id.rg_humi)
    RadioGroup rgHumi;
    private boolean mReceiverTag = false;
    private String[] mAlarms;
    private int mTempSelected;
    private int mHumiSelected;
    private boolean mIsFailed;

    private InputFilter inputFilter = new InputFilter() {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            // 删除等特殊字符，直接返回
            if (TextUtils.isEmpty(source)) {
                return null;
            }
            String dValue = dest.toString();
            String[] splitArray = dValue.split("\\.");
            if (splitArray.length > 1) {
                String dotValue = splitArray[1];
                int dotIndex = dValue.indexOf(".");
                if (dend <= dotIndex) {
                    return null;
                } else {
                    // 2 表示输入框的小数位数
                    int diff = dotValue.length() + 1 - 2;
                    if (diff > 0) {
                        return source.subSequence(start, end - diff);
                    }
                }
            }
            return null;
        }
    };

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
        rgTemp.setOnCheckedChangeListener(this);
        if (mTempSelected == 0) {
            rbTempDisable.setChecked(true);
        } else {
            rbTempEnable.setChecked(true);
        }
        mHumiSelected = MokoSupport.getInstance().humiEnable;
        llHumiAlarm.setVisibility(mHumiSelected == 0 ? View.GONE : View.VISIBLE);
        etHumiLow.setText(MokoSupport.getInstance().humiLow);
        etHumiLow.setSelection(MokoSupport.getInstance().humiLow.length());
        etHumiHigh.setText(MokoSupport.getInstance().humiHigh);
        etHumiHigh.setSelection(MokoSupport.getInstance().humiHigh.length());
        tvHumiCurrent.setText(MokoSupport.getInstance().humiCurrent);
        rgHumi.setOnCheckedChangeListener(this);
        if (mHumiSelected == 0) {
            rbHumiDisable.setChecked(true);
        } else {
            rbHumiEnable.setChecked(true);
        }
        etTempLow.setFilters(new InputFilter[]{inputFilter});
        etTempHigh.setFilters(new InputFilter[]{inputFilter});
        etHumiLow.setFilters(new InputFilter[]{inputFilter});
        etHumiHigh.setFilters(new InputFilter[]{inputFilter});
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

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 200)
    public void onOrderTaskResponseEvent(OrderTaskResponseEvent event) {
        EventBus.getDefault().cancelEventDelivery(event);
        final String action = event.getAction();
        runOnUiThread(() -> {
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
                OrderTaskResponse response = event.getResponse();
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

    public void resetData(View view) {
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setTitle("Reset All Parameters");
        dialog.setMessage("Please confirm whether to reset all parameters?");
        dialog.setOnAlertConfirmListener(new AlertMessageDialog.OnAlertConfirmListener() {
            @Override
            public void onClick() {
                showLoadingProgressDialog();
                MokoSupport.getInstance().sendOrder(OrderTaskAssembler.setResetOrderTask());
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
            if (tempLow.length() == 1 && ".".equals(tempLow)) {
                ToastUtils.showToast(this, "Temperature Threshold error");
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
            if (tempHigh.length() == 1 && ".".equals(tempHigh)) {
                ToastUtils.showToast(this, "Temperature Threshold error");
                return;
            }
            float tempHighInt = Float.parseFloat(tempHigh);
            if (tempHighInt < 0 || tempHighInt > 65) {
                ToastUtils.showToast(this, "High Temperature Threshold range 0~65");
                return;
            }
            if (tempLowInt >= tempHighInt) {
                ToastUtils.showToast(this, "Temperature Threshold error");
                return;
            }
            orderTasks.add(OrderTaskAssembler.setTempDataOrderTask(mTempSelected, Math.round(tempLowInt * 100), Math.round(tempHighInt * 100)));
        } else {
            orderTasks.add(OrderTaskAssembler.setTempDataOrderTask(mTempSelected, 0, 0));
        }
        if (mHumiSelected == 1) {
            String humiLow = etHumiLow.getText().toString();
            if (TextUtils.isEmpty(humiLow)) {
                ToastUtils.showToast(this, "Low Humidity Threshold is empty");
                return;
            }
            if (humiLow.length() == 1 && ".".equals(humiLow)) {
                ToastUtils.showToast(this, "Humidity Threshold error");
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
            if (humiHigh.length() == 1 && ".".equals(humiHigh)) {
                ToastUtils.showToast(this, "Humidity Threshold error");
                return;
            }
            float humiHighInt = Float.parseFloat(humiHigh);
            if (humiHighInt < 10 || humiHighInt > 90) {
                ToastUtils.showToast(this, "High Humidity Threshold range 0~90");
                return;
            }
            if (humiLowInt >= humiHighInt) {
                ToastUtils.showToast(this, "Humidity Threshold error");
                return;
            }
            orderTasks.add(OrderTaskAssembler.setHumiDataOrderTask(mHumiSelected,  Math.round(humiLowInt * 100), Math.round(humiHighInt * 100)));
        } else {
            orderTasks.add(OrderTaskAssembler.setHumiDataOrderTask(mHumiSelected, 0, 0));
        }
        mIsFailed = false;
        int intervalInt = Integer.parseInt(i2cInterval);
        orderTasks.add(OrderTaskAssembler.setI2CIntervalOrderTask(intervalInt));
        MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
        showLoadingProgressDialog();
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.rb_temp_disable:
                mTempSelected = 0;
                llTempAlarm.setVisibility(View.GONE);
                break;
            case R.id.rb_temp_enable:
                mTempSelected = 1;
                llTempAlarm.setVisibility(View.VISIBLE);
                break;
            case R.id.rb_humi_disable:
                mHumiSelected = 0;
                llHumiAlarm.setVisibility(View.GONE);
                break;
            case R.id.rb_humi_enable:
                mHumiSelected = 1;
                llHumiAlarm.setVisibility(View.VISIBLE);
                break;
        }
    }
}
