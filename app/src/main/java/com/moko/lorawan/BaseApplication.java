package com.moko.lorawan;

import android.app.Application;
import android.content.Intent;

import com.moko.lorawan.service.MokoService;
import com.moko.support.MokoSupport;

import es.dmoral.toasty.Toasty;

public class BaseApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Toasty.Config.getInstance().apply();
        MokoSupport.getInstance().init(getApplicationContext());
        // 启动服务
        startService(new Intent(this, MokoService.class));
    }
}
