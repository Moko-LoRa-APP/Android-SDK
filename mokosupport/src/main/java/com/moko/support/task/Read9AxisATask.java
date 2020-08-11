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

public class Read9AxisATask extends OrderTask {
    private static final int ORDERDATA_LENGTH = 3;

    public byte[] orderData;

    public Read9AxisATask() {
        super(OrderType.CHARACTERISTIC, OrderEnum.READ_9_AXIS_A, OrderTask.RESPONSE_TYPE_WRITE_NO_RESPONSE);
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
        if (value.length != 9)
            return;
        if (order.getOrderHeader() != (value[1] & 0xFF))
            return;
        if (0x06 != (value[2] & 0xFF))
            return;
        byte[] axBytes = Arrays.copyOfRange(value, 3, 5);
        byte[] axBytesReverse = new byte[2];
        axBytesReverse[0] = axBytes[1];
        axBytesReverse[1] = axBytes[0];
        MokoSupport.getInstance().ax = MokoUtils.bytesToHexString(axBytesReverse);
        byte[] ayBytes = Arrays.copyOfRange(value, 5, 7);
        byte[] ayBytesReverse = new byte[2];
        ayBytesReverse[0] = ayBytes[1];
        ayBytesReverse[1] = ayBytes[0];
        MokoSupport.getInstance().ay = MokoUtils.bytesToHexString(ayBytesReverse);
        byte[] azBytes = Arrays.copyOfRange(value, 7, 9);
        byte[] azBytesReverse = new byte[2];
        azBytesReverse[0] = azBytes[1];
        azBytesReverse[1] = azBytes[0];
        MokoSupport.getInstance().az = MokoUtils.bytesToHexString(azBytesReverse);

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
