package com.moko.support.task;


import com.moko.support.MokoConstants;
import com.moko.support.MokoSupport;
import com.moko.support.callback.MokoOrderTaskCallback;
import com.moko.support.entity.OrderEnum;
import com.moko.support.entity.OrderType;
import com.moko.support.log.LogModule;
import com.moko.support.utils.MokoUtils;

import java.util.Arrays;

public class Read9AxisMTask extends OrderTask {
    private static final int ORDERDATA_LENGTH = 3;

    public byte[] orderData;

    public Read9AxisMTask(MokoOrderTaskCallback callback) {
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
        if (value.length != 9)
            return;
        if (order.getOrderHeader() != (value[1] & 0xFF))
            return;
        if (0x06 != (value[2] & 0xFF))
            return;
        byte[] mxBytes = Arrays.copyOfRange(value, 3, 5);
        byte[] mxBytesReverse = new byte[2];
        mxBytesReverse[0] = mxBytes[1];
        mxBytesReverse[1] = mxBytes[0];
        MokoSupport.getInstance().mx = String.format("%#x", MokoUtils.toInt(mxBytesReverse));
        byte[] myBytes = Arrays.copyOfRange(value, 5, 7);
        byte[] myBytesReverse = new byte[2];
        myBytesReverse[0] = myBytes[1];
        myBytesReverse[1] = myBytes[0];
        MokoSupport.getInstance().my = String.format("%#x", MokoUtils.toInt(myBytesReverse));
        byte[] mzBytes = Arrays.copyOfRange(value, 7, 9);
        byte[] mzBytesReverse = new byte[2];
        mzBytesReverse[0] = mzBytes[1];
        mzBytesReverse[1] = mzBytes[0];
        MokoSupport.getInstance().mz = String.format("%#x", MokoUtils.toInt(mzBytesReverse));

        LogModule.i(order.getOrderName() + "成功");
        orderStatus = OrderTask.ORDER_STATUS_SUCCESS;

        MokoSupport.getInstance().pollTask();
        callback.onOrderResult(response);
        MokoSupport.getInstance().executeTask(callback);
    }
}
