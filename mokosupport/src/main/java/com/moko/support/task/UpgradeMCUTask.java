package com.moko.support.task;


import com.moko.support.MokoConstants;
import com.moko.support.MokoSupport;
import com.moko.support.callback.MokoOrderTaskCallback;
import com.moko.support.entity.OrderEnum;
import com.moko.support.entity.OrderType;
import com.moko.support.log.LogModule;

public class UpgradeMCUTask extends OrderTask {
    private static final int ORDERDATA_LENGTH = 11;
    // 固件升级
    private byte[] orderData;

    public UpgradeMCUTask(MokoOrderTaskCallback callback) {
        super(OrderType.CHARACTERISTIC_MCU, OrderEnum.UPGRADE_MCU, callback, OrderTask.RESPONSE_TYPE_WRITE_NO_RESPONSE);

    }

    public void setOrderData(byte[] indexCount, byte[] fileCount) {
        orderData = new byte[ORDERDATA_LENGTH];
        orderData[0] = (byte) MokoConstants.HEADER_MCU;
        orderData[1] = (byte) order.getOrderHeader();
        for (int i = 1, indexLength = indexCount.length; i <= indexLength; i++) {
            orderData[i + 1] = indexCount[indexLength - i];
        }
        for (int i = 1, fileLength = fileCount.length; i <= fileLength; i++) {
            orderData[i + 5] = fileCount[fileLength - i];
        }
        orderData[10] = (byte) MokoConstants.END_READ;
    }

    @Override
    public byte[] assemble() {
        return orderData;
    }

    @Override
    public void parseValue(byte[] value) {
        if (value.length != 4)
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
