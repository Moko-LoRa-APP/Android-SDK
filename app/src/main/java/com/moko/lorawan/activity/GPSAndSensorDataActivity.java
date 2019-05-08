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

import butterknife.Bind;
import butterknife.ButterKnife;

public class GPSAndSensorDataActivity extends BaseActivity {

    @Bind(R.id.tv_longitude)
    TextView tvLongitude;
    @Bind(R.id.tv_latitude)
    TextView tvLatitude;
    @Bind(R.id.tv_speed)
    TextView tvSpeed;
    @Bind(R.id.tv_altitude)
    TextView tvAltitude;
    @Bind(R.id.tv_gx)
    TextView tvGx;
    @Bind(R.id.tv_gy)
    TextView tvGy;
    @Bind(R.id.tv_gz)
    TextView tvGz;
    @Bind(R.id.tv_ax)
    TextView tvAx;
    @Bind(R.id.tv_ay)
    TextView tvAy;
    @Bind(R.id.tv_az)
    TextView tvAz;
    @Bind(R.id.tv_mx)
    TextView tvMx;
    @Bind(R.id.tv_my)
    TextView tvMy;
    @Bind(R.id.tv_mz)
    TextView tvMz;
    @Bind(R.id.tv_x_angle)
    TextView tvXAngle;
    @Bind(R.id.tv_y_angle)
    TextView tvYAngle;

    private boolean mReceiverTag = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps_sensor_data);
        ButterKnife.bind(this);
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


    public void back(View view) {
        finish();
    }
}
