package com.moko.support.task;


import com.moko.support.MokoConstants;
import com.moko.support.MokoSupport;
import com.moko.support.entity.OrderEnum;
import com.moko.support.entity.OrderType;
import com.moko.support.event.OrderTaskResponseEvent;
import com.moko.support.log.LogModule;

import org.greenrobot.eventbus.EventBus;

public class UpgradeMCUDetailTask extends OrderTask {
    // 固件升级

    private byte[] orderData;

    public UpgradeMCUDetailTask(byte[] packageIndex, byte[] fileBytes) {
        super(OrderType.CHARACTERISTIC_MCU, OrderEnum.UPGRADE_MCU_DETAIL, OrderTask.RESPONSE_TYPE_WRITE_NO_RESPONSE);
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
        if ((value.length == 4)) {
            if ((value[1] & 0xFF) != order.getOrderHeader())
                return;
            if ((value[2] & 0xFF) != 0x01)
                return;
            if ((value[3] & 0xff) != 0xAA)
                return;
            LogModule.i(order.getOrderName() + "成功");
        } else if ((value.length == 7)) {
            if ((value[1] & 0xFF) != 0x43)
                return;
            if ((value[6] & 0xFF) != 0xAA)
                return;
        } else {
            return;
        }
        response.responseValue = value;
        orderStatus = OrderTask.ORDER_STATUS_SUCCESS;
        MokoSupport.getInstance().pollTask();
        OrderTaskResponseEvent event = new OrderTaskResponseEvent();
        event.setAction(MokoConstants.ACTION_ORDER_RESULT);
        event.setResponse(response);
        EventBus.getDefault().post(event);
    }

    @Override
    public boolean timeoutPreTask() {
        MokoSupport.getInstance().pollTask();
        MokoSupport.getInstance().executeTask();
        OrderTaskResponseEvent event = new OrderTaskResponseEvent();
        event.setAction(MokoConstants.ACTION_ORDER_TIMEOUT);
        event.setResponse(response);
        EventBus.getDefault().post(event);
        return false;
    }
}
