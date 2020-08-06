package com.moko.support.task;


import com.moko.support.MokoConstants;
import com.moko.support.MokoSupport;
import com.moko.support.callback.MokoOrderTaskCallback;
import com.moko.support.entity.OrderEnum;
import com.moko.support.entity.OrderType;
import com.moko.support.event.OrderTaskResponseEvent;
import com.moko.support.log.LogModule;
import com.moko.support.utils.MokoUtils;

import org.greenrobot.eventbus.EventBus;

import java.util.Arrays;

public class ReadTempDataTask extends OrderTask {
    private static final int ORDERDATA_LENGTH = 3;

    public byte[] orderData;

    public ReadTempDataTask(MokoOrderTaskCallback callback) {
        super(OrderType.CHARACTERISTIC, OrderEnum.READ_TEMP, callback, OrderTask.RESPONSE_TYPE_WRITE_NO_RESPONSE);
        orderData = new byte[ORDERDATA_LENGTH];
        orderData[0] = (byte) MokoConstants.HEADER_SEND;
        orderData[1] = (byte) order.getOrderHeader();
        orderData[2] = (byte) MokoConstants.END_READ;
    }

    @Override
    public byte[] assemble() {
        return orderData;
    }

    @Override
    public void parseValue(byte[] value) {
        if (value.length != 0x0A)
            return;
        if (order.getOrderHeader() != (value[1] & 0xFF))
            return;
        if (0x07 != (value[2] & 0xFF))
            return;
        int tempEnable = value[3] & 0xFF;
        MokoSupport.getInstance().tempEnable = tempEnable;
        byte[] tempLow = Arrays.copyOfRange(value, 4, 6);
        MokoSupport.getInstance().tempLow = MokoUtils.getDecimalFormat("0.00").format(MokoUtils.toInt(tempLow) * 0.01f);
        byte[] tempHigh = Arrays.copyOfRange(value, 6, 8);
        MokoSupport.getInstance().tempHigh = MokoUtils.getDecimalFormat("0.00").format(MokoUtils.toInt(tempHigh) * 0.01f);
        byte[] tempCurrent = Arrays.copyOfRange(value, 8, 10);
        MokoSupport.getInstance().tempCurrent = MokoUtils.getDecimalFormat("0.00").format((MokoUtils.toInt(tempCurrent) - 4500) * 0.01f);

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
