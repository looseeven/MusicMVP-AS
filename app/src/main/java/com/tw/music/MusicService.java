package com.tw.music;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.os.Binder;
import android.os.FileUtils;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.widget.Toast;

import com.tw.music.lrc.LrcTranscoding;
import com.tw.music.view.MusicWidgetProvider;

public class MusicService extends Service {
	private static final String TAG = "MusicService";

	private final IBinder mBinder = new MusicBinder();

	public class MusicBinder extends Binder {
		public MusicService getService() {
			return MusicService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	private TWMusic mTW = null;

	public MediaPlayer mMediaPlayer;
	private Handler mAHandler = null;

	private static final int NOTIFY_CHANGE = 0xff01;
    private static final int SHOW_PROGRESS = 0xff02;
    private static final int NEXT = 0xff03;
    private static final int PREV = 0xff04;
    private static final int SAVE_MUSICINFO = 0xff05;
	public static final int RETURN_ACCOFF= 0x0202;
    private boolean isACCOFF = false;
    private static final int ACTION_BACK = 0x0304;
    public static boolean mBack = false;

    private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what) {
				case RETURN_ACCOFF:
				if (msg.arg1 == 1 && msg.arg2 == 0) { //正式关机
					isACCOFF = true;
				}
				break;
			case ACTION_BACK:
				mBack =true;
				break;
			case NEXT:
				_next();
				break;
			case PREV:
				_prev();
				break;
			case TWMusic.RETURN_MOUNT: {
				String volume = null;
				switch(msg.arg1) {
				case 1:
					volume = "/storage/" + msg.obj;
					break;
				case 2:
					volume = "/storage/" + msg.obj;
					break;
				case 3:
					volume = "/mnt/sdcard/iNand";
					break;
				}
				if((mTW.mCurrentPath != null) && mTW.mCurrentPath.startsWith(volume)) {
					if(msg.arg2 == 0) {
						mTW.mPlaylistRecord.clearRecord();
						stop();
					} else {
						mTW.loadFile(mTW.mPlaylistRecord, mTW.mCurrentPath);
						mTW.toRPlaylist(mTW.mCurrentIndex);
				    	if(prepare(mTW.mCurrentAPath) == 0) {
				    		seekTo(mTW.mCurrentPos);
				    	}
					}
				}
				if(mAHandler != null) {
					mAHandler.obtainMessage(msg.what, msg.arg1, msg.arg2, msg.obj).sendToTarget();
				}
				break;
			}
			case TWMusic.RETURN_MUSIC:
				switch(msg.arg1) {
				case 1: // pp
					pp();
					break;
				case 2: // save/stop
					save();
					break;
				case 3: // next
					next();
					break;
				case 4: // prev
					prev();
					break;
				case 5: // play
					if(!isPlaying()) {
						start();
					}
					break;
				case 6: // pause
					if(isPlaying()) {
						pause();
					}
					break;
				case 7: // duck
					duck(true);
					break;
				case 8:
					duck(false);
					break;
				case 9: // ffwd
					ffwd();
					break;
				case 10: // rew
					rew();
					break;
				}
				break;
			case SAVE_MUSICINFO:
				if (isPlaying()) {
					save();
				}
				mHandler.removeMessages(SAVE_MUSICINFO);
				mHandler.sendEmptyMessageDelayed(SAVE_MUSICINFO, 3000);
				break;
			case SHOW_PROGRESS:
				if(isPlaying()) {
					int duration = getDuration();
					int position = getCurrentPosition();
					mTW.mCurrentPos = mMediaPlayer.getCurrentPosition();
					if(duration < 0) {
						duration = 0;
					}
					if(position < 0) {
						position = 0;
					}
					int currenttime = position / 1000;
					int scurrenttime = currenttime;
					int mcurrenttime = scurrenttime / 60;
					int hcurrenttime = mcurrenttime / 60;
					scurrenttime %= 60;
					mcurrenttime %= 60;
					hcurrenttime %= 24;
					if((duration > 0) && (position <= duration)) {
						int percent = position * 100 / duration;
						mTW.media(0, mTW.mCurrentIndex + 1, mTW.mPlaylistRecord.mCLength, (hcurrenttime<<16) | (mcurrenttime<<8) | scurrenttime, percent);
						mTW.write(0x9f00, 0x03, (isPlaying() ? 0x80 : 0x00) | (percent & 0x7f));
						mTW.write(0x0303, 0x03, (isPlaying() ? 0x80 : 0x00) | (percent & 0x7f));
					}
					if(mAHandler != null) {
						mAHandler.sendEmptyMessage(SHOW_PROGRESS);
					}
					mWidget.notifyChange(MusicService.this);
					mHandler.removeMessages(SHOW_PROGRESS);
					mHandler.sendEmptyMessageDelayed(SHOW_PROGRESS, 1000);
					Intent intent = new Intent("com.tw.launcher.music_progress_duration");
				    intent.putExtra("msg_music_progress",position);
				    intent.putExtra("msg_music_duration",duration);
				    sendBroadcast(intent);
				}
				break;
			}
		}
    };

    public static final String CMDPREV = "prev";
    public static final String CMDNEXT = "next";
    public static final String CMDPP = "pp";
    public static final String CMDUPDATE = "update";
    public static final String ACTIONCMD = "com.tw.music.action.cmd";
	public static final String ACTIONPREV = "com.tw.music.action.prev";
	public static final String ACTIONNEXT = "com.tw.music.action.next";
	public static final String ACTIONPP = "com.tw.music.action.pp";

	private MusicWidgetProvider mWidget = null;

	private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String cmd = intent.getStringExtra("cmd");
            if (CMDPREV.equals(cmd) || ACTIONPREV.equals(action)) {
            	prev();
			} else if (CMDNEXT.equals(cmd) || ACTIONNEXT.equals(action)) {
				next();
			} else if (CMDPP.equals(cmd) || ACTIONPP.equals(action)) {
				pp();
			} else if (CMDUPDATE.equals(cmd)) {
				int[] appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
				mWidget.performUpdate(MusicService.this, appWidgetIds);
			}
		}
	};

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null) {
            String action = intent.getAction();
            String cmd = intent.getStringExtra("cmd");
            if (CMDPREV.equals(cmd) || ACTIONPREV.equals(action)) {
            	prev();
			} else if (CMDNEXT.equals(cmd) || ACTIONNEXT.equals(action)) {
				next();
			} else if (CMDPP.equals(cmd) || ACTIONPP.equals(action)) {
				pp();
			}
		}
		return START_STICKY;
	}

	int mHintsLengh=7;
	boolean isError=false;
	long[] mHints = new long[mHintsLengh];//初始全部为0
	@Override
	public void onCreate() {
		super.onCreate();
		mTW = TWMusic.open();
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnCompletionListener(new OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				next();
			}
		});
        mMediaPlayer.setOnErrorListener(new OnErrorListener() {
			@Override
			public boolean onError(MediaPlayer mp, int what, int extra) {
				if (mHints[mHints.length - 1]>0) {//得初始化一次
					if (SystemClock.uptimeMillis()-mHints[mHints.length - 1]<=700) {//如果两次间隔时间不超过700毫秒，记录当前这次次数
							//将mHints数组内的所有元素左移一个位置
							System.arraycopy(mHints, 1, mHints, 0, mHints.length - 1);
							//获得当前系统已经启动的时间
							mHints[mHints.length - 1] = SystemClock.uptimeMillis();
							if(SystemClock.uptimeMillis()-mHints[0]<=5000){
							    mHints = new long[mHintsLengh];//初始全部为0
							    isError=true;
							    Toast.makeText(getApplicationContext(),getString(R.string.error), Toast.LENGTH_LONG).show();
							}
					}else {
						mHints = new long[mHintsLengh];//初始全部为0
					}
				}else {
					//获得当前系统已经启动的时间
					mHints[mHints.length - 1] = SystemClock.uptimeMillis();
				}
//				next();
				return true;
			}
		});
    	if(prepare(mTW.mCurrentAPath) == 0) {
    		seekTo(mTW.mCurrentPos);
    	}
    	mTW.addHandler(TAG, mHandler);
        mWidget = MusicWidgetProvider.getInstance();
		IntentFilter commandFilter = new IntentFilter();
		commandFilter.addAction(ACTIONCMD);
		commandFilter.addAction(ACTIONPREV);
		commandFilter.addAction(ACTIONNEXT);
		commandFilter.addAction(ACTIONPP);
		registerReceiver(mIntentReceiver, commandFilter);
		mHandler.sendEmptyMessageDelayed(SAVE_MUSICINFO, 3000);
	}

	@Override
	public void onDestroy() {
		unregisterReceiver(mIntentReceiver);
		mTW.removeHandler(TAG);
		save();
		mHandler.removeMessages(SHOW_PROGRESS);
		mMediaPlayer.release();
		mMediaPlayer = null;
		mTW.requestSource(false);
		mTW.close();
		mTW = null;
		super.onDestroy();
	}

	private int prepare(String path) {
		mMediaPlayer.stop();
		mHandler.removeMessages(SHOW_PROGRESS);
		mMediaPlayer.reset();
        try {
			mMediaPlayer.setDataSource(path);
			mMediaPlayer.prepare();
			return 0;
		} catch (IllegalArgumentException e) {
			return -1;
		} catch (IllegalStateException e) {
			return -2;
		} catch (IOException e) {
			return -3;
		} catch (Exception e) {
			return -4;
		}
	}

	public void start() {
		mTW.requestSource(true);
		mMediaPlayer.start();
		mHandler.removeMessages(SHOW_PROGRESS);
		mHandler.sendEmptyMessage(SHOW_PROGRESS);
		retriever(mTW.mCurrentAPath);
		mTW.mCurrentLrcViewPath = getCurrentLrc(mTW.mCurrentAPath);
		notifyChange();
	}

	public void seekTo(int msec) {
		if(mMediaPlayer.isPlaying()) {
			int duration = mMediaPlayer.getDuration();
			if((msec > 0) && (duration > 0) && (msec < duration)) {
				mMediaPlayer.seekTo(msec);
			}
		}
	}

	public void pause() {
		mTW.mCurrentPos = mMediaPlayer.getCurrentPosition();
		mMediaPlayer.pause();
		mHandler.removeMessages(SHOW_PROGRESS);
		notifyChange();
	}

	public boolean isPlaying() {
		if (mMediaPlayer != null){
			return mMediaPlayer.isPlaying();
		}else {
			return false;
		}
	}

	public void stop() {
		mMediaPlayer.stop();
		mHandler.removeMessages(SHOW_PROGRESS);
		mTW.mCurrentArtist = null;
		mTW.mCurrentAlbum = null;
		mTW.mCurrentSong = null;
		notifyChange();
	}

	private void retriever(String path) {
		mTW.mCurrentLrcViewPath = null;
		mTW.mCurrentArtist = null;
		mTW.mCurrentAlbum = null;
		mTW.mCurrentSong = null;
		if(mTW.mAlbumArt != null) {
			mTW.mAlbumArt = null;
		}
        String localeString = null;
        Locale locale = getResources().getConfiguration().locale;
        if (locale != null) {
            String language = locale.getLanguage();
            String country = locale.getCountry();
            if (language != null) {
                if (country != null) {
                    localeString = language + "_" + country;
                } else {
                    localeString = language;
                }
            }
        }
		MediaMetadataRetriever r = new MediaMetadataRetriever();
		try {
			r.setDataSource(path);
			if(r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) != null) {
				mTW.mCurrentArtist = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
				mTW.mCurrentAlbum = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
				mTW.mCurrentSong = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
				byte albumArt[] = r.getEmbeddedPicture();
				Intent intent = new Intent("com.tw.launcher.musicimage");
				intent.putExtra("msg_Artist", mTW.mCurrentArtist);
				intent.putExtra("msg_Song", mTW.mCurrentSong);
				intent.putExtra("msg_path", path);

				if(albumArt != null) {
					mTW.mAlbumArt = BitmapFactory.decodeByteArray(albumArt, 0, albumArt.length);
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					mTW.mAlbumArt.compress(Bitmap.CompressFormat.JPEG, 100, bos);
					albumArt = bos.toByteArray();
				}else{
					albumArt = null;
				}
				if (albumArt != null && albumArt.length < 1024*1024) {
					intent.putExtra("msg_Albumart", albumArt);
				}else{
					byte albumArt1[] = new byte[] {};
					intent.putExtra("msg_Albumart", albumArt1);
				}
				sendBroadcast(intent);
			}
		} catch (Exception e) {
				Intent intent = new Intent("com.tw.launcher.musicimage");
				byte albumArt[] = new byte[] {};
				intent.putExtra("msg_Albumart", albumArt);
				sendBroadcast(intent);
		}
		r.release();
	}
	private String getCurrentLrc(String path){
		try {
			if (path != null) {
				path = path.substring(0, path.lastIndexOf("."))+".lrc";
				MusicActivity.lrc_view.setLrc(LrcTranscoding.converfile(path));
				MusicActivity.lrc_view.setPlayer(mMediaPlayer);
				MusicActivity.lrc_view.setMode(0);				
				MusicActivity.lrc_view.init();
				return path;
			}
		} catch (Exception e) {
    		e.printStackTrace();
		}
		return path;
	}
    private void rew() {
    	if(mMediaPlayer.isPlaying()) {
        	int pos = mMediaPlayer.getCurrentPosition();
        	pos -= 10000;
        	if(pos > 0 && pos < mMediaPlayer.getDuration()) {
            	mMediaPlayer.seekTo(pos);
        	}
    	}
    }

    private void ffwd() {
    	if(mMediaPlayer.isPlaying()) {
        	int pos = mMediaPlayer.getCurrentPosition();
        	pos += 15000;
        	if(pos > 0 && pos < mMediaPlayer.getDuration()) {
            	mMediaPlayer.seekTo(pos);
        	}
    	}
    }

    private boolean play(int pos) {
    	if(mTW.mCurrentIndex > -1 && mTW.mCurrentIndex < mTW.mPlaylistRecord.mCLength) {
    		mTW.mCurrentAPath = mTW.mPlaylistRecord.mLName[mTW.mCurrentIndex].mPath;
        	if(prepare(mTW.mCurrentAPath) == 0) {
        		start();
        		seekTo(pos);
        		return true;
        	}
    	}
    	return false;
    }

    private void notifyChange() {
    	int percent = 0;
		int duration = getDuration();
		int position = getCurrentPosition();
		if(duration < 0) {
			duration = 0;
		}
		if(position < 0) {
			position = 0;
		}
    	if((duration > 0) && (position <= duration)) {
    		percent = position * 100 / duration;
    	}
        String titleName = getTrackName();
        if(titleName == null) {
        	titleName = getFileName();
        	if(titleName == null) {
        		titleName = getString(R.string.unknown);
        	}
        }
        mTW.write(0x9f00, 0x03, (isPlaying() ? 0x80 : 0x00) | (percent & 0x7f), titleName);
        mTW.write(0x0303, 0x03, (isPlaying() ? 0x80 : 0x00) | (percent & 0x7f), titleName);
		if(mAHandler != null) {
			mAHandler.sendEmptyMessage(NOTIFY_CHANGE);
		}
		mWidget.notifyChange(MusicService.this);
    }

	public void current(int pos, boolean r) {
		synchronized (mTW) {
			int [] p = mTW.mRPlaylist;
			if(p != null) {
				int length = p.length;
				if(length > 0) {
					int index = mTW.mCurrentRIndex;
					if(r) {
						if(index < -1) {
							index = -1;
						}
						int i;
						for(i = index; i > -1; i--) {
							mTW.mCurrentIndex = p[i];
			    			if(play(pos)) {
			    				mTW.mCurrentRIndex = i;
		        				pos = 0;
			    				break;
			    			}
			    			pos = 0;
						}
						if((mTW.mRepeat != 0) && (i == -1)) {
							for(i = length - 1; i > index; i--) {
								mTW.mCurrentIndex = p[i];
			        			if(play(pos)) {
			        				mTW.mCurrentRIndex = i;
			        				pos = 0;
			        				break;
			        			}
			        			pos = 0;
							}
							if(i == index) {
			    				stop();
							}
						}
						if(mTW.mCurrentRIndex == -1) {
							mTW.mCurrentRIndex = 0;
							mTW.mCurrentIndex = p[mTW.mCurrentRIndex];
				    		stop();
						}
					} else {
						if(index > length) {
							index = length;
						}
						int i;
						for(i = index; i < length; i++) {
							mTW.mCurrentIndex = p[i];
			    			if(play(pos)) {
			    				mTW.mCurrentRIndex = i;
		        				pos = 0;
			    				break;
			    			}
			    			pos = 0;
						}
			    		if((mTW.mRepeat != 0) && (i == length)) {
			    			for(i = 0; i < index; i++) {
			    				mTW.mCurrentIndex = p[i];
			        			if(play(pos)) {
			        				mTW.mCurrentRIndex = i;
			        				pos = 0;
			        				break;
			        			}
			        			pos = 0;
			    			}
			    			if(i == index) {
			    				stop();
			    			}
			    		}
						if(mTW.mCurrentRIndex == length) {
							mTW.mCurrentRIndex = length - 1;
							mTW.mCurrentIndex = p[mTW.mCurrentRIndex];
				    		stop();
						}
					}
				}
			}
		}
	}

    private boolean hasMessagesNP() {
    	return (mHandler.hasMessages(NEXT) || mHandler.hasMessages(PREV));
    }

    public boolean getMusicState(){
    	return isPlaying();
    }

    private void _next() {
    	if((mTW.mRPlaylist != null) && (mTW.mRPlaylist.length > 0)) {
    		if(mTW.mRepeat != 2) {
    			mTW.mCurrentRIndex++;
    		}
        	current(0, false);
    	}
    }

    public void next() {
    	if (isError) {
    		isError=false;
			return;
		}
    	if(!hasMessagesNP()) {
    		mHandler.sendEmptyMessage(NEXT);
    	}
    }

    private void _prev() {
    	if((mTW.mRPlaylist != null) && (mTW.mRPlaylist.length > 0)) {
    		if(mTW.mRepeat != 2) {
    			mTW.mCurrentRIndex--;
    		}
        	current(0, true);
    	}
    }

    public void prev() {
    	if(!hasMessagesNP()) {
    		mHandler.sendEmptyMessage(PREV);
    	}
    }

    private void save() {
		if (isACCOFF) {
			mTW.mCurrentPos = mMediaPlayer.getCurrentPosition()-4000;
		}else{
			mTW.mCurrentPos = mMediaPlayer.getCurrentPosition();
		}
		try {
			BufferedWriter bw = null;
			try {
				bw = new BufferedWriter(new FileWriter("/data/tw/music"));
				bw.write(mTW.mCurrentAPath);
				bw.write('\n');
				bw.write(Integer.toString(mTW.mCurrentIndex));
				bw.write('\n');
				bw.write(Integer.toString(mTW.mCurrentPos));
				bw.write('\n');
				bw.write(Integer.toString(mTW.mShuffle));
				bw.write('\n');
				bw.write(Integer.toString(mTW.mRepeat));
				bw.write('\n');
				bw.flush();
			} catch (Exception e) {
				new File("/data/tw/music").delete();
			} finally {
				if(bw != null) {
					bw.close();
					bw = null;
				}
			}
			FileUtils.setPermissions("/data/tw/music", 0666, -1, -1);
		} catch (Exception e) {
		}
    }

    public void pp() {
		if(isPlaying()) {
			pause();
		} else {
			start();
		}
	}

    public void duck(boolean is) {
    	if(is) {
        	mMediaPlayer.setVolume(0.5f, 0.5f);
    	} else {
        	mMediaPlayer.setVolume(1.0f, 1.0f);
    	}
    }

    public void setAHandler(Handler handler) {
    	mAHandler = handler;
    }

    public int getDuration() {
    	return mMediaPlayer.getDuration();
    }

    public int getCurrentPosition() {
    	return mMediaPlayer.getCurrentPosition();
    }

    public String getArtistName() {
    	return mTW.mCurrentArtist;
    }

    public String getAlbumName() {
    	return mTW.mCurrentAlbum;
    }

    public String getTrackName() {
    	return mTW.mCurrentSong;
    }

    public Bitmap getAlbumArt() {
    	return mTW.mAlbumArt;
    }

    public String getFileName() {
    	if (mTW.mCurrentIndex < mTW.mPlaylistRecord.mCLength) {
    		return mTW.mPlaylistRecord.mLName[mTW.mCurrentIndex].mName;
    	}
    	return null;
    }

    public MediaPlayer getPlayer() {
		return mMediaPlayer;
	}
}
