package com.moko.support.callback;

import com.moko.support.task.OrderTaskResponse;

/**
 * @Date 2017/5/10
 * @Author wenzheng.liu
 * @Description 返回数据回调类
 * @ClassPath com.moko.support.callback.OrderCallback
 */
@Deprecated
public interface MokoOrderTaskCallback {
    @Deprecated
    void onOrderResult(OrderTaskResponse response);
    @Deprecated
    void onOrderTimeout(OrderTaskResponse response);
    @Deprecated
    void onOrderFinish();
}
