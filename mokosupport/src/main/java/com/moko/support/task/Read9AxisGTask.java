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

public class Read9AxisGTask extends OrderTask {
    private static final int ORDERDATA_LENGTH = 3;

    public byte[] orderData;

    public Read9AxisGTask(MokoOrderTaskCallback callback) {
        super(OrderType.CHARACTERISTIC, OrderEnum.READ_9_AXIS_G, callback, OrderTask.RESPONSE_TYPE_WRITE_NO_RESPONSE);
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
        byte[] gxBytes = Arrays.copyOfRange(value, 3, 5);
        byte[] gxBytesReverse = new byte[2];
        gxBytesReverse[0] = gxBytes[1];
        gxBytesReverse[1] = gxBytes[0];
        MokoSupport.getInstance().gx = MokoUtils.bytesToHexString(gxBytesReverse);
        byte[] gyBytes = Arrays.copyOfRange(value, 5, 7);
        byte[] gyBytesReverse = new byte[2];
        gyBytesReverse[0] = gyBytes[1];
        gyBytesReverse[1] = gyBytes[0];
        MokoSupport.getInstance().gy = MokoUtils.bytesToHexString(gyBytesReverse);
        byte[] gzBytes = Arrays.copyOfRange(value, 7, 9);
        byte[] gzBytesReverse = new byte[2];
        gzBytesReverse[0] = gzBytes[1];
        gzBytesReverse[1] = gzBytes[0];
        MokoSupport.getInstance().gz = MokoUtils.bytesToHexString(gzBytesReverse);

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
