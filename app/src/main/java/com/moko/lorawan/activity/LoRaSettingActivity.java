package com.moko.lorawan.activity;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.moko.lorawan.R;
import com.moko.lorawan.dialog.BottomDialog;
import com.moko.lorawan.dialog.LoadingDialog;
import com.moko.lorawan.dialog.RegionBottomDialog;
import com.moko.lorawan.entity.Region;
import com.moko.lorawan.utils.OrderTaskAssembler;
import com.moko.lorawan.utils.ToastUtils;
import com.moko.support.MokoConstants;
import com.moko.support.MokoSupport;
import com.moko.support.entity.DeviceTypeEnum;
import com.moko.support.entity.OrderEnum;
import com.moko.support.event.ConnectStatusEvent;
import com.moko.support.event.OrderTaskResponseEvent;
import com.moko.support.log.LogModule;
import com.moko.support.task.OrderTask;
import com.moko.support.task.OrderTaskResponse;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;

public class LoRaSettingActivity extends BaseActivity implements RadioGroup.OnCheckedChangeListener {

    @Bind(R.id.rb_modem_abp)
    RadioButton rbModemAbp;
    @Bind(R.id.rb_modem_otaa)
    RadioButton rbModemOtaa;
    @Bind(R.id.rg_modem)
    RadioGroup rgModem;
    @Bind(R.id.et_dev_eui)
    EditText etDevEui;
    @Bind(R.id.et_app_eui)
    EditText etAppEui;
    @Bind(R.id.et_app_key)
    EditText etAppKey;
    @Bind(R.id.ll_modem_otaa)
    LinearLayout llModemOtaa;
    @Bind(R.id.et_dev_addr)
    EditText etDevAddr;
    @Bind(R.id.et_nwk_skey)
    EditText etNwkSkey;
    @Bind(R.id.et_app_skey)
    EditText etAppSkey;
    @Bind(R.id.ll_modem_abp)
    LinearLayout llModemAbp;
    @Bind(R.id.rb_type_classa)
    RadioButton rbTypeClassa;
    @Bind(R.id.rb_type_classc)
    RadioButton rbTypeClassc;
    @Bind(R.id.rg_device_type)
    RadioGroup rgDeviceType;
    @Bind(R.id.et_report_interval)
    EditText etReportInterval;
    @Bind(R.id.tv_ch_1)
    TextView tvCh1;
    @Bind(R.id.tv_ch_2)
    TextView tvCh2;
    @Bind(R.id.tv_dr_1)
    TextView tvDr1;
    @Bind(R.id.cb_adr)
    CheckBox cbAdr;
    @Bind(R.id.tv_region)
    TextView tvRegion;
    @Bind(R.id.ll_report_invterval)
    LinearLayout llReportInvterval;
    @Bind(R.id.rb_type_unconfirmed)
    RadioButton rbTypeUnconfirmed;
    @Bind(R.id.rb_type_confirmed)
    RadioButton rbTypeConfirmed;
    @Bind(R.id.rg_msg_type)
    RadioGroup rgMsgType;
    @Bind(R.id.ll_msg_type)
    LinearLayout llMsgType;
    @Bind(R.id.ll_device_type)
    LinearLayout llDeviceType;
    @Bind(R.id.tv_report_interval_tips)
    TextView tvReportIntervalTips;
    @Bind(R.id.cb_advance_setting)
    CheckBox cbAdvanceSetting;
    @Bind(R.id.ll_advanced_setting)
    LinearLayout llAdvancedSetting;


