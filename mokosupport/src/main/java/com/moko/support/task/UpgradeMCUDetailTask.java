package com.moko.support.task;


import com.moko.support.MokoConstants;
import com.moko.support.callback.MokoOrderTaskCallback;
import com.moko.support.entity.OrderEnum;
import com.moko.support.entity.OrderType;

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
        for (int i = 1; i <= fileLength; i++) {
            orderData[i + 5] = fileBytes[fileLength - i];
        }
    }

    @Override
    public byte[] assemble() {
        return orderData;
    }
}
