package com.tw.music.activity;


import com.tw.music.MusicService;

import android.Manifest;
import android.app.Application;
import android.content.Intent;

public class MusicApplication extends Application{
	private String[] permissions = {Manifest.permission.RECORD_AUDIO,Manifest.permission.WRITE_EXTERNAL_STORAGE};

	@Override
    public void onCreate() {
//          startService(new Intent(this, MusicService.class));
  		//				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
  		//					int i0 = ContextCompat.checkSelfPermission(mContext,permissions[0]);
  		//					int i1 = ContextCompat.checkSelfPermission(mContext,permissions[1]);
  		//					if(i1 != PackageManager.PERMISSION_GRANTED || i0 != PackageManager.PERMISSION_GRANTED){
  		//						startPermission();
  		//					}
  		//					if(i1 == PackageManager.PERMISSION_GRANTED){
  		//						setVisualizerFxAndUi();
  		//					}
  		//				}
      	// 开始提交请求权限
      	//	private void startPermission() {
      	//		ActivityCompat.requestPermissions((Activity) mContext, permissions, 321);
      	//		setVisualizerFxAndUi();
      	//	}
    }
}
