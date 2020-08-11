package com.moko.support.task;


import com.moko.support.MokoConstants;
import com.moko.support.MokoSupport;
import com.moko.support.entity.OrderEnum;
import com.moko.support.entity.OrderType;
import com.moko.support.event.OrderTaskResponseEvent;
import com.moko.support.log.LogModule;
import com.moko.support.utils.MokoUtils;

import org.greenrobot.eventbus.EventBus;

import java.util.Arrays;

public class ReadHumiDataTask extends OrderTask {
    private static final int ORDERDATA_LENGTH = 3;

    public byte[] orderData;

    public ReadHumiDataTask() {
        super(OrderType.CHARACTERISTIC, OrderEnum.READ_HUMI, OrderTask.RESPONSE_TYPE_WRITE_NO_RESPONSE);
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
        int humiEnable = value[3] & 0xFF;
        MokoSupport.getInstance().humiEnable = humiEnable;
        byte[] humiLow = Arrays.copyOfRange(value, 4, 6);
        MokoSupport.getInstance().humiLow = MokoUtils.getDecimalFormat("0.00").format(MokoUtils.toInt(humiLow) * 0.01f);
        byte[] humiHigh = Arrays.copyOfRange(value, 6, 8);
        MokoSupport.getInstance().humiHigh = MokoUtils.getDecimalFormat("0.00").format(MokoUtils.toInt(humiHigh) * 0.01f);
        byte[] humiCurrent = Arrays.copyOfRange(value, 8, 10);
        MokoSupport.getInstance().humiCurrent = MokoUtils.getDecimalFormat("0.00").format(MokoUtils.toInt(humiCurrent) * 0.01f);

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
