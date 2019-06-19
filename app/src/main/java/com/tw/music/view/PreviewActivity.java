package com.tw.music.view;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import com.tw.music.R;
import com.tw.music.activity.BaseActivity;
import com.tw.music.contarct.Contarct;
import com.tw.music.contarct.Contarct.prePresenter;
import com.tw.music.lrc.LrcView;
import com.tw.music.presenter.PreviewPresenter;

public class PreviewActivity extends BaseActivity implements Contarct.View{
	private static final String TAG = "PreviewActivity";

	public static Uri mUri;
	private prePresenter mPresenter;
	private TextView mTitle; //歌曲
	private TextView mArtist; //专辑
	private SeekBar mSeekBar; //播放进度条
	private TextView mLoadingText; //提示信息
	private boolean isPlayPause = false;
	public static LrcView lrc_view; //歌词
	
	@Override
	public void initView() {
		Intent intent = getIntent();
		if (intent == null) {
			finish();
			return;
		}
		mUri = intent.getData();
		if (mUri == null) {
			finish();
			return;
		}
		new PreviewPresenter(this);
    	setVolumeControlStream(AudioManager.STREAM_MUSIC);
		mPresenter.onstart(PreviewActivity.this);
		setContentView(R.layout.music_preview_act);
		mTitle = (TextView) findViewById(R.id.line1);
		mArtist = (TextView) findViewById(R.id.line2);
		mSeekBar = (SeekBar) findViewById(R.id.progress);
		mSeekBar.setOnSeekBarChangeListener(mSeekListener);
		mLoadingText = (TextView) findViewById(R.id.loading);
		lrc_view = (LrcView) findViewById(R.id.lrc_view);
	}

	@Override
	public void initData() {
		if (mUri.getScheme().equals("http")) {
			String msg = getString(R.string.streamloadingtext, mUri.getHost());
			mLoadingText.setText(msg);
		} else {
			mLoadingText.setVisibility(View.GONE);
		}
		mPresenter.setUri(mUri);
	}

	public void onClick(View v){
		mPresenter.setPlayPlause();
		if (isPlayPause) {
			((ImageButton)findViewById(R.id.playpause)).setImageResource(R.mipmap.btn_playback_ic_play_small);
		} else {
			((ImageButton)findViewById(R.id.playpause)).setImageResource(R.mipmap.btn_playback_ic_pause_small);
		}
	}

	private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
		public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
			if (!fromuser) {
				return;
			}
			mPresenter.setSeekBar(progress);
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
		}
	};


	@Override
	public void showError() {
		Toast.makeText(this, R.string.playback_failed, Toast.LENGTH_SHORT).show();
		finish();
	}

	@Override
	public void onPrepared() {
		mSeekBar.setVisibility(View.VISIBLE);
	}
	@Override
	public void showID3(String title, String artist, String album) {
		mTitle.setText(title);
		mArtist.setText(artist);
	}

	@Override
	public void showSeekBar(int totaltime, int currenttime) {
		mSeekBar.setMax(totaltime);
		mSeekBar.setProgress(currenttime);
	}

	@Override
	public void showPlaypause(Boolean playpause) {
		isPlayPause = playpause;
		if (playpause) {
			((ImageButton)findViewById(R.id.playpause)).setImageResource(R.mipmap.btn_playback_ic_pause_small);
		} else {
			((ImageButton)findViewById(R.id.playpause)).setImageResource(R.mipmap.btn_playback_ic_play_small);
		}
	}

	@Override
	public void ondestroy() {
		mPresenter.ondestroy();
	}

	@Override
	public void setPresenter(prePresenter presenter) {
		this.mPresenter = presenter;
	}


	@Override
	public void onresume() {
		
	}

	@Override
	public void onpause() {
		
	}
}
