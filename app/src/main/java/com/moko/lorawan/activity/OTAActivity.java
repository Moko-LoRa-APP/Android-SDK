package com.moko.lorawan.activity;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.moko.lorawan.AppConstants;
import com.moko.lorawan.R;
import com.moko.lorawan.dialog.BottomDialog;
import com.moko.lorawan.dialog.LoadingDialog;
import com.moko.lorawan.service.DfuService;
import com.moko.lorawan.service.MokoService;
import com.moko.lorawan.utils.FileUtils;
import com.moko.lorawan.utils.ToastUtils;
import com.moko.support.MokoConstants;
import com.moko.support.MokoSupport;
import com.moko.support.entity.DeviceTypeEnum;
import com.moko.support.entity.OrderEnum;
import com.moko.support.event.ConnectStatusEvent;
import com.moko.support.log.LogModule;
import com.moko.support.task.OrderTask;
import com.moko.support.task.OrderTaskResponse;
import com.moko.support.utils.MokoUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;
import no.nordicsemi.android.dfu.DfuLogListener;
import no.nordicsemi.android.dfu.DfuProgressListener;
import no.nordicsemi.android.dfu.DfuProgressListenerAdapter;
import no.nordicsemi.android.dfu.DfuServiceInitiator;
import no.nordicsemi.android.dfu.DfuServiceListenerHelper;

public class OTAActivity extends BaseActivity {
    public static final int REQUEST_CODE_SELECT_FIRMWARE = 0x10;

    @Bind(R.id.tv_file_path)
    TextView tvFilePath;
    @Bind(R.id.tv_ota)
    TextView tvOta;

