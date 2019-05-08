package com.moko.support.task;


import com.moko.support.MokoConstants;
import com.moko.support.MokoSupport;
import com.moko.support.callback.MokoOrderTaskCallback;
import com.moko.support.entity.OrderEnum;
import com.moko.support.entity.OrderType;
import com.moko.support.log.LogModule;
import com.moko.support.utils.MokoUtils;

import java.util.Arrays;

public class Read9AxisAngleTask extends OrderTask {
    private static final int ORDERDATA_LENGTH = 3;

    public byte[] orderData;

    public Read9AxisAngleTask(MokoOrderTaskCallback callback) {
        super(OrderType.CHARACTERISTIC, OrderEnum.READ_9_AXIS_M, callback, OrderTask.RESPONSE_TYPE_WRITE_NO_RESPONSE);
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
        if (value.length != 7)
            return;
        if (order.getOrderHeader() != (value[1] & 0xFF))
            return;
        if (0x04 != (value[2] & 0xFF))
            return;
        byte[] xBytes = Arrays.copyOfRange(value, 3, 5);
        byte[] xBytesReverse = new byte[2];
        xBytesReverse[0] = xBytes[1];
        xBytesReverse[1] = xBytes[0];
        float x_angle = MokoUtils.toInt(xBytesReverse) * 0.01f;
        MokoSupport.getInstance().x_angle = String.format("%f", x_angle);
        byte[] yBytes = Arrays.copyOfRange(value, 5, 7);
        byte[] yBytesReverse = new byte[2];
        yBytesReverse[0] = yBytes[1];
        yBytesReverse[1] = yBytes[0];
        float y_angle = MokoUtils.toInt(yBytesReverse) * 0.01f;
        MokoSupport.getInstance().y_angle = String.format("%f", y_angle);

        LogModule.i(order.getOrderName() + "成功");
        orderStatus = OrderTask.ORDER_STATUS_SUCCESS;

        MokoSupport.getInstance().pollTask();
        callback.onOrderResult(response);
        MokoSupport.getInstance().executeTask(callback);
    }
}
