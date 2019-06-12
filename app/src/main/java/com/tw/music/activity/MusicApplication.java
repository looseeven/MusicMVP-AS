package com.tw.music.activity;

import com.tw.music.MusicService;

import android.app.Application;
import android.content.Intent;

public class MusicApplication extends Application{
	@Override
    public void onCreate() {
          startService(new Intent(this, MusicService.class));
    }

}
