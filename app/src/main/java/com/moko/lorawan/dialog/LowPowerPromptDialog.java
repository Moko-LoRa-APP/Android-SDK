package com.moko.lorawan.dialog;

import android.text.TextUtils;
import android.view.View;

import com.moko.lorawan.R;
import com.moko.lorawan.view.WheelView;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class LowPowerPromptDialog extends MokoBaseDialog {


    @Bind(R.id.wv_bottom)
    WheelView wvBottom;
    private ArrayList<String> mDatas;
    private int mIndex;


    @Override
    public int getLayoutRes() {
        return R.layout.dialog_bottom;
    }

    @Override
    public void bindView(View v) {
        ButterKnife.bind(this, v);
        wvBottom.setData(createData());
        wvBottom.setDefault(mIndex);
    }

    @Override
    public float getDimAmount() {
        return 0.7f;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    @OnClick({R.id.tv_cancel, R.id.tv_confirm})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.tv_cancel:
                dismiss();
                break;
            case R.id.tv_confirm:
                if (TextUtils.isEmpty(wvBottom.getSelectedText())) {
                    return;
                }
                dismiss();
                final int selected = wvBottom.getSelected();
                if (listener != null) {
                    listener.onValueSelected(selected);
                }
                break;
        }
    }

    private ArrayList<String> createData() {
        mDatas = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            mDatas.add(String.format("%d%%", i * 10));
        }
        return mDatas;
    }

    public void setSelected(int index) {
        this.mIndex = index;
    }

    private OnBottomListener listener;

    public void setListener(OnBottomListener listener) {
        this.listener = listener;
    }

    public interface OnBottomListener {
        void onValueSelected(int value);
    }
}
