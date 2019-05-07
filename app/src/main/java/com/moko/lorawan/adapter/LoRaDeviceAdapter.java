package com.moko.lorawan.adapter;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.moko.lorawan.R;
import com.moko.support.entity.DeviceInfo;

public class LoRaDeviceAdapter extends BaseQuickAdapter<DeviceInfo, BaseViewHolder> {

    public LoRaDeviceAdapter() {
        super(R.layout.item_scan_device);
    }

    @Override
    protected void convert(BaseViewHolder helper, DeviceInfo item) {
        helper.setText(R.id.tv_device_name, item.name);
        helper.setText(R.id.tv_device_rssi, item.rssi + "");
    }
}
