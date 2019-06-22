package com.tw.music.presenter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.media.audiofx.Visualizer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import com.tw.music.MusicService;
import com.tw.music.R;
import com.tw.music.TWMusic;
import com.tw.music.adapter.MyListAdapter;
import com.tw.music.bean.MusicName;
import com.tw.music.bean.Record;
import com.tw.music.contarct.Contarct;
import com.tw.music.utils.CollectionUtils;
import com.tw.music.utils.SharedPreferencesUtils;
import com.tw.music.visualizer.BaseVisualizerView;

/**
 * @author xy by 20190612
 * The logical layer of the music master module
 */
public class MusicPresenter implements Contarct.mainPresenter{
	private static final String TAG = "MusicPresenter";
	public static Contarct.mainView mainView;
	public static TWMusic mTW = null;
	Context mContext; 
	int wallpoition =0;//壁纸
	private MusicService mService = null; 
	private final String ACTION_UPDATE_ALL = "com.gss.widget.UPDATE_ALL";	
	private BaseVisualizerView mBaseVisualizerView;
	private Visualizer mVisualizer;
	private boolean showFreqView=true; //由于控制显示歌词/频谱
	private static MyListAdapter mAdapter;
	public static boolean isCollectMusic = false; //用于判断是否在收藏列表
	public final int NOTIFY_CHANGE = 0xff01;
	public final int SHOW_PROGRESS = 0xff02;

	public MusicPresenter(Contarct.mainView view) {
		mainView = view;
		mainView.setPresenter(this);
	}
	