    private String mDeviceMac;
    private String mDeviceName;
    private boolean mReceiverTag = false;
    private String[] mOTAs;
    private int mOTASelected;
    private MokoService mMokoService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ota);
        ButterKnife.bind(this);
        mDeviceName = getIntent().getStringExtra(AppConstants.EXTRA_KEY_DEVICE_NAME);
        mDeviceMac = getIntent().getStringExtra(AppConstants.EXTRA_KEY_DEVICE_MAC);
        mOTAs = getResources().getStringArray(R.array.OTA);
        bindService(new Intent(this, MokoService.class), mServiceConnection, BIND_AUTO_CREATE);
        EventBus.getDefault().register(this);
        mOTASelected = 1;
        tvOta.setText(mOTAs[mOTASelected]);
    }


    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mMokoService = ((MokoService.LocalBinder) service).getService();
            // 注册广播接收器
            IntentFilter filter = new IntentFilter();
            filter.addAction(MokoConstants.ACTION_ORDER_RESULT);
            filter.addAction(MokoConstants.ACTION_ORDER_TIMEOUT);
            filter.addAction(MokoConstants.ACTION_ORDER_FINISH);
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            filter.setPriority(300);
            registerReceiver(mReceiver, filter);
            mReceiverTag = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                    switch (blueState) {
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            dismissDFUProgressDialog();
                            finish();
                            break;
                    }
                }
                if (MokoConstants.ACTION_ORDER_TIMEOUT.equals(action)) {
                    abortBroadcast();
                    OrderTaskResponse response = (OrderTaskResponse) intent.getSerializableExtra(MokoConstants.EXTRA_KEY_RESPONSE_ORDER_TASK);

                    OrderEnum orderEnum = response.order;
                    byte[] value = response.responseValue;
                    switch (orderEnum) {
                        case UPGRADE_MCU:
                        case UPGRADE_MCU_DETAIL:
                            onUpgradeFailure();
                            tvFilePath.post(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        sendData();
                                    } catch (IOException e) {
                                        onUpgradeFailure();
                                    }
                                }
                            });
                            break;
                    }
                }
                if (MokoConstants.ACTION_ORDER_FINISH.equals(action)) {
                }
                if (MokoConstants.ACTION_ORDER_RESULT.equals(action)) {
                    abortBroadcast();
                    OrderTaskResponse response = (OrderTaskResponse) intent.getSerializableExtra(MokoConstants.EXTRA_KEY_RESPONSE_ORDER_TASK);
                    OrderEnum orderEnum = response.order;
                    byte[] value = response.responseValue;
                    switch (orderEnum) {
                        case UPGRADE_MCU:
                            dismissLoadingProgressDialog();
                            if ((value[3] & 0xff) != 0xAA) {
                                ToastUtils.showToast(OTAActivity.this, "Error");
                                onUpgradeFailure();
                                return;
                            }
                            tvFilePath.post(new Runnable() {
                                @Override
                                public void run() {
                                    sendUpgradeFile();
                                }
                            });
                            break;
                        case UPGRADE_MCU_DETAIL:
                            if ((value[1] & 0xFF) == orderEnum.getOrderHeader()) {
                                // 升级成功
                                isStop = true;
                                dismissDFUProgressDialog();
                                ToastUtils.showToast(OTAActivity.this, "DfuCompleted!");
                            }
                            tvFilePath.post(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        sendData();
                                    } catch (IOException e) {
                                        onUpgradeFailure();
                                    }
                                }
                            });
                            break;
                    }
                }
            }
        }
    };


    private int index;
    private InputStream in;
    private boolean isStop;

    private void sendUpgradeFile() {
        showDFUProgressDialog("Waiting...");
        try {
            index = 0;
            isStop = false;
            if (in == null) {
                in = new FileInputStream(firmwareFile);
            }
            sendData();
        } catch (Exception e) {
            onUpgradeFailure();
        }
    }

    private void sendData() throws IOException {
        if (in == null)
            return;
        int unReadLength = in.available();
        if (unReadLength > 0 && !isStop) {
            byte[] packageIndex = MokoUtils.toByteArray(index, 4);
            byte fileByte[] = new byte[unReadLength < 14 ? unReadLength : 14];
            in.read(fileByte);
            upgradeBand(packageIndex, fileByte);
            index++;
            if (in == null) {
                return;
            }
            long length = firmwareFile.length();
            int read = (int) (length - unReadLength);
            final int percent = (int) (((float) read / (float) length) * 100);
            mDFUDialog.setMessage("Progress:" + percent + "%");
        } else {
            in.close();
            in = null;
        }
    }

    public void upgradeBand(byte[] packageIndex, byte[] fileBytes) {
        OrderTask task = mMokoService.getUpgradeMCUDetailOrderTask(packageIndex, fileBytes);
        MokoSupport.getInstance().sendOrder(task);
    }

    private void onUpgradeFailure() {
        isStop = true;
        dismissDFUProgressDialog();
        ToastUtils.showToast(this, "Error:DFU Failed");
        mMokoService.disConnectBle();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConnectStatusEvent(ConnectStatusEvent event) {
        String action = event.getAction();
        if (MokoConstants.ACTION_CONN_STATUS_DISCONNECTED.equals(action)) {
            // 设备断开
            tvFilePath.postDelayed(new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            }, 1000);

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mReceiverTag) {
            mReceiverTag = false;
            // 注销广播
            unregisterReceiver(mReceiver);
        }
        unbindService(mServiceConnection);
        EventBus.getDefault().unregister(this);
    }

    public void back(View view) {
        finish();
    }

    public void selectFile(View view) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");//设置类型，我这里是任意类型，任意后缀的可以这样写。
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "select file first!"), REQUEST_CODE_SELECT_FIRMWARE);
        } catch (ActivityNotFoundException ex) {
            ToastUtils.showToast(this, "install file manager app");
        }
    }

    private File firmwareFile;

    public void upgrade(View view) {
        String filePath = tvFilePath.getText().toString();
        if (TextUtils.isEmpty(filePath)) {
            ToastUtils.showToast(this, "select file first!");
            return;
        }
        if (!MokoSupport.getInstance().isConnDevice(this, mDeviceMac)) {
            ToastUtils.showToast(this, "Device is disconnected");
            return;
        }
        firmwareFile = new File(filePath);
        if (firmwareFile.exists()) {
            if (mOTASelected == 1) {
                final DfuServiceInitiator starter = new DfuServiceInitiator(mDeviceMac)
                        .setDeviceName(mDeviceName)
                        .setKeepBond(false)
                        .setDisableNotification(true);
                starter.setZip(null, filePath);
                starter.start(this, DfuService.class);
                showDFUProgressDialog("Waiting...");
            } else {
                showLoadingProgressDialog();
                int fileLength = (int) firmwareFile.length();
                int indexLength;
                if (fileLength % 14 > 0) {
                    indexLength = fileLength / 14 + 1;
                } else {
                    indexLength = fileLength / 14;
                }
                byte[] indexCount = MokoUtils.toByteArray(indexLength, 4);
                byte[] fileCount = MokoUtils.toByteArray(fileLength, 4);
                MokoSupport.getInstance().sendOrder(mMokoService.getUpgradeMCUOrderTask(indexCount, fileCount));
            }
        } else {
            Toast.makeText(this, "file is not exists!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SELECT_FIRMWARE && resultCode == RESULT_OK) {
            //得到uri，后面就是将uri转化成file的过程。
            Uri uri = data.getData();
            String firmwareFilePath = FileUtils.getPath(this, uri);
            tvFilePath.setText(firmwareFilePath);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        DfuServiceListenerHelper.registerProgressListener(this, mDfuProgressListener);
        DfuServiceListenerHelper.registerLogListener(this, mDfuLogListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        DfuServiceListenerHelper.unregisterProgressListener(this, mDfuProgressListener);
        DfuServiceListenerHelper.unregisterLogListener(this, mDfuLogListener);
    }

    private int mDeviceConnectCount;

    private final DfuProgressListener mDfuProgressListener = new DfuProgressListenerAdapter() {
        @Override
        public void onDeviceConnecting(String deviceAddress) {
            LogModule.w("onDeviceConnecting...");
            mDeviceConnectCount++;
            if (mDeviceConnectCount > 3) {
                Toast.makeText(OTAActivity.this, "Error:DFU Failed", Toast.LENGTH_SHORT).show();
                dismissDFUProgressDialog();
                final LocalBroadcastManager manager = LocalBroadcastManager.getInstance(OTAActivity.this);
                final Intent abortAction = new Intent(DfuService.BROADCAST_ACTION);
                abortAction.putExtra(DfuService.EXTRA_ACTION, DfuService.ACTION_ABORT);
                manager.sendBroadcast(abortAction);
            }
        }

        @Override
        public void onDeviceDisconnecting(String deviceAddress) {
            LogModule.w("onDeviceDisconnecting...");
        }

        @Override
        public void onDfuProcessStarting(String deviceAddress) {
            mDFUDialog.setMessage("DfuProcessStarting...");
        }


        @Override
        public void onEnablingDfuMode(String deviceAddress) {
            mDFUDialog.setMessage("EnablingDfuMode...");
        }

        @Override
        public void onFirmwareValidating(String deviceAddress) {
            mDFUDialog.setMessage("FirmwareValidating...");
        }

        @Override
        public void onDfuCompleted(String deviceAddress) {
            ToastUtils.showToast(OTAActivity.this, "DfuCompleted!");
            dismissDFUProgressDialog();
        }

        @Override
        public void onDfuAborted(String deviceAddress) {
            mDFUDialog.setMessage("DfuAborted...");
        }

        @Override
        public void onProgressChanged(String deviceAddress, int percent, float speed, float avgSpeed, int currentPart, int partsTotal) {
            mDFUDialog.setMessage("Progress:" + percent + "%");
        }

        @Override
        public void onError(String deviceAddress, int error, int errorType, String message) {
            Toast.makeText(OTAActivity.this, "Error:" + message, Toast.LENGTH_SHORT).show();
            LogModule.i("Error:" + message);
            dismissDFUProgressDialog();
        }
    };

    private final DfuLogListener mDfuLogListener = new DfuLogListener() {
        @Override
        public void onLogEvent(String deviceAddress, int level, String message) {
            switch (level) {
                case DfuService.LOG_LEVEL_APPLICATION:
                    LogModule.w(level + ":" + message);
                    break;
                case DfuService.LOG_LEVEL_VERBOSE:
                    LogModule.w(level + ":" + message);
                    break;
                case DfuService.LOG_LEVEL_DEBUG:
                    LogModule.w(level + ":" + message);
                    break;
                case DfuService.LOG_LEVEL_INFO:
                    LogModule.w(level + ":" + message);
                    break;
                case DfuService.LOG_LEVEL_WARNING:
                    LogModule.w(level + ":" + message);
                    break;
                case DfuService.LOG_LEVEL_ERROR:
                    LogModule.w(level + ":" + message);
                    break;
            }
        }
    };

    private LoadingDialog mLoadingDialog;

    private void showLoadingProgressDialog() {
        mLoadingDialog = new LoadingDialog();
        mLoadingDialog.show(getSupportFragmentManager());

    }

    private void dismissLoadingProgressDialog() {
        if (mLoadingDialog != null)
            mLoadingDialog.dismissAllowingStateLoss();
    }

    private ProgressDialog mDFUDialog;

    private void showDFUProgressDialog(String tips) {
        mDFUDialog = new ProgressDialog(OTAActivity.this);
        mDFUDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mDFUDialog.setCanceledOnTouchOutside(false);
        mDFUDialog.setCancelable(false);
        mDFUDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mDFUDialog.setMessage(tips);
        if (!isFinishing() && mDFUDialog != null && !mDFUDialog.isShowing()) {
            mDFUDialog.show();
        }
    }

    private void dismissDFUProgressDialog() {
        mDeviceConnectCount = 0;
        if (!isFinishing() && mDFUDialog != null && mDFUDialog.isShowing()) {
            mDFUDialog.dismiss();
        }
    }

    public void selectOTAType(View view) {
//        if (MokoSupport.deviceTypeEnum == DeviceTypeEnum.LW001_BG
//                || MokoSupport.deviceTypeEnum == DeviceTypeEnum.LW003_B) {
//            return;
//        }
//        ArrayList<String> otas = new ArrayList<>();
//        for (int i = 0; i < mOTAs.length; i++) {
//            otas.add(mOTAs[i]);
//        }
//        BottomDialog bottomDialog = new BottomDialog();
//        bottomDialog.setDatas(otas, mOTASelected);
//        bottomDialog.setListener(new BottomDialog.OnBottomListener() {
//            @Override
//            public void onValueSelected(int value) {
//                mOTASelected = value;
//                tvOta.setText(mOTAs[mOTASelected]);
//            }
//        });
//        bottomDialog.show(getSupportFragmentManager());
    }
}
