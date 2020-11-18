package com.moko.support.task;

import com.moko.support.MokoConstants;
import com.moko.support.MokoSupport;
import com.moko.support.entity.OrderEnum;
import com.moko.support.entity.OrderType;
import com.moko.support.event.OrderTaskResponseEvent;
import com.moko.support.log.LogModule;

import org.greenrobot.eventbus.EventBus;

public class VerifyPasswordTask extends OrderTask {
    public byte[] data;

    public VerifyPasswordTask() {
        super(OrderType.CHARACTERISTIC_NOTIFY, OrderEnum.NOTIFY, OrderTask.RESPONSE_TYPE_WRITE_NO_RESPONSE);
    }

    public void setData(String password) {
        this.data = new byte[9];
        byte[] passwordBytes = password.getBytes();
        int length = passwordBytes.length;
        data[0] = (byte) 0xED;
        for (int i = 0; i < length; i++) {
            data[i + 1] = passwordBytes[i];
        }
    }

    @Override
    public byte[] assemble() {
        return data;
    }

    @Override
    public void parseValue(byte[] value) {
        if (value.length != 2)
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
