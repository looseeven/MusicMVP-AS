package com.tw.music.presenter;

import java.io.IOException;

import com.tw.music.MusicActivity;
import com.tw.music.contarct.Contarct;
import com.tw.music.utils.lrc.LrcTranscoding;

import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;


public class PreviewPresenter implements Contarct.prePresenter,OnPreparedListener, OnErrorListener{
    private Contarct.View PreView;
	private PreviewPlayer mPlayer;
	private static final String TAG = "PreviewPresenter";
	public static String scheme;
	private AudioManager mAudioManager;
	private long mMediaId = -1;
	public static String title,artist,album;
	private Handler mProgressRefresher;
	private int mDuration;
	static Context mContext;
	private boolean isOnDestroy = false;
	
    public PreviewPresenter(Contarct.View View) {
    	PreView = View;
    	PreView.setPresenter(this);
    }

    @Override
    public void onstart(Context c) {
    	isOnDestroy = true;
    	mContext = c;
    }
    
    @Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
    	PreView.showError();
		return false;
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		if (!isOnDestroy) return;
		mPlayer = (PreviewPlayer) mp;
		mPlayer.start();
		PreView.onPrepared();
	}

	@Override
	public void setUri(Uri mUri) {
    	mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
		PreviewPlayer player = (PreviewPlayer)((Activity) mContext).getLastNonConfigurationInstance();
		mProgressRefresher = new Handler();
		scheme = mUri.getScheme();
		if (player == null) {
			mPlayer = new PreviewPlayer();
			mPlayer.setActivity(this);
			try {
				mPlayer.setDataSourceAndPrepare(mUri);
				Log.i("md","mUri: "+mUri);
				mProgressRefresher.postDelayed(new ProgressRefresher(), 1000);
			} catch (Exception ex) {
				// catch generic Exception, since we may be called with a media
				// content URI, another content provider's URI, a file URI,
				// an http URI, and there are different exceptions associated
				// with failure to open each of those.
				Log.d(TAG, "Failed to open file: " + ex);
				PreView.showError();
				return;
			}
		} else {
			mPlayer = player;
			mPlayer.setActivity(this);
			if (mPlayer.isPrepared()) {
				showPostPrepareUI();
			}
		}
		
		AsyncQueryHandler mAsyncQueryHandler = new AsyncQueryHandler(mContext.getContentResolver()) {
			@Override
			protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
				if (cursor != null && cursor.moveToFirst()) {
					int titleIdx = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
					int artistIdx = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
					int albumIdx = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
					int idIdx = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
					int displaynameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
					
					if (idIdx >=0) {
						mMediaId = cursor.getLong(idIdx);
					}
					if (titleIdx >= 0) {
						title = cursor.getString(titleIdx);
						if (artistIdx >= 0) {
							artist = cursor.getString(artistIdx);
							if (albumIdx >= 0) {
								album = cursor.getString(albumIdx);
							}
						}
					} else if (displaynameIdx >= 0) {
						title = cursor.getString(displaynameIdx);
					} else {
						Log.w(TAG, "Cursor had no names for us");
					}
				} else {
					Log.w(TAG, "empty cursor");
				}
				PreView.showID3(title, artist, album);
				if (cursor != null) {
					cursor.close();
				}
			}
		};
		if (scheme.equals(ContentResolver.SCHEME_CONTENT)) {
			if (mUri.getAuthority() == MediaStore.AUTHORITY) {
				// try to get title and artist from the media content provider
				mAsyncQueryHandler.startQuery(0, null, mUri, new String [] {
						MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST},
						null, null, null);
			} else {
				// Try to get the display name from another content provider.
				// Don't specifically ask for the display name though, since the
				// provider might not actually support that column.
				mAsyncQueryHandler.startQuery(0, null, mUri, null, null, null, null);
			}
		} else if (scheme.equals("file")) {
			// check if this file is in the media database (clicking on a download
			// in the download manager might follow this path
			String path = mUri.getPath();
			getCurrentLrc(path);
			mAsyncQueryHandler.startQuery(0, null,  MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
					new String [] {MediaStore.Audio.Media._ID,
					MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST},
					MediaStore.Audio.Media.DATA + "=?", new String [] {path}, null);
		} else {
			// We can't get metadata from the file/stream itself yet, because
			// that API is hidden, so instead we display the URI being played
		}
	}
	private String getCurrentLrc(String path){
		try {
			if (path != null) {
				path = path.substring(0, path.lastIndexOf("."))+".lrc";
				MusicActivity.lrc_view.setLrc(LrcTranscoding.converfile(path));
				MusicActivity.lrc_view.setPlayer(mPlayer);
				MusicActivity.lrc_view.setMode(0);
				MusicActivity.lrc_view.init();
				return path;
			}
		} catch (Exception e) {
			e.printStackTrace();
			Log.i("md","e  "+e.toString());
		}
		return path;
	}
	@Override
	public void setSeekBar(int progress) {
		mPlayer.seekTo(progress);
	}

	@Override
	public void setPlayPlause() {
		if (mPlayer.isPlaying()) {
			mPlayer.pause();
		} else {
			mPlayer.start();
		}
	}

	private void stopPlayback() {
		if (mProgressRefresher != null) {
			mProgressRefresher.removeCallbacksAndMessages(null);
		}
		if (mPlayer != null) {
			mPlayer.release();
			mPlayer = null;
			mAudioManager.abandonAudioFocus(mAudioFocusListener);
		}
	}


	private void showPostPrepareUI() {
		mDuration = mPlayer.getDuration();
		PreView.showSeekBar(mDuration, mPlayer.getCurrentPosition());
		mAudioManager.requestAudioFocus(mAudioFocusListener, AudioManager.STREAM_MUSIC,
				AudioManager.AUDIOFOCUS_GAIN);
		mProgressRefresher.postDelayed(new ProgressRefresher(), 1000);
	}

	private boolean mPausedByTransientLossOfFocus;
	private OnAudioFocusChangeListener mAudioFocusListener = new OnAudioFocusChangeListener() {
		public void onAudioFocusChange(int focusChange) {
			if (mPlayer == null) {
				// this activity has handed its MediaPlayer off to the next activity
				// (e.g. portrait/landscape switch) and should abandon its focus
				mAudioManager.abandonAudioFocus(this); //注销掉当前的音频焦点
				return;
			}
			switch (focusChange) {
			case AudioManager.AUDIOFOCUS_LOSS://失去焦点很长时间
				mPausedByTransientLossOfFocus = false;
				mPlayer.stop();
				mPlayer.release();
				mPlayer=null;
				break;
			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT://暂时失去焦点
				if (mPlayer.isPlaying()) {
					mPausedByTransientLossOfFocus = true;
					mPlayer.pause();
				}
				break;
			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK://你暂时失去了音频焦点，但你可以小声地继续播放音频（低音量）而不是完全扼杀音频。
				if (mPlayer.isPlaying())
					mPlayer.setVolume(1.0f, 1.0f);
				break;
			case AudioManager.AUDIOFOCUS_GAIN://得到焦点
				if (mPausedByTransientLossOfFocus) {
					mPausedByTransientLossOfFocus = false;
					start();
					mPlayer.setVolume(1.0f, 1.0f);
				}
				break;
			}
		}
	};

	private void start() {
		mAudioManager.requestAudioFocus(mAudioFocusListener, AudioManager.STREAM_MUSIC,
				AudioManager.AUDIOFOCUS_GAIN);
		mPlayer.start();
		mProgressRefresher.postDelayed(new ProgressRefresher(), 1000);
	}

	class ProgressRefresher implements Runnable {
		public void run() {
			mDuration = mPlayer.getDuration();
			if (mPlayer != null &&  mDuration != 0) {
				PreView.showSeekBar(mDuration, mPlayer.getCurrentPosition());
			}
			PreView.showPlaypause(mPlayer.isPlaying());
			mProgressRefresher.removeCallbacksAndMessages(null);
			mProgressRefresher.postDelayed(new ProgressRefresher(), 1000);
		}
	}
	
	/*
	 * Wrapper class to help with handing off the MediaPlayer to the next instance
	 * of the activity in case of orientation change, without losing any state.
	 */
	 public static class PreviewPlayer extends MediaPlayer implements OnPreparedListener {
		PreviewPresenter mActivity;
		boolean mIsPrepared = false;

		public void setActivity(PreviewPresenter activity) {
			mActivity = activity;
			setOnPreparedListener(this);
		}

		public void setDataSourceAndPrepare(Uri uri) throws IllegalArgumentException,
		SecurityException, IllegalStateException, IOException {
			setDataSource(mContext,uri);
			prepareAsync();
		}

		/* (non-Javadoc)
		 * @see android.media.MediaPlayer.OnPreparedListener#onPrepared(android.media.MediaPlayer)
		 */
		@Override
		public void onPrepared(MediaPlayer mp) {
			mIsPrepared = true;
			mActivity.onPrepared(mp);
		}
		
		boolean isPrepared() {
			return mIsPrepared;
		}
	 }

	@Override
	public void ondestroy() {
		isOnDestroy = false;
		stopPlayback();
	}

	@Override
	public void onpause() {
		
	}

	@Override
	public void onresume() {
		
	}
}
