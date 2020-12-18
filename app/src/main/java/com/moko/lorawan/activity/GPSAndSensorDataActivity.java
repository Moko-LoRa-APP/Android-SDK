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
import com.moko.support.MokoConstants;
import com.moko.support.MokoSupport;
import com.moko.support.event.ConnectStatusEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;

public class GPSAndSensorDataActivity extends BaseActivity {

    @BindView(R.id.tv_longitude)
    TextView tvLongitude;
    @BindView(R.id.tv_latitude)
    TextView tvLatitude;
    @BindView(R.id.tv_speed)
    TextView tvSpeed;
    @BindView(R.id.tv_altitude)
    TextView tvAltitude;
    @BindView(R.id.tv_gx)
    TextView tvGx;
    @BindView(R.id.tv_gy)
    TextView tvGy;
    @BindView(R.id.tv_gz)
    TextView tvGz;
    @BindView(R.id.tv_ax)
    TextView tvAx;
    @BindView(R.id.tv_ay)
    TextView tvAy;
    @BindView(R.id.tv_az)
    TextView tvAz;
    @BindView(R.id.tv_mx)
    TextView tvMx;
    @BindView(R.id.tv_my)
    TextView tvMy;
    @BindView(R.id.tv_mz)
    TextView tvMz;
    @BindView(R.id.tv_x_angle)
    TextView tvXAngle;
    @BindView(R.id.tv_y_angle)
    TextView tvYAngle;

    private boolean mReceiverTag = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps_sensor_data);
        ButterKnife.bind(this);

        String longitude = MokoSupport.getInstance().getLongitude();
        String latitude = MokoSupport.getInstance().getLatitude();
        String speed = MokoSupport.getInstance().getSpeed();
        String altitude = MokoSupport.getInstance().getAltitude();
        String gx = MokoSupport.getInstance().gx;
        String gy = MokoSupport.getInstance().gy;
        String gz = MokoSupport.getInstance().gz;
        String ax = MokoSupport.getInstance().ax;
        String ay = MokoSupport.getInstance().ay;
        String az = MokoSupport.getInstance().az;
        String mx = MokoSupport.getInstance().mx;
        String my = MokoSupport.getInstance().my;
        String mz = MokoSupport.getInstance().mz;
        String x_angle = MokoSupport.getInstance().x_angle;
        String y_angle = MokoSupport.getInstance().y_angle;

        tvLongitude.setText(longitude);
        tvLatitude.setText(latitude);
        tvSpeed.setText(speed);
        tvAltitude.setText(altitude);
        tvGx.setText(gx);
        tvGy.setText(gy);
        tvGz.setText(gz);
        tvAx.setText(ax);
        tvAy.setText(ay);
        tvAz.setText(az);
        tvMx.setText(mx);
        tvMy.setText(my);
        tvMz.setText(mz);
        tvXAngle.setText(x_angle);
        tvYAngle.setText(y_angle);

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
