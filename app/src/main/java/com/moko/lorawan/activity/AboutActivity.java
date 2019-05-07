package com.moko.lorawan.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.moko.lorawan.R;
import com.moko.lorawan.utils.Utils;

import butterknife.Bind;
import butterknife.ButterKnife;

public class AboutActivity extends BaseActivity {

    @Bind(R.id.tv_soft_version)
    TextView tvSoftVersion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        ButterKnife.bind(this);
        tvSoftVersion.setText(getString(R.string.version_info, Utils.getVersionInfo(this)));
    }

    public void openURL(View view) {
        Uri uri = Uri.parse(getString(R.string.about_moko_url));
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }

    public void back(View view) {
        finish();
    }
}
