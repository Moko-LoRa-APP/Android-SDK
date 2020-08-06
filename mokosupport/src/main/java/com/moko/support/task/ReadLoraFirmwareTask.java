package com.moko.support.task;


import com.moko.support.MokoConstants;
import com.moko.support.MokoSupport;
import com.moko.support.callback.MokoOrderTaskCallback;
import com.moko.support.entity.DeviceTypeEnum;
import com.moko.support.entity.OrderEnum;
import com.moko.support.entity.OrderType;
import com.moko.support.event.OrderTaskResponseEvent;
import com.moko.support.log.LogModule;

import org.greenrobot.eventbus.EventBus;

import java.util.Arrays;

public class ReadLoraFirmwareTask extends OrderTask {
    private static final int ORDERDATA_LENGTH = 3;

    public byte[] orderData;

    public ReadLoraFirmwareTask(MokoOrderTaskCallback callback) {
        super(OrderType.CHARACTERISTIC, OrderEnum.READ_LORA_FIRMWARE, callback, OrderTask.RESPONSE_TYPE_WRITE_NO_RESPONSE);
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
        if (order.getOrderHeader() != (value[1] & 0xFF))
            return;
        byte[] loraFirmware = Arrays.copyOfRange(value, 3, value.length);
        if (MokoSupport.deviceTypeEnum != DeviceTypeEnum.LW004_BP) {
            MokoSupport.getInstance().setLoraFirmware(new String(loraFirmware));
        } else {
            String loraFirmwareStr = String.format("%d.%d.%d", loraFirmware[0] & 0xFF, loraFirmware[1] & 0xFF, loraFirmware[2] & 0xFF);
            MokoSupport.getInstance().setLoraFirmware(loraFirmwareStr);
        }
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
