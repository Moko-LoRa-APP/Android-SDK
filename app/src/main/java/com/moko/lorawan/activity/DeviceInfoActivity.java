package com.moko.lorawan.activity;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.TextView;

import com.moko.lorawan.R;
import com.moko.support.MokoSupport;

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

    private boolean mReceiverTag = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_info);
        ButterKnife.bind(this);
        tvCompanyName.setText(MokoSupport.getInstance().getCompanyName());
        tvManufactureDate.setText(MokoSupport.getInstance().getManufacureDate());
        tvModelName.setText(MokoSupport.getInstance().getModelName());
        tvBleFirmware.setText(MokoSupport.getInstance().getBleFirmware());
        tvLoraFirmware.setText(MokoSupport.getInstance().getLoraFirmware());
        // 注册广播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.setPriority(300);
        registerReceiver(mReceiver, filter);
        mReceiverTag = true;
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
    }
}
