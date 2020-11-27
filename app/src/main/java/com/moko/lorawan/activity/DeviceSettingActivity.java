package com.moko.lorawan.activity;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.moko.lorawan.R;
import com.moko.lorawan.dialog.ChangePasswordDialog;
import com.moko.lorawan.dialog.LoadingDialog;
import com.moko.lorawan.dialog.LowPowerPromptDialog;
import com.moko.lorawan.utils.OrderTaskAssembler;
import com.moko.lorawan.utils.ToastUtils;
import com.moko.support.MokoConstants;
import com.moko.support.MokoSupport;
import com.moko.support.entity.OrderEnum;
import com.moko.support.event.ConnectStatusEvent;
import com.moko.support.event.OrderTaskResponseEvent;
import com.moko.support.task.OrderTaskResponse;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Timer;
import java.util.TimerTask;

import butterknife.Bind;
import butterknife.ButterKnife;

public class DeviceSettingActivity extends BaseActivity {


    @Bind(R.id.tv_low_power_prompt)
    TextView tvLowPowerPrompt;
    @Bind(R.id.tv_low_power_prompt_tips)
    TextView tvLowPowerPromptTips;
    private boolean mReceiverTag = false;

    private boolean mIsFailed;

    private int mSelected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_setting);
        ButterKnife.bind(this);

        int lowPowerPrompt = MokoSupport.getInstance().lowPowerPrompt;
        tvLowPowerPrompt.setText(String.format("%d%%", lowPowerPrompt));
        tvLowPowerPromptTips.setText(getString(R.string.low_power_prompt_tips, lowPowerPrompt));
        mSelected = lowPowerPrompt / 10 - 1;
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
                    ToastUtils.showToast(DeviceSettingActivity.this, "Success");
                } else {
                    ToastUtils.showToast(DeviceSettingActivity.this, "Error");
                }
            }
            if (MokoConstants.ACTION_ORDER_RESULT.equals(action)) {
                OrderTaskResponse response = event.getResponse();
                OrderEnum orderEnum = response.order;
                byte[] value = response.responseValue;
                switch (orderEnum) {
                    case WRITE_LOW_POWER_PROMPT:
                    case WRITE_PASSWORD:
                        if ((value[3] & 0xff) != 0xAA) {
                            mIsFailed = true;
                        }
                        break;
                }
            }
        });
    }


    public void selecLowPowerPrompt(View view) {
        LowPowerPromptDialog bottomDialog = new LowPowerPromptDialog();
        bottomDialog.setSelected(mSelected);
        bottomDialog.setListener(new LowPowerPromptDialog.OnBottomListener() {
            @Override
            public void onValueSelected(int value) {
                mSelected = value;
                int lowPowerPrompt = (mSelected + 1) * 10;
                tvLowPowerPrompt.setText(String.format("%d%%", lowPowerPrompt));
                tvLowPowerPromptTips.setText(getString(R.string.low_power_prompt_tips, lowPowerPrompt));
            }
        });
        bottomDialog.show(getSupportFragmentManager());
    }

    public void onSave(View view) {
        int lowPowerPrompt = (mSelected + 1) * 10;
        MokoSupport.getInstance().sendOrder(OrderTaskAssembler.setLowPowerPromptTask(lowPowerPrompt));
        showLoadingProgressDialog();
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

    public void changePassword(View view) {
        final ChangePasswordDialog dialog = new ChangePasswordDialog(this);
        dialog.setOnPasswordClicked(password -> {
            showLoadingProgressDialog();
            MokoSupport.getInstance().sendOrder(OrderTaskAssembler.changePassword(password));
        });
        dialog.show();
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override

            public void run() {
                runOnUiThread(() -> dialog.showKeyboard());
            }
        }, 200);
    }
}
