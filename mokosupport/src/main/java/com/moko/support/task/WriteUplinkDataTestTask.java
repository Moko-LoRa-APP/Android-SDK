package com.moko.support.task;


import com.moko.support.MokoConstants;
import com.moko.support.MokoSupport;
import com.moko.support.callback.MokoOrderTaskCallback;
import com.moko.support.entity.OrderEnum;
import com.moko.support.entity.OrderType;
import com.moko.support.log.LogModule;

import java.util.Calendar;

public class WriteUplinkDataTestTask extends OrderTask {
    private static final int ORDERDATA_LENGTH = 13;

    public byte[] orderData;

    public WriteUplinkDataTestTask(MokoOrderTaskCallback callback) {
        super(OrderType.CHARACTERISTIC, OrderEnum.WRITE_UPLINK_DATA_TEST, callback, OrderTask.RESPONSE_TYPE_WRITE_NO_RESPONSE);
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR) - 2000;
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int min = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);

        orderData = new byte[ORDERDATA_LENGTH];
        orderData[0] = (byte) MokoConstants.HEADER_SEND;
        orderData[1] = (byte) order.getOrderHeader();
        orderData[2] = (byte) 0x0A;
        orderData[3] = (byte) year;
        orderData[4] = (byte) month;
        orderData[5] = (byte) day;
        orderData[6] = (byte) hour;
        orderData[7] = (byte) min;
        orderData[8] = (byte) second;
        orderData[9] = (byte) 0x4D;
        orderData[10] = (byte) 0x4F;
        orderData[11] = (byte) 0x4B;
        orderData[12] = (byte) 0x4F;

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
        callback.onOrderResult(response);
        MokoSupport.getInstance().executeTask(callback);
    }
}
