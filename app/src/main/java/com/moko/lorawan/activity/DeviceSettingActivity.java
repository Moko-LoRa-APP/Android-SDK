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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.moko.lorawan.R;
import com.moko.lorawan.dialog.AlertMessageDialog;
import com.moko.lorawan.dialog.LoadingDialog;
import com.moko.lorawan.dialog.RegionBottomDialog;
import com.moko.lorawan.entity.Region;
import com.moko.lorawan.service.MokoService;
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

public class DeviceSettingActivity extends BaseActivity implements RadioGroup.OnCheckedChangeListener {

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
//    @Bind(R.id.tv_ch_1)
//    TextView tvCh1;
//    @Bind(R.id.tv_ch_2)
//    TextView tvCh2;
//    @Bind(R.id.tv_dr_1)
//    TextView tvDr1;
//    @Bind(R.id.tv_dr_2)
//    TextView tvDr2;
//    @Bind(R.id.tv_save)
//    TextView tvSave;
//    @Bind(R.id.tv_connect)
//    TextView tvConnect;
//    @Bind(R.id.cb_adr)
//    CheckBox cbAdr;
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


    private MokoService mMokoService;
    private boolean mReceiverTag = false;
    private String[] mRegions;
    private int mSelectedRegion;
//    private int mSelectedCh1;
//    private int mSelectedCh2;
//    private int mSelectedDr1;
//    private int mSelectedDr2;
    private boolean mIsFailed;
//    private boolean mIsResetSuccess;
//    private boolean mReadCHDR;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_setting);
        ButterKnife.bind(this);
        bindService(new Intent(this, MokoService.class), mServiceConnection, BIND_AUTO_CREATE);
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
//        mSelectedCh1 = MokoSupport.getInstance().ch_1;
//        tvCh1.setText(mSelectedCh1 + "");
//        mSelectedCh2 = MokoSupport.getInstance().ch_2;
//        tvCh2.setText(mSelectedCh2 + "");
//        mSelectedDr1 = MokoSupport.getInstance().dr_1;
//        tvDr1.setText("DR" + mSelectedDr1);
//        mSelectedDr2 = MokoSupport.getInstance().dr_2;
//        tvDr2.setText("DR" + mSelectedDr2);
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
//        int adr = MokoSupport.getInstance().adr;
//        if (adr == 0) {
//            cbAdr.setChecked(false);
//        } else {
//            cbAdr.setChecked(true);
//        }
        EventBus.getDefault().register(this);
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mMokoService = ((MokoService.LocalBinder) service).getService();
            // 注册广播接收器
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
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

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 300)
    public void onOrderTaskResponseEvent(OrderTaskResponseEvent event) {
        EventBus.getDefault().cancelEventDelivery(event);
        final String action = event.getAction();
        runOnUiThread(() -> {
            if (MokoConstants.ACTION_ORDER_TIMEOUT.equals(action)) {

            }
            if (MokoConstants.ACTION_ORDER_FINISH.equals(action)) {
//                    if (mReadCHDR) {
//                        return;
//                    }
                dismissLoadingProgressDialog();
                if (!mIsFailed) {
                    ToastUtils.showToast(DeviceSettingActivity.this, "Success");
                } else {
                    ToastUtils.showToast(DeviceSettingActivity.this, "Error");
                }
//                    if (mIsResetSuccess) {
//                        DeviceSettingActivity.this.setResult(RESULT_OK);
//                        DeviceSettingActivity.this.finish();
//                    }
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
//                        case WRITE_RESET:
//                            if ((value[3] & 0xff) != 0xAA) {
//                                mIsFailed = true;
//                            } else {
//                                mIsResetSuccess = true;
//                            }
//                            break;
//                        case READ_CH:
//                        case READ_DR:
//                            mReadCHDR = false;
//                            mSelectedCh1 = MokoSupport.getInstance().ch_1;
//                            tvCh1.setText(mSelectedCh1 + "");
//                            mSelectedCh2 = MokoSupport.getInstance().ch_2;
//                            tvCh2.setText(mSelectedCh2 + "");
//                            mSelectedDr1 = MokoSupport.getInstance().dr_1;
//                            tvDr1.setText("DR" + mSelectedDr1);
//                            mSelectedDr2 = MokoSupport.getInstance().dr_2;
//                            tvDr2.setText("DR" + mSelectedDr2);
//                            break;
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

    public void selectRegion(View view) {
        ArrayList<Region> regions = new ArrayList<>();
        for (int i = 0; i < mRegions.length; i++) {
            String name = mRegions[i];
            if ("US915HYBRID".equals(name) || "AU915OLD".equals(name)
                    || "CN470PREQUEL".equals(name) || "STE920".equals(name)) {
                continue;
            }
            Region region = new Region();
            region.value = i;
            region.name = name;
            regions.add(region);
        }
        RegionBottomDialog bottomDialog = new RegionBottomDialog();
        bottomDialog.setDatas(regions, mSelectedRegion);
        bottomDialog.setListener(new RegionBottomDialog.OnBottomListener() {
            @Override
            public void onValueSelected(int value) {
//                mReadCHDR = true;
                mSelectedRegion = value;
                tvRegion.setText(mRegions[mSelectedRegion]);
//                showLoadingProgressDialog();
//                MokoSupport.getInstance().sendOrder(mMokoService.getRegionOrderTask(mSelectedRegion));
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

//    private static ArrayList<String> mCHList;
//    private static ArrayList<String> mDRList;

//    static {
//        mCHList = new ArrayList<>();
//        for (int i = 0; i <= 95; i++) {
//            mCHList.add(i + "");
//        }
//        mDRList = new ArrayList<>();
//        for (int i = 0; i <= 15; i++) {
//            mDRList.add("DR" + i);
//        }
//    }


//    public void selectCh1(View view) {
//        BottomDialog bottomDialog = new BottomDialog();
//        bottomDialog.setDatas(mCHList, mSelectedCh1);
//        bottomDialog.setListener(new BottomDialog.OnBottomListener() {
//            @Override
//            public void onValueSelected(int value) {
//                mSelectedCh1 = value;
//                tvCh1.setText(mCHList.get(value));
//                if (mSelectedCh1 > mSelectedCh2) {
//                    mSelectedCh2 = mSelectedCh1;
//                    tvCh2.setText(mCHList.get(value));
//                }
//            }
//        });
//        bottomDialog.show(getSupportFragmentManager());
//    }
//
//    public void selectCh2(View view) {
//        final ArrayList<String> ch2List = new ArrayList<>();
//        for (int i = mSelectedCh1; i <= 95; i++) {
//            ch2List.add(i + "");
//        }
//        BottomDialog bottomDialog = new BottomDialog();
//        bottomDialog.setDatas(ch2List, mSelectedCh2 - mSelectedCh1);
//        bottomDialog.setListener(new BottomDialog.OnBottomListener() {
//            @Override
//            public void onValueSelected(int value) {
//                mSelectedCh2 = value + mSelectedCh1;
//                tvCh2.setText(ch2List.get(value));
//            }
//        });
//        bottomDialog.show(getSupportFragmentManager());
//    }
//
//    public void selectDr1(View view) {
//        BottomDialog bottomDialog = new BottomDialog();
//        bottomDialog.setDatas(mDRList, mSelectedDr1);
//        bottomDialog.setListener(new BottomDialog.OnBottomListener() {
//            @Override
//            public void onValueSelected(int value) {
//                mSelectedDr1 = value;
//                tvDr1.setText(mDRList.get(value));
//                if (mSelectedDr1 > mSelectedDr2) {
//                    mSelectedDr2 = mSelectedDr1;
//                    tvDr2.setText(mDRList.get(value));
//                }
//            }
//        });
//        bottomDialog.show(getSupportFragmentManager());
//    }
//
//    public void selectDr2(View view) {
//        final ArrayList<String> dr2List = new ArrayList<>();
//        for (int i = mSelectedDr1; i <= 15; i++) {
//            dr2List.add("DR" + i);
//        }
//        BottomDialog bottomDialog = new BottomDialog();
//        bottomDialog.setDatas(dr2List, mSelectedDr2 - mSelectedDr1);
//        bottomDialog.setListener(new BottomDialog.OnBottomListener() {
//            @Override
//            public void onValueSelected(int value) {
//                mSelectedDr2 = value + mSelectedDr1;
//                tvDr2.setText(dr2List.get(value));
//            }
//        });
//        bottomDialog.show(getSupportFragmentManager());
//    }

//    public void onSave(View view) {
//        ArrayList<OrderTask> orderTasks = new ArrayList<>();
//        if (rbModemAbp.isChecked()) {
//            String devAddr = etDevAddr.getText().toString();
//            String nwkSkey = etNwkSkey.getText().toString();
//            String appSkey = etAppSkey.getText().toString();
//            if (devAddr.length() != 8) {
//                ToastUtils.showToast(this, "data length error");
//                return;
//            }
//            if (nwkSkey.length() != 32) {
//                ToastUtils.showToast(this, "data length error");
//                return;
//            }
//            if (appSkey.length() != 32) {
//                ToastUtils.showToast(this, "data length error");
//                return;
//            }
//            orderTasks.add(mMokoService.getDevAddrOrderTask(devAddr));
//            orderTasks.add(mMokoService.getNwkSKeyOrderTask(nwkSkey));
//            orderTasks.add(mMokoService.getAppSKeyOrderTask(appSkey));
//            orderTasks.add(mMokoService.getUploadModeOrderTask(1));
//        } else {
//            String devEui = etDevEui.getText().toString();
//            String appEui = etAppEui.getText().toString();
//            String appKey = etAppKey.getText().toString();
//            if (devEui.length() != 16) {
//                ToastUtils.showToast(this, "data length error");
//                return;
//            }
//            if (appEui.length() != 16) {
//                ToastUtils.showToast(this, "data length error");
//                return;
//            }
//            if (appKey.length() != 32) {
//                ToastUtils.showToast(this, "data length error");
//                return;
//            }
//            orderTasks.add(mMokoService.getDevEUIOrderTask(devEui));
//            orderTasks.add(mMokoService.getAppEUIOrderTask(appEui));
//            orderTasks.add(mMokoService.getAppKeyOrderTask(appKey));
//            orderTasks.add(mMokoService.getUploadModeOrderTask(2));
//        }
//        if (MokoSupport.deviceTypeEnum != DeviceTypeEnum.LW002_TH) {
//            String reportInterval = etReportInterval.getText().toString();
//            if (TextUtils.isEmpty(reportInterval)) {
//                ToastUtils.showToast(this, "Reporting Interval is empty");
//                return;
//            }
//            int intervalInt = Integer.parseInt(reportInterval);
//            if (intervalInt < 1 || intervalInt > 14400) {
//                ToastUtils.showToast(this, "Reporting Interval range 1~14400");
//                return;
//            }
//            orderTasks.add(mMokoService.getUploadIntervalOrderTask(intervalInt));
//        }
//        orderTasks.add(mMokoService.getMsgTypeOrderTask(rbTypeUnconfirmed.isChecked() ? 0 : 1));
//        mIsFailed = false;
//        // 保存
//        orderTasks.add(mMokoService.getRegionOrderTask(mSelectedRegion));
//        if (MokoSupport.deviceTypeEnum != DeviceTypeEnum.LW004_BP) {
//            orderTasks.add(mMokoService.getClassTypeOrderTask(rbTypeClassa.isChecked() ? 1 : 3));
//        }
////        orderTasks.add(mMokoService.getCHOrderTask(mSelectedCh1, mSelectedCh2));
////        orderTasks.add(mMokoService.getDROrderTask(mSelectedDr1, mSelectedDr2));
////        orderTasks.add(mMokoService.getADROrderTask(cbAdr.isChecked() ? 1 : 0));
//        MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
//        showLoadingProgressDialog();
//    }

    public void onConnect(View view) {
        ArrayList<OrderTask> orderTasks = new ArrayList<>();
        if (rbModemAbp.isChecked()) {
            String devAddr = etDevAddr.getText().toString();
            String nwkSkey = etNwkSkey.getText().toString();
            String appSkey = etAppSkey.getText().toString();
            if (devAddr.length() != 8) {
                ToastUtils.showToast(this, "data length error");
                return;
            }
            if (nwkSkey.length() != 32) {
                ToastUtils.showToast(this, "data length error");
                return;
            }
            if (appSkey.length() != 32) {
                ToastUtils.showToast(this, "data length error");
                return;
            }
            orderTasks.add(mMokoService.getDevAddrOrderTask(devAddr));
            orderTasks.add(mMokoService.getNwkSKeyOrderTask(nwkSkey));
            orderTasks.add(mMokoService.getAppSKeyOrderTask(appSkey));
            orderTasks.add(mMokoService.getUploadModeOrderTask(1));
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
            orderTasks.add(mMokoService.getDevEUIOrderTask(devEui));
            orderTasks.add(mMokoService.getAppEUIOrderTask(appEui));
            orderTasks.add(mMokoService.getAppKeyOrderTask(appKey));
            orderTasks.add(mMokoService.getUploadModeOrderTask(2));
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
                int max = 14400 + searchTime;
                if (intervalInt < min || intervalInt > max) {
                    ToastUtils.showToast(this, "Error!No-alarm reporting interval must greater than the GPS satellite search time.");
                    return;
                }
                orderTasks.add(mMokoService.getUploadIntervalOrderTask(intervalInt - searchTime));
            } else {
                int intervalInt = Integer.parseInt(reportInterval);
                if (intervalInt < 1 || intervalInt > 14400) {
                    ToastUtils.showToast(this, "Reporting Interval range 1~14400");
                    return;
                }
                orderTasks.add(mMokoService.getUploadIntervalOrderTask(intervalInt));
            }
        }
        orderTasks.add(mMokoService.getMsgTypeOrderTask(rbTypeUnconfirmed.isChecked() ? 0 : 1));
        mIsFailed = false;
        LogModule.clearInfoForFile();
        // 保存并连接
        orderTasks.add(mMokoService.getRegionOrderTask(mSelectedRegion));
        if (MokoSupport.deviceTypeEnum != DeviceTypeEnum.LW004_BP) {
            orderTasks.add(mMokoService.getClassTypeOrderTask(rbTypeClassa.isChecked() ? 1 : 3));
        }
//        orderTasks.add(mMokoService.getCHOrderTask(mSelectedCh1, mSelectedCh2));
//        orderTasks.add(mMokoService.getDROrderTask(mSelectedDr1, mSelectedDr2));
//        orderTasks.add(mMokoService.getADROrderTask(cbAdr.isChecked() ? 1 : 0));
        orderTasks.add(mMokoService.getConnectOrderTask());
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
