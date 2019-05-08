package com.moko.support.task;


import com.moko.support.MokoConstants;
import com.moko.support.MokoSupport;
import com.moko.support.callback.MokoOrderTaskCallback;
import com.moko.support.entity.OrderEnum;
import com.moko.support.entity.OrderType;
import com.moko.support.log.LogModule;
import com.moko.support.utils.ByteArrayConveter;

public class ReadGPSTask extends OrderTask {
    private static final int ORDERDATA_LENGTH = 3;

    public byte[] orderData;

    public ReadGPSTask(MokoOrderTaskCallback callback) {
        super(OrderType.CHARACTERISTIC, OrderEnum.READ_GPS, callback, OrderTask.RESPONSE_TYPE_WRITE_NO_RESPONSE);
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
        if (value.length != 19)
            return;
        if (order.getOrderHeader() != (value[1] & 0xFF))
            return;
        if (0x10 != (value[2] & 0xFF))
            return;
        float latitude = ByteArrayConveter.getFloat(value, 3);
        MokoSupport.getInstance().setLatitude(String.format("N,%f", latitude));
        float longitude = ByteArrayConveter.getFloat(value, 7);
        MokoSupport.getInstance().setLongitude(String.format("E,%f", longitude));
        float sltitude = ByteArrayConveter.getFloat(value, 11);
        MokoSupport.getInstance().setAltitude(String.format("%f", sltitude));
        float speed = ByteArrayConveter.getFloat(value, 15);
        MokoSupport.getInstance().setSpeed(String.format("%f", speed));

        LogModule.i(order.getOrderName() + "成功");
        orderStatus = OrderTask.ORDER_STATUS_SUCCESS;

        MokoSupport.getInstance().pollTask();
        callback.onOrderResult(response);
        MokoSupport.getInstance().executeTask(callback);
    }
}