	@Override
	public void onstart(final Context mContext) {
		this.mContext = mContext;
		mTW = TWMusic.open();
		fullScreen((Activity) mContext);
		mAdapter = new MyListAdapter(mContext,mTW);
		mTW.requestService(TWMusic.ACTIVITY_RUSEME);
		mContext.bindService(new Intent(mContext, MusicService.class), mConnection, Context.BIND_AUTO_CREATE);
		setVisualizerFxAndUi();
		getCollListRecord();
		if (mTW!=null) {
			mainView.showRepeat(mTW.mRepeat, 0);
		}
	}


	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what) {
			case TWMusic.RETURN_MOUNT: {
				mAdapter.notifyDataSetChanged();
				break;
			}
			case NOTIFY_CHANGE:
				if(mService != null) {
					showMusicInfo();
				}
				break;
			case SHOW_PROGRESS:
				if(mService != null) {
					int duration = mService.getDuration();
					int position = mService.getCurrentPosition();
					if(duration < 0) {
						duration = 0;
					}
					if(position < 0) {
						position = 0;
					}
					if(position > duration){
						return;
					} else {
						mainView.showSeekBar(duration, position);
					}
				}
				break;
			}
		}
	};

	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
			mService = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mService = ((MusicService.MusicBinder)service).getService();
			mService.setAHandler(mHandler);
			mHandler.sendEmptyMessage(NOTIFY_CHANGE);
		}
	};

	private void setupVisualizerFxAndUi() {
		try {
			if(mVisualizer != null){
				mVisualizer = null;
			}
			mBaseVisualizerView = new BaseVisualizerView(mContext);
			mBaseVisualizerView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
			mVisualizer = new Visualizer(mService.getPlayer().getAudioSessionId());
			mVisualizer.setEnabled(false);
			mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
			//mVisualizer.getCaptureSize();//频谱段位
			mBaseVisualizerView.setVisualizer(mVisualizer);
			mVisualizer.setEnabled(true);
			mainView.showVisualizerView(mBaseVisualizerView);
		}catch (Exception e){
			e.printStackTrace();
		}
	}

	@Override
	public void ondestroy() {
		mTW.requestSource(false);
		if(mService != null) {
			mService.setAHandler(null);
		}
		try { 
			mContext.unbindService(mConnection);
		} catch (Exception e) {
		}
		mTW.close();
		mTW = null;
		mHandler.removeCallbacksAndMessages(null);
		mVisualizer = null;
		mBaseVisualizerView = null;
	}

	@Override
	public void onpause() {
		mTW.requestService(TWMusic.ACTIVITY_PAUSE);
	}

	@Override
	public void onresume() {
		mTW.requestService(TWMusic.ACTIVITY_RUSEME);
		showFreqView = SharedPreferencesUtils.getBooleanPref(mContext, "music","showFreqView");
		wallpoition = SharedPreferencesUtils.getIntPref(mContext, "id", "id");
		mainView.showWallPaper(wallpoition);
		mainView.showLrcorVis(showFreqView);
		mainView.showListDrawer(mTW.mCList.mIndex);
		Log.i("md","mTW.mCList.mIndex:  "+mTW.mCList.mIndex);
		mainView.updateAdapterData(mAdapter);
	}

	@Override
	public void setSeekBar(int progress) {
		seekTo(progress);
	}

	private void seekTo(int msec) {
		if(mService != null) {
			mService.seekTo(msec);
		}
	}

	@Override
	public void setPlayPlause() {
		if(mService != null) {
			if(mService.isPlaying()) {
				mService.pause();
			} else {
				mService.start();
			}
			mService.isPause = true;
		}
	}

	private void fullScreen(Activity activity) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				//5.x开始需要把颜色设置透明，否则导航栏会呈现系统默认的浅灰色
				Window window = activity.getWindow();
				View decorView = window.getDecorView();
				//两个 flag 要结合使用，表示让应用的主体内容占用系统状态栏的空间
				int option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
						| View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
				decorView.setSystemUiVisibility(option);
				window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
				window.setStatusBarColor(Color.TRANSPARENT);
				//导航栏颜色也可以正常设置
				//	                window.setNavigationBarColor(Color.TRANSPARENT);
			} else {
				Window window = activity.getWindow();
				WindowManager.LayoutParams attributes = window.getAttributes();
				int flagTranslucentStatus = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
				int flagTranslucentNavigation = WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION;
				attributes.flags |= flagTranslucentStatus;
				attributes.flags |= flagTranslucentNavigation;
				window.setAttributes(attributes);
			}
		}
	}

	@Override
	public void setChangeWall() {
		if(wallpoition>6){
			wallpoition=0;
		}else{
			wallpoition+=1;
		}
		SharedPreferencesUtils.setIntPref(mContext, "id", "id", wallpoition);
		mainView.showWallPaper(wallpoition);
	}

	@Override
	public void openEQ() {
		try {
			Intent it = new Intent();
			it.setClassName("com.tw.eq", "com.tw.eq.EQActivity");
			it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			mContext.startActivity(it);
		} catch (Exception e) {
			Log.e(TAG, Log.getStackTraceString(e));
		}
	}

	@Override
	public void openHome() {
		try {
			Intent it = new Intent(Intent.ACTION_MAIN);
			it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			it.addCategory(Intent.CATEGORY_HOME);
			mContext.startActivity(it);
		} catch (Exception e) {
			Log.e(TAG, Log.getStackTraceString(e));
		}
	}

	@Override
	public void setChangeLrcorVis() {
		showFreqView=!showFreqView;
		mainView.showLrcorVis(showFreqView);
		SharedPreferencesUtils.setBooleanPref(mContext, "music","showFreqView", showFreqView);
	}

	@Override
	public void setPrev() {
		prev();
	}

	@Override
	public void setNext() {
		next();
	}

	@Override
	public void setRepeat() {
		int repeatImage = mTW.mRepeat;
		repeatImage++;
		if (repeatImage>3) {
			repeatImage=0;
			mTW.mRepeat=1;
			mTW.mShuffle = 0;
		}else {
			if (repeatImage==1) {
				mTW.mRepeat = 1;
				mTW.mShuffle = 0;
			}else if (repeatImage==2){
				mTW.mShuffle = 0;
				mTW.mRepeat=2;
			}else {
				mTW.mShuffle = 1;
				mTW.mRepeat=3;
			}
		}
		mainView.showRepeat(mTW.mRepeat, mTW.mShuffle);
		mTW.toRPlaylist(mTW.mCurrentIndex);
	}

	@Override
	public void setCollect() {
		if(!TextUtils.isEmpty(mTW.mCurrentAPath)){ //判断路径是否为空
			if (CollectionUtils.itBeenCollected(mContext, mTW.mCurrentAPath, mTW.likeMusic)) {
				if(((ImageView) mAdapter.ivLove) != null){
					((ImageView) mAdapter.ivLove).getDrawable().setLevel(0);
				}
				mainView.showCollect(false);
				CollectionUtils.removeMusicFromCollectionList(mTW.mCurrentAPath,mTW.likeMusic);
			} else {
				if(((ImageView) mAdapter.ivLove) != null){
					((ImageView) mAdapter.ivLove).getDrawable().setLevel(1);
				}
				mainView.showCollect(true);
				if(!CollectionUtils.itBeenCollected(mContext,  mTW.mCurrentAPath, mTW.likeMusic)){
					CollectionUtils.addMusicToCollectionList(new MusicName(mService.getFileName(), mTW.mCurrentAPath), mTW.likeMusic);
				}
			}
			getCollListRecord();
			mAdapter.notifyDataSetChanged();
			CollectionUtils.saveCollectionMusicList(mContext,mTW.likeMusic);
		}
	}


	int onclick = -1;
	public boolean isContinuousClick(int i){
		if (onclick == i) {
			return true;
		}else{
			onclick = i;
			return false;
		}
	}

	@Override
	public void openPlayList() {
		isContinuousClick(0);
		mainView.showListDrawer(0);
		mTW.mCList = mTW.mPlaylistRecord;
		mTW.mCList.mIndex = 0;
		mAdapter.notifyDataSetChanged();
		isCollectMusic = false;
	}

	@Override
	public void openSDList() {
		mainView.showListDrawer(1);
		if (isContinuousClick(1)) {
			if(mTW.mSDRecordArrayList.size() > 0) {
				if(++mTW.mSDRecordLevel >= mTW.mSDRecordArrayList.size()) {
					mTW.mSDRecordLevel = 0;
				}
				mTW.mCList = mTW.mSDRecordArrayList.get(mTW.mSDRecordLevel);
			} else {
				mTW.mCList = mTW.mSDRecord;
			}
		}else{
			if(mTW.mSDRecordArrayList.size() > 0) {
				if(mTW.mSDRecordLevel >= mTW.mSDRecordArrayList.size()) {
					mTW.mSDRecordLevel = 0;
				}
				mTW.mCList = mTW.mSDRecordArrayList.get(mTW.mSDRecordLevel);
			} else {
				mTW.mCList = mTW.mSDRecord;
			}
		}
		mAdapter.notifyDataSetChanged();
		isCollectMusic = false;
	}
	@Override
	public void openUSBList() {
		mainView.showListDrawer(2);
		if (isContinuousClick(2)) {
			if(mTW.mUSBRecordArrayList.size() > 0) {
				if(++mTW.mUSBRecordLevel >= mTW.mUSBRecordArrayList.size()) {
					mTW.mUSBRecordLevel = 0;
				}
				mTW.mCList = mTW.mUSBRecordArrayList.get(mTW.mUSBRecordLevel);
			} else {
				mTW.mCList = mTW.mUSBRecord;
			}
		}else{
			if(mTW.mUSBRecordArrayList.size() > 0) {
				if(mTW.mUSBRecordLevel >= mTW.mUSBRecordArrayList.size()) {
					mTW.mUSBRecordLevel = 0;
				}
				mTW.mCList = mTW.mUSBRecordArrayList.get(mTW.mUSBRecordLevel);
			} else {
				mTW.mCList = mTW.mUSBRecord;
			}
		}
		mAdapter.notifyDataSetChanged();
		isCollectMusic = false;
	}
	@Override
	public void openiNandList() {
		isContinuousClick(3);
		mainView.showListDrawer(3);
		mTW.mCList = mTW.mMediaRecord;
		mTW.mCList.mIndex = 3;
		mAdapter.notifyDataSetChanged();
		isCollectMusic = false;
	}

	@Override
	public void openCollectList() {
		isContinuousClick(4);
		mainView.showListDrawer(4);
		MusicName[] mLName = new MusicName[mTW.likeMusic.size()];
		for(int i = 0; i < mTW.likeMusic.size();i++){
			mLName[i] = new MusicName(mTW.likeMusic.get(i).mName,mTW.likeMusic.get(i).mPath);
		}
		mTW.mLikeRecord.mLName = mLName;
		mTW.mLikeRecord.mLength = mTW.likeMusic.size();
		mTW.mCList = mTW.mLikeRecord;
		mTW.mCList.mIndex = 4;
		mAdapter.notifyDataSetChanged();
		isCollectMusic = true;
	}

	@Override
	public void setListitemlistener(int position) {
		try {
			if(isCollectMusic){
				mTW.mCurrentAPath = mTW.likeMusic.get(position).mPath;
				String path = mTW.mCurrentAPath.substring(0, mTW.mCurrentAPath.lastIndexOf("/"));
				mTW.mCurrentPath = path;
				mTW.mPlaylistRecord.copyLName(mTW.mLikeRecord);
				mTW.toRPlaylist(position);
				current(0, false);
				mService.mMediaPlayer.setOnErrorListener(new OnErrorListener() {
					@Override
					public boolean onError(MediaPlayer mp, int what, int extra) {
						try {
							AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
							builder.setMessage(R.string.noplay);
							builder.setCancelable(true);
							final AlertDialog dlg = builder.create();
							dlg.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
							dlg.show();
							mHandler.postDelayed(new Runnable() {
								public void run() {
									dlg.dismiss();
									next();
								}
							}, 2000);
						} catch (Exception e) {
						}
						return true;
					}
				});
			}else {
				if((mTW.mCList.mLevel != 0) && (position == 0)){
					mTW.mCList = mTW.mCList.mPrev;
				} else {
					if(mTW.mCList.mLevel != 0) {
						position--;
					}
					if((mTW.mCList.mLevel == 0) && (mTW.mCList.mIndex != 0)) {
						Record r = mTW.mCList.getNext(position);
						if(r == null) {
							r = new Record(mTW.mCList.mLName[position].mName, position, mTW.mCList.mLevel + 1, mTW.mCList);
							mTW.loadFile(r, mTW.mCList.mLName[position].mPath);
						}
						mTW.mCList.setNext(r);
						mTW.mCList = r;
					} else {
						mTW.mCurrentIndex = position;
						mTW.mCurrentAPath = mTW.mCList.mLName[position].mPath;
						String path = mTW.mCurrentAPath.substring(0, mTW.mCurrentAPath.lastIndexOf("/"));
						if(path != null) {
							if(mTW.mCList.mLevel == 1) {
								mTW.mPlaylistRecord.copyLName(mTW.mCList);
							}
						}
						mTW.toRPlaylist(position);
						mTW.mCurrentPath = path;
						current(0, false);
						mService.mMediaPlayer.setOnErrorListener(new OnErrorListener() {
							@Override
							public boolean onError(MediaPlayer mp, int what, int extra) {
								try {
									AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
									builder.setMessage(R.string.noplay);
									builder.setCancelable(true);
									final AlertDialog dlg = builder.create();
									dlg.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
									dlg.show();
									mHandler.postDelayed(new Runnable() {
										public void run() {
											dlg.dismiss();
										}
									}, 2000);

								} catch (Exception e) {
								}
								return true;
							}
						});
					}
				}
			}
			mAdapter.notifyDataSetChanged();
		} catch (Exception e) {
			Log.i(TAG, ""+e.toString());
		}
	}

	@Override
	public void setVisualizerFxAndUi() {
		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				setupVisualizerFxAndUi();
			}
		},1000);
	}

	private void current(int pos, boolean r) {
		if(mService != null) {
			mService.current(pos, r);
		}
	}
	private void next() {
		if(mService != null) {
			mService.next();
		}
	}
	private void prev() {
		if(mService != null) {
			mService.prev();
		}
	}

	private void showMusicInfo() {
		int MusicType = mService.isPlaying() ? 1 : 0;
		Intent intent = new Intent();
		intent.setAction(ACTION_UPDATE_ALL);
		intent.putExtra("musictype", MusicType);//int 数据  1 播放， 0 暂停
		mContext.sendBroadcast(intent);
		CharSequence artistName = mService.getArtistName();
		CharSequence albumName = mService.getAlbumName();
		CharSequence titleName = mService.getTrackName();
		if(artistName == null) {
			artistName = mContext.getString(R.string.unknown);
		}
		if(albumName == null) {
			albumName = mContext.getString(R.string.unknown);
		}
		if(titleName == null) {
			titleName = mService.getFileName();
			if(titleName == null) {
				titleName = mContext.getString(R.string.unknown);
			}
		}
		mAdapter.notifyDataSetChanged();
		updateCollectButtonState();
		mainView.showSmoothScrollToPosition(mTW.mCurrentIndex + mTW.mCList.mLevel);
		mainView.showPlaypause(mService.isPlaying(),mService.isPause);
		mainView.showID3((String)titleName, (String)artistName, (String)albumName);
		mainView.showAlbumArt(mService.getAlbumArt());
		CollectionUtils.getCollectionMusicList(mContext,mTW.likeMusic);
	}

	private void updateCollectButtonState(){
		if (mTW.mCurrentAPath != null) {
			if (CollectionUtils.itBeenCollected(mContext, mTW.mCurrentAPath, mTW.likeMusic)) {
				mainView.showCollect(true);
			} else {
				mainView.showCollect(false);
			}
		}
	}
	/**
	 * 获取收藏列表目录
	 */
	private void getCollListRecord(){
		CollectionUtils.getCollectionMusicList(mContext,mTW.likeMusic);
		MusicName[] mLName = new MusicName[mTW.likeMusic.size()];
		for(int i = 0; i < mTW.likeMusic.size();i++){
			mLName[i] = new MusicName(mTW.likeMusic.get(i).mName,mTW.likeMusic.get(i).mPath);
		}
		mTW.mLikeRecord.mLName = mLName;
		mTW.mLikeRecord.mLength = mTW.likeMusic.size();
	}
}
