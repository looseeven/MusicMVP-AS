package com.tw.music.view;

import java.util.ArrayList;
import java.util.List;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.tw.music.MusicActivity;
import com.tw.music.R;
import com.tw.music.activity.BaseActivity;

public class PermissionActivity extends BaseActivity {
    private static final String TAG = "PermissionActivity";
    private String[] permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static final int MY_PERMISSIONS_REQUEST = 1;
    // 声明一个集合，在后面的代码中用来存储用户拒绝授权的权
    List<String> mPermissionList = new ArrayList<>();

    @Override
    public void initView() {
        setContentView(R.layout.permission_act);
        mPermissionList.clear();
        for (int i = 0; i < permissions.length; i++) {
            if (ContextCompat.checkSelfPermission(this, permissions[i]) != PackageManager.PERMISSION_GRANTED) {
                mPermissionList.add(permissions[i]);
            }
        }
        String[] permissions = mPermissionList.toArray(new String[mPermissionList.size()]);//将List转为数组
        ActivityCompat.requestPermissions(this, permissions, MY_PERMISSIONS_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    boolean showRequestPermission = ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[i]);
                    if (showRequestPermission) {
                        Log.i(TAG, i + "  未同意，但未勾选不再询问");
                    } else {
                        Log.i(TAG, i + "  未同意，并且勾选不再询问,调整至应用程序界面");
                        Uri packageURI = Uri.parse("package:" + "com.tw.music");
                        Intent intent =  new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,packageURI);
                        startActivity(intent);
                    }
                    finish();
                } else {
                    Log.i(TAG, i + "  同意權限請求");
                    startActivity(new Intent(this, MusicActivity.class));
                    finish();
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    @Override
    public void initData() {

    }

    @Override
    public void ondestroy() {

    }

    @Override
    public void onresume() {

    }

    @Override
    public void onpause() {

    }
}
