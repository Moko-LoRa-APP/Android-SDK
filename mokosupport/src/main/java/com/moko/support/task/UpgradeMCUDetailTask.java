package com.moko.support.task;


import com.moko.support.MokoConstants;
import com.moko.support.MokoSupport;
import com.moko.support.callback.MokoOrderTaskCallback;
import com.moko.support.entity.OrderEnum;
import com.moko.support.entity.OrderType;
import com.moko.support.log.LogModule;

public class UpgradeMCUDetailTask extends OrderTask {
    // 固件升级

    private byte[] orderData;

    public UpgradeMCUDetailTask(MokoOrderTaskCallback callback, byte[] packageIndex, byte[] fileBytes) {
        super(OrderType.CHARACTERISTIC_MCU, OrderEnum.UPGRADE_MCU_DETAIL, callback, OrderTask.RESPONSE_TYPE_WRITE_NO_RESPONSE);
        int fileLength = fileBytes.length;
        int dataLength = fileLength + 6;
        orderData = new byte[dataLength];
        orderData[0] = (byte) MokoConstants.HEADER_MCU;
        orderData[1] = (byte) order.getOrderHeader();
        for (int i = 1, indexLength = packageIndex.length; i <= indexLength; i++) {
            orderData[i + 1] = packageIndex[indexLength - i];
        }
        for (int i = 0; i < fileLength; i++) {
            orderData[i + 6] = fileBytes[i];
        }
    }

    @Override
    public byte[] assemble() {
        return orderData;
    }


    @Override
    public void parseValue(byte[] value) {
        if ((value.length == 4)
                && (value[1] & 0xFF) == order.getOrderHeader()
                && (value[2] & 0xFF) == 0x01
                && (value[3] & 0xff) == 0xAA) {
            LogModule.i(order.getOrderName() + "成功");
        } else if ((value.length == 7)
                && (value[1] & 0xFF) == 0x43
                && (value[6] & 0xFF) != 0xAA) {
            return;
        }
        response.responseValue = value;
        orderStatus = OrderTask.ORDER_STATUS_SUCCESS;
        MokoSupport.getInstance().pollTask();
        callback.onOrderResult(response);
        MokoSupport.getInstance().executeTask(callback);
    }
}
