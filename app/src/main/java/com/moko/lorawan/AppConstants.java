package com.moko.lorawan;

public class AppConstants {
    // data time pattern
    public static final String PATTERN_HH_MM = "HH:mm";
    public static final String PATTERN_YYYY_MM_DD = "yyyy-MM-dd";
    public static final String PATTERN_MM_DD = "MM/dd";
    public static final String PATTERN_MM_DD_2 = "MM-dd";
    public static final String PATTERN_YYYY_MM_DD_HH_MM = "yyyy-MM-dd HH:mm";
    // sp
    public static final String SP_NAME = "sp_name_lorawan";

    public static final String SP_KEY_DEVICE_ADDRESS = "sp_key_device_address";
    public static final String SP_KEY_SAVED_PASSWORD = "SP_KEY_SAVED_PASSWORD";
    // extra_key
    // 设备列表
    public static final String EXTRA_KEY_RESPONSE_ORDER_TYPE = "EXTRA_KEY_RESPONSE_ORDER_TYPE";
    public static final String EXTRA_KEY_DEVICE_NAME = "EXTRA_KEY_DEVICE_NAME";
    public static final String EXTRA_KEY_DEVICE_MAC = "EXTRA_KEY_DEVICE_MAC";
    // request_code
    public static final int REQUEST_CODE_TEMP_TARGET = 100;
    public static final int REQUEST_CODE_ENABLE_BT = 1001;


    public static final int REQUEST_CODE_PERMISSION = 120;
    public static final int REQUEST_CODE_PERMISSION_2 = 121;
    public static final int REQUEST_CODE_LOCATION_SETTINGS = 122;
    public static final int REQUEST_CODE_LORA_SETTING = 123;
    public static final int REQUEST_CODE_REFRESH = 124;
    public static final int REQUEST_CODE_SETTING = 125;
    public static final int REQUEST_CODE_BASIC = 126;
    public static final int PERMISSION_REQUEST_CODE = 1;

    // result_code
    public static final int RESULT_CONN_DISCONNECTED = 2;
}
