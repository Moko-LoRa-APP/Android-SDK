package com.moko.lorawan.activity;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.moko.lorawan.R;
import com.moko.support.MokoConstants;
import com.moko.support.MokoSupport;
import com.moko.support.event.ConnectStatusEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.Bind;
import butterknife.ButterKnife;

public class DeviceInfoActivity extends BaseActivity {

    @Bind(R.id.tv_company_name)
    TextView tvCompanyName;
    @Bind(R.id.tv_manufacture_date)
    TextView tvManufactureDate;
    @Bind(R.id.tv_model_name)
    TextView tvModelName;
    @Bind(R.id.tv_ble_firmware)
    TextView tvBleFirmware;
    @Bind(R.id.tv_lora_firmware)
    TextView tvLoraFirmware;
    @Bind(R.id.ll_manufacture_date)
    LinearLayout llManufactureDate;
    @Bind(R.id.tv_mcu_firmware)
    TextView tvMcuFirmware;
    @Bind(R.id.ll_mcu_firmware)
    LinearLayout llMcuFirmware;

    private boolean mReceiverTag = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_info);
        ButterKnife.bind(this);
        tvCompanyName.setText(MokoSupport.getInstance().getCompanyName());

        tvModelName.setText(MokoSupport.getInstance().getModelName());
        tvBleFirmware.setText(MokoSupport.getInstance().getBleFirmware());
        tvLoraFirmware.setText(MokoSupport.getInstance().getLoraFirmware());
        int deviceType = MokoSupport.getInstance().getDeviceType();
        llManufactureDate.setVisibility(deviceType == 1 ? View.GONE : View.VISIBLE);
        if (deviceType != 1) {
            tvManufactureDate.setText(MokoSupport.getInstance().getManufacureDate());
        }
        if (deviceType == 1) {
            tvMcuFirmware.setText(MokoSupport.getInstance().getMCUFirmware());
        }
        llMcuFirmware.setVisibility(deviceType == 1 ? View.VISIBLE : View.GONE);
        // 注册广播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.setPriority(300);
        registerReceiver(mReceiver, filter);
        mReceiverTag = true;
        EventBus.getDefault().register(this);
    }

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

    public void back(View view) {
        finish();
    }
}