    private boolean mReceiverTag = false;
    private ArrayList<Region> mRegionsList;
    private String[] mRegions;
    private int mSelectedRegion;
    private int mSelectedCh1;
    private int mSelectedCh2;
    private int mSelectedDr1;
    private int mMaxCH;
    private int mMaxDR;
    private boolean mIsFailed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lora_setting);
        ButterKnife.bind(this);
        rgModem.setOnCheckedChangeListener(this);
        int uploadMode = MokoSupport.getInstance().getUploadMode();
        if (uploadMode == 1) {
            rbModemAbp.setChecked(true);
        } else {
            rbModemOtaa.setChecked(true);
        }
        String devEUI = MokoSupport.getInstance().devEUI;
        etDevEui.setText(devEUI);
        String appEUI = MokoSupport.getInstance().appEUI;
        etAppEui.setText(appEUI);
        String appKey = MokoSupport.getInstance().appKey;
        etAppKey.setText(appKey);
        String devAddr = MokoSupport.getInstance().devAddr;
        etDevAddr.setText(devAddr);
        String nwkSKey = MokoSupport.getInstance().nwkSKey;
        etNwkSkey.setText(nwkSKey);
        String appSKey = MokoSupport.getInstance().appSKey;
        etAppSkey.setText(appSKey);
        mRegions = getResources().getStringArray(R.array.region);
        mRegionsList = new ArrayList<>();
        for (int i = 0; i < mRegions.length; i++) {
            String name = mRegions[i];
            if ("US915HYBRID".equals(name) || "AU915OLD".equals(name)
                    || "CN470PREQUEL".equals(name) || "STE920".equals(name)) {
                continue;
            }
            Region region = new Region();
            region.value = i;
            region.name = name;
            mRegionsList.add(region);
        }
        mSelectedRegion = MokoSupport.getInstance().getRegion();
        tvRegion.setText(mRegions[mSelectedRegion]);
        if (MokoSupport.deviceTypeEnum == DeviceTypeEnum.LW004_BP) {
            llDeviceType.setVisibility(View.GONE);
            tvReportIntervalTips.setVisibility(View.VISIBLE);
        } else {
            int classType = MokoSupport.getInstance().getClassType();
            if (classType == 1) {
                rbTypeClassa.setChecked(true);
            } else {
                rbTypeClassc.setChecked(true);
            }
        }
        mSelectedCh1 = MokoSupport.getInstance().ch_1;
        tvCh1.setText(mSelectedCh1 + "");
        mSelectedCh2 = MokoSupport.getInstance().ch_2;
        tvCh2.setText(mSelectedCh2 + "");
        mSelectedDr1 = MokoSupport.getInstance().dr_1;
        tvDr1.setText("DR" + mSelectedDr1);
        if (MokoSupport.deviceTypeEnum == DeviceTypeEnum.LW002_TH) {
            llReportInvterval.setVisibility(View.GONE);
        } else {
            llReportInvterval.setVisibility(View.VISIBLE);
            if (MokoSupport.deviceTypeEnum == DeviceTypeEnum.LW004_BP) {
                long uploadInterval = MokoSupport.getInstance().uploadInterval;
                int searchTime = MokoSupport.getInstance().alarmSatelliteSearchTime;
                etReportInterval.setText(String.valueOf(uploadInterval + searchTime));
            } else {
                long uploadInterval = MokoSupport.getInstance().uploadInterval;
                etReportInterval.setText(uploadInterval + "");
            }
        }
        int msgType = MokoSupport.getInstance().msgType;
        if (msgType == 1) {
            rbTypeConfirmed.setChecked(true);
        } else {
            rbTypeUnconfirmed.setChecked(true);
        }
        int adr = MokoSupport.getInstance().adr;
        if (adr == 0) {
            cbAdr.setChecked(false);
        } else {
            cbAdr.setChecked(true);
        }
        cbAdvanceSetting.setOnCheckedChangeListener((buttonView, isChecked) -> {
            llAdvancedSetting.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });
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
                    ToastUtils.showToast(LoRaSettingActivity.this, "Success");
                } else {
                    ToastUtils.showToast(LoRaSettingActivity.this, "Error");
                }
            }
            if (MokoConstants.ACTION_ORDER_RESULT.equals(action)) {
                OrderTaskResponse response = event.getResponse();
                OrderEnum orderEnum = response.order;
                byte[] value = response.responseValue;
                switch (orderEnum) {
                    case WRITE_DEV_ADDR:
                    case WRITE_NWK_SKEY:
                    case WRITE_APP_SKEY:
                    case WRITE_DEV_EUI:
                    case WRITE_APP_EUI:
                    case WRITE_APP_KEY:
                    case WRITE_CLASS_TYPE:
                    case WRITE_MSG_TYPE:
                    case WRITE_UPLOAD_MODE:
                    case WRITE_UPLOAD_INTERVAL:
                    case WRITE_CH:
                    case WRITE_DR:
                    case WRITE_POWER:
                    case WRITE_ADR:
                    case WRITE_CONNECT:
                    case WRITE_REGION:
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

    public void selectRegion(View view) {
        RegionBottomDialog bottomDialog = new RegionBottomDialog();
        bottomDialog.setDatas(mRegionsList, mSelectedRegion);
        bottomDialog.setListener(value -> {
            if (mSelectedRegion != value) {
                mSelectedRegion = value;
                tvRegion.setText(mRegions[mSelectedRegion]);
                initCHDRRange();
                updateCHDR();
            }
        });
        bottomDialog.show(getSupportFragmentManager());
    }

    private void updateCHDR() {
        switch (mSelectedRegion) {
            case 0:
            case 4:
            case 9:
            case 10:
                mSelectedCh1 = 0;
                mSelectedCh2 = 2;
                mSelectedDr1 = 0;
                break;
            case 5:
                mSelectedCh1 = 0;
                mSelectedCh2 = 7;
                mSelectedDr1 = 2;
                break;
            case 3:
                mSelectedCh1 = 0;
                mSelectedCh2 = 5;
                mSelectedDr1 = 0;
                break;
            case 1:
            case 7:
                mSelectedCh1 = 0;
                mSelectedCh2 = 7;
                mSelectedDr1 = 0;
                break;
            case 8:
                mSelectedCh1 = 0;
                mSelectedCh2 = 1;
                mSelectedDr1 = 2;
                break;
        }

        tvCh1.setText(String.valueOf(mSelectedCh1));
        tvCh2.setText(String.valueOf(mSelectedCh2));
        tvDr1.setText(String.format("DR%d", mSelectedDr1));
    }

    private ArrayList<String> mCHList;
    private ArrayList<String> mDRList;

    private void initCHDRRange() {
        mCHList = new ArrayList<>();
        mDRList = new ArrayList<>();
        switch (mSelectedRegion) {
            case 0:
            case 3:
            case 4:
            case 8:
            case 9:
            case 10:
                // EU868、CN779、EU443、AS923、KR920、IN865
                mMaxCH = 15;
                mMaxDR = 5;
                break;
            case 1:
                // US915
                mMaxCH = 63;
                mMaxDR = 4;
                break;
            case 5:
                // AU915
                mMaxCH = 63;
                mMaxDR = 6;
                break;
            case 7:
                // CN470
                mMaxCH = 95;
                mMaxDR = 5;
                break;
        }
        for (int i = 0; i <= mMaxCH; i++) {
            mCHList.add(i + "");
        }
        for (int i = 0; i <= mMaxDR; i++) {
            mDRList.add("DR" + i);
        }
    }

    public void selectCh1(View view) {
        BottomDialog bottomDialog = new BottomDialog();
        bottomDialog.setDatas(mCHList, mSelectedCh1);
        bottomDialog.setListener(value -> {
            mSelectedCh1 = value;
            tvCh1.setText(mCHList.get(value));
            if (mSelectedCh1 > mSelectedCh2) {
                mSelectedCh2 = mSelectedCh1;
                tvCh2.setText(mCHList.get(value));
            }
        });
        bottomDialog.show(getSupportFragmentManager());
    }

    public void selectCh2(View view) {
        final ArrayList<String> ch2List = new ArrayList<>();
        for (int i = mSelectedCh1; i <= mMaxCH; i++) {
            ch2List.add(i + "");
        }
        BottomDialog bottomDialog = new BottomDialog();
        bottomDialog.setDatas(ch2List, mSelectedCh2 - mSelectedCh1);
        bottomDialog.setListener(value -> {
            mSelectedCh2 = value + mSelectedCh1;
            tvCh2.setText(ch2List.get(value));
        });
        bottomDialog.show(getSupportFragmentManager());
    }

    public void selectDr1(View view) {
        if (cbAdr.isChecked()) {
            return;
        }
        BottomDialog bottomDialog = new BottomDialog();
        bottomDialog.setDatas(mDRList, mSelectedDr1);
        bottomDialog.setListener(value -> {
            mSelectedDr1 = value;
            tvDr1.setText(mDRList.get(value));
        });
        bottomDialog.show(getSupportFragmentManager());
    }

    public void onConnect(View view) {
        ArrayList<OrderTask> orderTasks = new ArrayList<>();
        if (rbModemAbp.isChecked()) {
            String devEui = etDevEui.getText().toString();
            String appEui = etAppEui.getText().toString();
            String devAddr = etDevAddr.getText().toString();
            String appSkey = etAppSkey.getText().toString();
            String nwkSkey = etNwkSkey.getText().toString();
            if (devEui.length() != 16) {
                ToastUtils.showToast(this, "data length error");
                return;
            }
            if (appEui.length() != 16) {
                ToastUtils.showToast(this, "data length error");
                return;
            }
            if (devAddr.length() != 8) {
                ToastUtils.showToast(this, "data length error");
                return;
            }
            if (appSkey.length() != 32) {
                ToastUtils.showToast(this, "data length error");
                return;
            }
            if (nwkSkey.length() != 32) {
                ToastUtils.showToast(this, "data length error");
                return;
            }
            orderTasks.add(OrderTaskAssembler.setDevEUIOrderTask(devEui));
            orderTasks.add(OrderTaskAssembler.setAppEUIOrderTask(appEui));
            orderTasks.add(OrderTaskAssembler.setDevAddrOrderTask(devAddr));
            orderTasks.add(OrderTaskAssembler.setAppSKeyOrderTask(appSkey));
            orderTasks.add(OrderTaskAssembler.setNwkSKeyOrderTask(nwkSkey));
            orderTasks.add(OrderTaskAssembler.setUploadModeOrderTask(1));
        } else {
            String devEui = etDevEui.getText().toString();
            String appEui = etAppEui.getText().toString();
            String appKey = etAppKey.getText().toString();
            if (devEui.length() != 16) {
                ToastUtils.showToast(this, "data length error");
                return;
            }
            if (appEui.length() != 16) {
                ToastUtils.showToast(this, "data length error");
                return;
            }
            if (appKey.length() != 32) {
                ToastUtils.showToast(this, "data length error");
                return;
            }
            orderTasks.add(OrderTaskAssembler.setDevEUIOrderTask(devEui));
            orderTasks.add(OrderTaskAssembler.setAppEUIOrderTask(appEui));
            orderTasks.add(OrderTaskAssembler.setAppKeyOrderTask(appKey));
            orderTasks.add(OrderTaskAssembler.setUploadModeOrderTask(2));
        }
        if (MokoSupport.deviceTypeEnum != DeviceTypeEnum.LW002_TH) {
            String reportInterval = etReportInterval.getText().toString();
            if (TextUtils.isEmpty(reportInterval)) {
                ToastUtils.showToast(this, "Reporting Interval is empty");
                return;
            }
            if (MokoSupport.deviceTypeEnum == DeviceTypeEnum.LW004_BP) {
                int intervalInt = Integer.parseInt(reportInterval);
                int searchTime = MokoSupport.getInstance().alarmSatelliteSearchTime;
                int min = 1 + searchTime;
                if (intervalInt < min) {
                    ToastUtils.showToast(this, "Error!No-alarm reporting interval must greater than the GPS satellite search time.");
                    return;
                }
                if (intervalInt < 2 || intervalInt > 14400) {
                    ToastUtils.showToast(this, "Reporting Interval range 2~14400");
                    return;
                }
                orderTasks.add(OrderTaskAssembler.setUploadIntervalOrderTask(intervalInt - searchTime));
            } else {
                int intervalInt = Integer.parseInt(reportInterval);
                if (intervalInt < 1 || intervalInt > 14400) {
                    ToastUtils.showToast(this, "Reporting Interval range 1~14400");
                    return;
                }
                orderTasks.add(OrderTaskAssembler.setUploadIntervalOrderTask(intervalInt));
            }
        }
        orderTasks.add(OrderTaskAssembler.setMsgTypeOrderTask(rbTypeUnconfirmed.isChecked() ? 0 : 1));
        mIsFailed = false;
        LogModule.clearInfoForFile();
        // 保存并连接
        orderTasks.add(OrderTaskAssembler.setRegionOrderTask(mSelectedRegion));
        if (MokoSupport.deviceTypeEnum != DeviceTypeEnum.LW004_BP) {
            orderTasks.add(OrderTaskAssembler.setClassTypeOrderTask(rbTypeClassa.isChecked() ? 1 : 3));
        }
        orderTasks.add(OrderTaskAssembler.setCHOrderTask(mSelectedCh1, mSelectedCh2));
        orderTasks.add(OrderTaskAssembler.setDROrderTask(mSelectedDr1));
        orderTasks.add(OrderTaskAssembler.setADROrderTask(cbAdr.isChecked() ? 1 : 0));
        orderTasks.add(OrderTaskAssembler.setConnectOrderTask());
        MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
        showLoadingProgressDialog();
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.rb_modem_abp:
                llModemAbp.setVisibility(View.VISIBLE);
                llModemOtaa.setVisibility(View.GONE);
                break;
            case R.id.rb_modem_otaa:
                llModemAbp.setVisibility(View.GONE);
                llModemOtaa.setVisibility(View.VISIBLE);
                break;
        }
    }
}
