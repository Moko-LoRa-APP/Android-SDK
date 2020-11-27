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

public class ReadFilterMinorTask extends OrderTask {
    private static final int ORDERDATA_LENGTH = 3;

    public byte[] orderData;

    public ReadFilterMinorTask() {
        super(OrderType.CHARACTERISTIC, OrderEnum.READ_FILTER_MINOR, OrderTask.RESPONSE_TYPE_WRITE_NO_RESPONSE);
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
        if (order.getOrderHeader() != (value[1] & 0xFF))
            return;
        int length = value[2] & 0xFF;
        if (length > 0) {
            byte[] filterMinorMin = Arrays.copyOfRange(value, 3, 5);
            byte[] filterMinorMax = Arrays.copyOfRange(value, 5, value.length);
            MokoSupport.getInstance().filterMinorMin = String.valueOf(MokoUtils.toInt(filterMinorMin));
            MokoSupport.getInstance().filterMinorMax = String.valueOf(MokoUtils.toInt(filterMinorMax));
        } else {
            MokoSupport.getInstance().filterMinorMin = "";
            MokoSupport.getInstance().filterMinorMax = "";
        }

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
