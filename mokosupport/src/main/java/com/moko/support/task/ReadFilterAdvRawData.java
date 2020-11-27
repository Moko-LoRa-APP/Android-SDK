package com.moko.support.task;

import android.text.TextUtils;

import com.moko.support.MokoConstants;
import com.moko.support.MokoSupport;
import com.moko.support.entity.OrderEnum;
import com.moko.support.entity.OrderType;
import com.moko.support.event.OrderTaskResponseEvent;
import com.moko.support.utils.MokoUtils;

import org.greenrobot.eventbus.EventBus;

import java.util.Arrays;

public class ReadFilterAdvRawData extends OrderTask {
    private static final int ORDERDATA_LENGTH = 3;

    public byte[] orderData;
    private StringBuffer stringBuffer = new StringBuffer("");

    public ReadFilterAdvRawData() {
        super(OrderType.CHARACTERISTIC, OrderEnum.READ_FILTER_RAW_DATA, OrderTask.RESPONSE_TYPE_WRITE_NO_RESPONSE);
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
        int length = value.length;
        if (length < 5)
            return;
        if (order.getOrderHeader() != (value[1] & 0xFF))
            return;
        int dataLength = (value[2] & 0xFF) - 2;
        int isStart = value[3] & 0xFF;
        int isEnd = value[4] & 0xFF;
        if (dataLength > 0) {
            String data = MokoUtils.bytesToHexString(Arrays.copyOfRange(value, 5, 5 + dataLength));
            stringBuffer.append(data);
        }
        if (isEnd == 0) {
            String rawData = stringBuffer.toString();
            if (!TextUtils.isEmpty(rawData)) {
                byte[] rawDataBytes = MokoUtils.hex2bytes(stringBuffer.toString());
                int rawDataLength = rawDataBytes.length;
                byte[] responseValue = new byte[rawDataLength + 3];
                responseValue[0] = (byte) MokoConstants.HEADER_SEND;
                responseValue[1] = (byte) order.getOrderHeader();
                responseValue[2] = (byte) rawDataLength;
                for (int i = 0; i < rawDataLength; i++) {
                    responseValue[i + 3] = rawDataBytes[i];
                }
                response.responseValue = responseValue;
                MokoSupport.getInstance().filterRawData = rawData;
            } else {
                response.responseValue = value;
                MokoSupport.getInstance().filterRawData = "";
            }
            orderStatus = OrderTask.ORDER_STATUS_SUCCESS;
            MokoSupport.getInstance().pollTask();
            MokoSupport.getInstance().executeTask();
            OrderTaskResponseEvent event = new OrderTaskResponseEvent();
            event.setAction(MokoConstants.ACTION_ORDER_RESULT);
            event.setResponse(response);
            EventBus.getDefault().post(event);
        }
    }
}
