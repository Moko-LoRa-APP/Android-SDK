package com.moko.support.task;


import com.moko.support.MokoConstants;
import com.moko.support.MokoSupport;
import com.moko.support.entity.OrderEnum;
import com.moko.support.entity.OrderType;
import com.moko.support.event.OrderTaskResponseEvent;
import com.moko.support.log.LogModule;
import com.moko.support.utils.MokoUtils;

import org.greenrobot.eventbus.EventBus;

public class WriteAlarmUploadIntervalTask extends OrderTask {
    private static final int ORDERDATA_LENGTH = 7;

    public byte[] orderData;

    public WriteAlarmUploadIntervalTask() {
        super(OrderType.CHARACTERISTIC, OrderEnum.WRITE_ALARM_UPLOAD_INTERVAL, OrderTask.RESPONSE_TYPE_WRITE_NO_RESPONSE);
    }

    public void setOrderData(int uploadInterval, int scanTime) {
        orderData = new byte[ORDERDATA_LENGTH];
        orderData[0] = (byte) MokoConstants.HEADER_SEND;
        orderData[1] = (byte) order.getOrderHeader();
        orderData[2] = (byte) 0x04;
        byte[] uploadIntervalBytes = MokoUtils.toByteArray(uploadInterval, 2);
        orderData[3] = uploadIntervalBytes[1];
        orderData[4] = uploadIntervalBytes[0];
        byte[] scanTimeBytes = MokoUtils.toByteArray(scanTime, 2);
        orderData[5] = scanTimeBytes[1];
        orderData[6] = scanTimeBytes[0];
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
