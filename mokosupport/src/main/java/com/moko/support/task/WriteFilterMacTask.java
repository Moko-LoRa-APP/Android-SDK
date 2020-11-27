package com.moko.support.task;


import android.text.TextUtils;

import com.moko.support.MokoConstants;
import com.moko.support.MokoSupport;
import com.moko.support.entity.OrderEnum;
import com.moko.support.entity.OrderType;
import com.moko.support.event.OrderTaskResponseEvent;
import com.moko.support.log.LogModule;
import com.moko.support.utils.MokoUtils;

import org.greenrobot.eventbus.EventBus;

public class WriteFilterMacTask extends OrderTask {
    private static final int ORDERDATA_LENGTH = 3;

    public byte[] orderData;

    public WriteFilterMacTask() {
        super(OrderType.CHARACTERISTIC, OrderEnum.WRITE_FILTER_MAC, OrderTask.RESPONSE_TYPE_WRITE_NO_RESPONSE);
    }

    public void setOrderData(String filterMac) {
        if (TextUtils.isEmpty(filterMac)) {
            orderData = new byte[ORDERDATA_LENGTH];
            orderData[0] = (byte) MokoConstants.HEADER_SEND;
            orderData[1] = (byte) order.getOrderHeader();
            orderData[2] = (byte) 0;
        } else {
            byte[] filterMacBytes = MokoUtils.hex2bytes(filterMac);
            int lengh = filterMacBytes.length;
            orderData = new byte[3 + lengh];
            orderData[0] = (byte) MokoConstants.HEADER_SEND;
            orderData[1] = (byte) order.getOrderHeader();
            orderData[2] = (byte) lengh;
            for (int i = 0; i < lengh; i++) {
                orderData[3 + i] = filterMacBytes[i];
            }
        }
    }

    @Override
    public byte[] assemble() {
        return orderData;
    }

    @Override
    public void parseValue(byte[] value) {
        if (value.length != 4)
            return;
        if (order.getOrderHeader() != (value[1] & 0xFF))
            return;
        if (0x01 != (value[2] & 0xFF))
            return;

        response.responseValue = value;

        LogModule.i(order.getOrderName() + "成功");
        orderStatus = OrderTask.ORDER_STATUS_SUCCESS;

        MokoSupport.getInstance().pollTask();
        MokoSupport.getInstance().executeTask();
        OrderTaskResponseEvent event = new OrderTaskResponseEvent();
        event.setAction(MokoConstants.ACTION_ORDER_RESULT);
        event.setResponse(response);
        EventBus.getDefault().post(event);
    }
}
