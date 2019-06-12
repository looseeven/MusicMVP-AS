package com.tw.music.activity;

import android.app.Activity;
import android.os.Bundle;
/*
 * @author xy by 20190611
 *	Activity abstract class
 */
public abstract class BaseActivity extends Activity{

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initView();
		initData();
	}

	public abstract void initView() ;
	public abstract void initData() ;
	public abstract void ondestroy() ;
	public abstract void onresume() ;
	public abstract void onpause() ;
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		ondestroy();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		onresume();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		onpause();
	}
}
