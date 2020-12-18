package com.moko.lorawan.activity;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.moko.lorawan.R;
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

public class MulticastSettingActivity extends BaseActivity {


    @BindView(R.id.cb_multicast_switch)
    CheckBox cbMulticastSwitch;
    @BindView(R.id.et_multicast_addr)
    EditText etMulticastAddr;
    @BindView(R.id.et_multicast_nwkskey)
    EditText etMulticastNwkskey;
    @BindView(R.id.et_multicast_appskey)
    EditText etMulticastAppskey;
    @BindView(R.id.ll_multicast_info)
    LinearLayout llMulticastInfo;

    private boolean mReceiverTag = false;
    private boolean mIsFailed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multicast_setting);
        ButterKnife.bind(this);
        llMulticastInfo.setVisibility(MokoSupport.getInstance().multicastSwitch == 0 ? View.GONE : View.VISIBLE);
        cbMulticastSwitch.setChecked(MokoSupport.getInstance().multicastSwitch != 0);
        cbMulticastSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                llMulticastInfo.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });
        etMulticastAddr.setText(MokoSupport.getInstance().multicastAddr);
        etMulticastNwkskey.setText(MokoSupport.getInstance().multicastNwkSKey);
        etMulticastAppskey.setText(MokoSupport.getInstance().multicastAppSKey);
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
                    ToastUtils.showToast(MulticastSettingActivity.this, "Success");
                } else {
                    ToastUtils.showToast(MulticastSettingActivity.this, "Error");
                }
            }
            if (MokoConstants.ACTION_ORDER_RESULT.equals(action)) {
                OrderTaskResponse response = event.getResponse();
                OrderEnum orderEnum = response.order;
                byte[] value = response.responseValue;
                switch (orderEnum) {
                    case WRITE_MULTICAST_SWITCH:
                    case WRITE_MULTICAST_ADDRESS:
                    case WRITE_MULTICAST_NWKSKEY:
                    case WRITE_MULTICAST_APPSKEY:
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
        if (cbMulticastSwitch.isChecked()) {
            String addr = etMulticastAddr.getText().toString();
            String nwkskey = etMulticastNwkskey.getText().toString();
            String appskey = etMulticastAppskey.getText().toString();
            if (addr.length() != 8) {
                ToastUtils.showToast(this, "data length error");
                return;
            }
            if (nwkskey.length() != 32) {
                ToastUtils.showToast(this, "data length error");
                return;
            }
            if (appskey.length() != 32) {
                ToastUtils.showToast(this, "data length error");
                return;
            }

            orderTasks.add(OrderTaskAssembler.setMulticastAddrOrderTask(addr));
            orderTasks.add(OrderTaskAssembler.setMulticastNwkSKeyOrderTask(nwkskey));
            orderTasks.add(OrderTaskAssembler.setMulticastAppSKeyOrderTask(appskey));
            orderTasks.add(OrderTaskAssembler.setMulticastSwitchOrderTask(1));
            MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
        } else {
            orderTasks.add(OrderTaskAssembler.setMulticastSwitchOrderTask(0));
            MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
        }
        showLoadingProgressDialog();
    }
}
