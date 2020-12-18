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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.moko.lorawan.R;
import com.moko.lorawan.dialog.AlertMessageDialog;
import com.moko.lorawan.dialog.LoadingDialog;
import com.moko.lorawan.utils.OrderTaskAssembler;
import com.moko.lorawan.utils.ToastUtils;
import com.moko.support.MokoConstants;
import com.moko.support.MokoSupport;
import com.moko.support.entity.DataTypeEnum;
import com.moko.support.entity.OrderEnum;
import com.moko.support.event.ConnectStatusEvent;
import com.moko.support.event.OrderTaskResponseEvent;
import com.moko.support.task.OrderTask;
import com.moko.support.task.OrderTaskResponse;
import com.moko.support.utils.MokoUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ScanSettingFilterActivity extends BaseActivity implements CompoundButton.OnCheckedChangeListener, SeekBar.OnSeekBarChangeListener {


    private final String FILTER_ASCII = "\\A\\p{ASCII}*\\z";
    @BindView(R.id.sb_rssi_filter)
    SeekBar sbRssiFilter;
    @BindView(R.id.tv_rssi_filter_value)
    TextView tvRssiFilterValue;
    @BindView(R.id.tv_rssi_filter_tips)
    TextView tvRssiFilterTips;
    @BindView(R.id.cb_mac_address)
    CheckBox cbMacAddress;
    @BindView(R.id.et_mac_address)
    EditText etMacAddress;
    @BindView(R.id.cb_adv_name)
    CheckBox cbAdvName;
    @BindView(R.id.et_adv_name)
    EditText etAdvName;
    @BindView(R.id.cb_ibeacon_uuid)
    CheckBox cbIbeaconUuid;
    @BindView(R.id.et_ibeacon_uuid)
    EditText etIbeaconUuid;
    @BindView(R.id.cb_ibeacon_major)
    CheckBox cbIbeaconMajor;
    @BindView(R.id.ll_ibeacon_major)
    LinearLayout llIbeaconMajor;
    @BindView(R.id.et_ibeacon_major_min)
    EditText etIbeaconMajorMin;
    @BindView(R.id.et_ibeacon_major_max)
    EditText etIbeaconMajorMax;
    @BindView(R.id.ll_ibeacon_minor)
    LinearLayout llIbeaconMinor;
    @BindView(R.id.cb_ibeacon_minor)
    CheckBox cbIbeaconMinor;
    @BindView(R.id.et_ibeacon_minor_min)
    EditText etIbeaconMinorMin;
    @BindView(R.id.et_ibeacon_minor_max)
    EditText etIbeaconMinorMax;
    @BindView(R.id.cb_raw_adv_data)
    CheckBox cbRawAdvData;
    @BindView(R.id.ll_raw_data_filter)
    LinearLayout llRawDataFilter;
    @BindView(R.id.iv_raw_data_del)
    ImageView ivRawDataDel;
    @BindView(R.id.iv_raw_data_add)
    ImageView ivRawDataAdd;
    @BindView(R.id.tv_save)
    TextView tvSave;

    private boolean mReceiverTag = false;
    private boolean mIsFailed;

    private ArrayList<String> filterRawDatas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);
        ButterKnife.bind(this);
        initView();

        EventBus.getDefault().register(this);

        // 注册广播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.setPriority(400);
        registerReceiver(mReceiver, filter);
        mReceiverTag = true;
    }

    private void initView() {
        InputFilter inputFilter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                if (!(source + "").matches(FILTER_ASCII)) {
                    return "";
                }

                return null;
            }
        };
        etAdvName.setFilters(new InputFilter[]{new InputFilter.LengthFilter(11), inputFilter});
        final String filterName = MokoSupport.getInstance().filterName;
        if (!TextUtils.isEmpty(filterName)) {
            cbAdvName.setChecked(true);
            etAdvName.setText(filterName);
            etAdvName.setVisibility(View.VISIBLE);
        }
        final String filterMac = MokoSupport.getInstance().filterMac;
        if (!TextUtils.isEmpty(filterMac)) {
            cbMacAddress.setChecked(true);
            etMacAddress.setText(filterMac);
            etMacAddress.setVisibility(View.VISIBLE);
        }
        final String filterUUID = MokoSupport.getInstance().filterUUID;
        if (!TextUtils.isEmpty(filterUUID)) {
            cbIbeaconUuid.setChecked(true);
            etIbeaconUuid.setText(filterUUID);
            etIbeaconUuid.setVisibility(View.VISIBLE);
        }
        final String filterMajorMin = MokoSupport.getInstance().filterMajorMin;
        final String filterMajorMax = MokoSupport.getInstance().filterMajorMax;
        if (!TextUtils.isEmpty(filterMajorMin)
                && !TextUtils.isEmpty(filterMajorMax)) {
            cbIbeaconMajor.setChecked(true);
            etIbeaconMajorMin.setText(filterMajorMin);
            etIbeaconMajorMax.setText(filterMajorMax);
            llIbeaconMajor.setVisibility(View.VISIBLE);
        }
        final String filterMinorMin = MokoSupport.getInstance().filterMinorMin;
        final String filterMinorMax = MokoSupport.getInstance().filterMinorMax;
        if (!TextUtils.isEmpty(filterMinorMin)
                && !TextUtils.isEmpty(filterMinorMax)) {
            cbIbeaconMinor.setChecked(true);
            etIbeaconMinorMin.setText(filterMinorMin);
            etIbeaconMinorMax.setText(filterMinorMax);
            llIbeaconMinor.setVisibility(View.VISIBLE);
        }

        sbRssiFilter.setOnSeekBarChangeListener(this);

        final int rssi = MokoSupport.getInstance().filterRssi * -1;
        int progress = rssi + 127;
        sbRssiFilter.setProgress(progress);
        tvRssiFilterValue.setText(String.format("%ddBm", rssi));
        tvRssiFilterTips.setText(getString(R.string.rssi_filter, rssi));

        final String rawData = MokoSupport.getInstance().filterRawData;
        if (!TextUtils.isEmpty(rawData)) {
            cbRawAdvData.setChecked(true);

            byte[] rawDataBytes = MokoUtils.hex2bytes(rawData);
            for (int i = 0, l = rawDataBytes.length; i < l; ) {
                View v = LayoutInflater.from(this).inflate(R.layout.item_raw_data_filter, llRawDataFilter, false);
                EditText etDataType = v.findViewById(R.id.et_data_type);
                EditText etMin = v.findViewById(R.id.et_min);
                EditText etMax = v.findViewById(R.id.et_max);
                EditText etRawData = v.findViewById(R.id.et_raw_data);
                int filterLength = rawDataBytes[i] & 0xFF;
                i++;
                String type = MokoUtils.byte2HexString(rawDataBytes[i]);
                i++;
                String min = String.valueOf((rawDataBytes[i] & 0xFF));
                i++;
                String max = String.valueOf((rawDataBytes[i] & 0xFF));
                i++;
                String data = MokoUtils.bytesToHexString(Arrays.copyOfRange(rawDataBytes, i, i + filterLength - 3));
                i += filterLength - 3;
                etDataType.setText(type);
                etMin.setText(min);
                etMax.setText(max);
                etRawData.setText(data);
                llRawDataFilter.addView(v);
            }
            llRawDataFilter.setVisibility(View.VISIBLE);
            ivRawDataAdd.setVisibility(View.VISIBLE);
            ivRawDataDel.setVisibility(View.VISIBLE);
        }

        cbAdvName.setOnCheckedChangeListener(this);
        cbMacAddress.setOnCheckedChangeListener(this);
        cbIbeaconUuid.setOnCheckedChangeListener(this);
        cbIbeaconMajor.setOnCheckedChangeListener(this);
        cbIbeaconMinor.setOnCheckedChangeListener(this);
        cbRawAdvData.setOnCheckedChangeListener(this);
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
                    ToastUtils.showToast(ScanSettingFilterActivity.this, "Success");
                } else {
                    ToastUtils.showToast(ScanSettingFilterActivity.this, "Error");
                }
            }
            if (MokoConstants.ACTION_ORDER_RESULT.equals(action)) {
                OrderTaskResponse response = event.getResponse();
                OrderEnum orderEnum = response.order;
                byte[] value = response.responseValue;
                switch (orderEnum) {
                    case WRITE_FILTER_NAME:
                    case WRITE_FILTER_RSSI:
                    case WRITE_FILTER_MAC:
                    case WRITE_FILTER_UUID:
                    case WRITE_FILTER_MAJOR:
                    case WRITE_FILTER_MINOR:
                    case WRITE_FILTER_RAW_DATA:
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
        if (isValid()) {
            showLoadingProgressDialog();
            saveParams();
        } else {
            ToastUtils.showToast(this, "Opps！Save failed. Please check the input characters and try again.");
        }
    }

    private void saveParams() {
        final int progress = sbRssiFilter.getProgress();
        int filterRssi = progress - 127;
        List<OrderTask> orderTasks = new ArrayList<>();
        final String mac = etMacAddress.getText().toString();
        final String name = etAdvName.getText().toString();
        final String uuid = etIbeaconUuid.getText().toString();
        final String majorMin = etIbeaconMajorMin.getText().toString();
        final String majorMax = etIbeaconMajorMax.getText().toString();
        final String minorMin = etIbeaconMinorMin.getText().toString();
        final String minorMax = etIbeaconMinorMax.getText().toString();

        orderTasks.add(OrderTaskAssembler.setFilterRssiOrderTask(filterRssi));
        orderTasks.add(OrderTaskAssembler.setFilterMacOrderTask(cbMacAddress.isChecked() ? mac : ""));
        orderTasks.add(OrderTaskAssembler.setFilterNameOrderTask(cbAdvName.isChecked() ? name : ""));
        orderTasks.add(OrderTaskAssembler.setFilterUUIDOrderTask(cbIbeaconUuid.isChecked() ? uuid : ""));
        orderTasks.add(OrderTaskAssembler.setFilterMajorRange(
                cbIbeaconMajor.isChecked() ? 1 : 0,
                cbIbeaconMajor.isChecked() ? Integer.parseInt(majorMin) : 0,
                cbIbeaconMajor.isChecked() ? Integer.parseInt(majorMax) : 0));
        orderTasks.add(OrderTaskAssembler.setFilterMinorRange(
                cbIbeaconMinor.isChecked() ? 1 : 0,
                cbIbeaconMinor.isChecked() ? Integer.parseInt(minorMin) : 0,
                cbIbeaconMinor.isChecked() ? Integer.parseInt(minorMax) : 0));
        orderTasks.add(OrderTaskAssembler.setFilterAdvRawData(cbRawAdvData.isChecked() ? filterRawDatas : null));

        MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }

    private boolean isValid() {
        final String mac = etMacAddress.getText().toString();
        final String name = etAdvName.getText().toString();
        final String uuid = etIbeaconUuid.getText().toString();
        final String majorMin = etIbeaconMajorMin.getText().toString();
        final String majorMax = etIbeaconMajorMax.getText().toString();
        final String minorMin = etIbeaconMinorMin.getText().toString();
        final String minorMax = etIbeaconMinorMax.getText().toString();
        if (cbMacAddress.isChecked()) {
            if (TextUtils.isEmpty(mac))
                return false;
            int length = mac.length();
            if (length % 2 != 0)
                return false;
        }
        if (cbAdvName.isChecked()) {
            if (TextUtils.isEmpty(name))
                return false;
        }
        if (cbIbeaconUuid.isChecked()) {
            if (TextUtils.isEmpty(uuid))
                return false;
            int length = uuid.length();
            if (length % 2 != 0)
                return false;
        }
        if (cbIbeaconMajor.isChecked()) {
            if (TextUtils.isEmpty(majorMin))
                return false;
            if (Integer.parseInt(majorMin) > 65535)
                return false;
            if (TextUtils.isEmpty(majorMax))
                return false;
            if (Integer.parseInt(majorMax) > 65535)
                return false;
            if (Integer.parseInt(majorMin) > Integer.parseInt(majorMax))
                return false;

        }
        if (cbIbeaconMinor.isChecked()) {
            if (TextUtils.isEmpty(minorMin))
                return false;
            if (Integer.parseInt(minorMin) > 65535)
                return false;
            if (TextUtils.isEmpty(minorMax))
                return false;
            if (Integer.parseInt(minorMax) > 65535)
                return false;
            if (Integer.parseInt(minorMin) > Integer.parseInt(minorMax))
                return false;
        }
        filterRawDatas = new ArrayList<>();
        if (cbRawAdvData.isChecked()) {
            // 发送设置的过滤RawData
            int count = llRawDataFilter.getChildCount();
            if (count == 0)
                return false;

            for (int i = 0; i < count; i++) {
                View v = llRawDataFilter.getChildAt(i);
                EditText etDataType = v.findViewById(R.id.et_data_type);
                EditText etMin = v.findViewById(R.id.et_min);
                EditText etMax = v.findViewById(R.id.et_max);
                EditText etRawData = v.findViewById(R.id.et_raw_data);
                final String dataTypeStr = etDataType.getText().toString();
                final String minStr = etMin.getText().toString();
                final String maxStr = etMax.getText().toString();
                final String rawDataStr = etRawData.getText().toString();

                if (TextUtils.isEmpty(dataTypeStr))
                    return false;

                final int dataType = Integer.parseInt(dataTypeStr, 16);
                final DataTypeEnum dataTypeEnum = DataTypeEnum.fromDataType(dataType);
                if (dataTypeEnum == null)
                    return false;
                if (TextUtils.isEmpty(rawDataStr))
                    return false;
                int length = rawDataStr.length();
                if (length % 2 != 0)
                    return false;
                int min = 0;
                if (!TextUtils.isEmpty(minStr))
                    min = Integer.parseInt(minStr);
                int max = 0;
                if (!TextUtils.isEmpty(maxStr))
                    max = Integer.parseInt(maxStr);
                if (min == 0 && max != 0)
                    return false;
                if (min > 29)
                    return false;
                if (max > 29)
                    return false;
                if (max < min)
                    return false;
                if (min > 0) {
                    int interval = max - min;
                    if (length != ((interval + 1) * 2))
                        return false;
                }
                int rawDataLength = 3 + length / 2;
                StringBuffer rawData = new StringBuffer();
                rawData.append(MokoUtils.int2HexString(rawDataLength));
                rawData.append(MokoUtils.int2HexString(dataType));
                rawData.append(MokoUtils.int2HexString(min));
                rawData.append(MokoUtils.int2HexString(max));
                rawData.append(rawDataStr);
                filterRawDatas.add(rawData.toString());
            }
        }
        return true;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.cb_mac_address:
                etMacAddress.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                break;
            case R.id.cb_adv_name:
                etAdvName.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                break;
            case R.id.cb_ibeacon_uuid:
                etIbeaconUuid.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                break;
            case R.id.cb_ibeacon_major:
                llIbeaconMajor.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                break;
            case R.id.cb_ibeacon_minor:
                llIbeaconMinor.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                break;
            case R.id.cb_raw_adv_data:
                llRawDataFilter.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                ivRawDataAdd.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                ivRawDataDel.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                break;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        int rssi = progress - 127;
        tvRssiFilterValue.setText(String.format("%ddBm", rssi));
        tvRssiFilterTips.setText(getString(R.string.rssi_filter, rssi));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @OnClick({R.id.iv_raw_data_del, R.id.iv_raw_data_add})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.iv_raw_data_del:
                final int c = llRawDataFilter.getChildCount();
                if (c == 0) {
                    ToastUtils.showToast(this, "There are currently no filters to delete");
                    return;
                }
                AlertMessageDialog dialog = new AlertMessageDialog();
                dialog.setTitle("Warning");
                dialog.setMessage("Please confirm whether to delete  a filter option，If yes，the last option will be deleted. ");
                dialog.setOnAlertConfirmListener(() -> {
                    if (c > 0) {
                        llRawDataFilter.removeViewAt(c - 1);
                    }
                });
                dialog.show(getSupportFragmentManager());

                break;
            case R.id.iv_raw_data_add:
                int count = llRawDataFilter.getChildCount();
                if (count > 4) {
                    ToastUtils.showToast(this, "You can set up to 5 filters!");
                    return;
                }
                View v = LayoutInflater.from(this).inflate(R.layout.item_raw_data_filter, llRawDataFilter, false);
                llRawDataFilter.addView(v);
                break;
        }
    }
}
