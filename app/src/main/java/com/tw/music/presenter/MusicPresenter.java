package com.tw.music.presenter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.util.ArrayList;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.media.audiofx.Visualizer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.tw.music.MusicService;
import com.tw.music.R;
import com.tw.music.TWMusic;
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
	private Contarct.mainView mainView; 
	private TWMusic mTW = null;
	Context mContext; 
	int wallpoition =0;//壁纸
	private MusicService mService = null; 
	private final String ACTION_UPDATE_ALL = "com.gss.widget.UPDATE_ALL";	
	private BaseVisualizerView mBaseVisualizerView;
	private Visualizer mVisualizer;
	private boolean showFreqView=true; //由于控制显示歌词/频谱
	private MyListAdapter mAdapter;
	private Record mSDRecord;
	private Record mUSBRecord;
	private Record mMediaRecord;
	private Record mCList;
	private Record mLikeRecord = new Record("LIKE", 4, 0);//用于存放收藏列表的目录
	private ArrayList<MusicName> likeMusic = new ArrayList<MusicName>(); //收藏歌曲的列表 存有名字+路径
	private ArrayList<Record> mSDRecordArrayList = new ArrayList<Record>(); //用于存放所有SD有关音乐的目录 SD1 2 3
	private ArrayList<Record> mUSBRecordArrayList = new ArrayList<Record>();//用于存放所有USB有关音乐的目录 USB1 2 3
	private boolean isCollectMusic = false; //用于判断是否在收藏列表


	public MusicPresenter(Contarct.mainView view) {
		mainView = view;
		mainView.setPresenter(this);
	}
	private String[] permissions = {Manifest.permission.RECORD_AUDIO,Manifest.permission.WRITE_EXTERNAL_STORAGE};

	@Override
	public void onstart(final Context mContext) {
		mTW = TWMusic.open();
		this.mContext = mContext;
		new Thread(new Runnable() {
			@Override
			public void run() {
				fullScreen((Activity) mContext);
				initRecord();
				setIndex(mTW.mCurrentPath);
				mAdapter = new MyListAdapter(mContext);
				mTW.requestService(TWMusic.ACTIVITY_RUSEME);
				mainView.showWallPaper(wallpoition);
				mainView.showLrcorVis(showFreqView);
				mainView.showListDrawer(mCList.mIndex);
				mainView.updateAdapterData(mAdapter);
				mContext.bindService(new Intent(mContext, MusicService.class), mConnection, mContext.BIND_AUTO_CREATE);

				if((mService != null) && !mService.isPlaying()) {
					mService.start();
					mService.seekTo(mTW.mCurrentPos);
					mService.duck(false);
				}
				if (mTW!=null) {
					mainView.showRepeat(mTW.mRepeat, 0);
				}
				CollectionUtils.getCollectionMusicList(mContext,likeMusic);
				showFreqView = SharedPreferencesUtils.getBooleanPref(mContext, "music","showFreqView");
				wallpoition = SharedPreferencesUtils.getIntPref(mContext, "id", "id");
				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
					int i0 = ContextCompat.checkSelfPermission(mContext,permissions[0]);
					int i1 = ContextCompat.checkSelfPermission(mContext,permissions[1]);
					if(i1 != PackageManager.PERMISSION_GRANTED || i0 != PackageManager.PERMISSION_GRANTED){
						startPermission();
					}
					if(i1 == PackageManager.PERMISSION_GRANTED){
						setVisualizerFxAndUi();
					}
				}
			}
		}).start();
	}

	// 开始提交请求权限
	private void startPermission() {
		ActivityCompat.requestPermissions((Activity) mContext, permissions, 321);
		setVisualizerFxAndUi();
	}

	/**
	 * 根据播放路径拿到临时播放目录
	 * @param args
	 */
	private void setIndex(String args) {
		if (args == null) {
			return;
		}
		if(args.contains("/mnt/sdcard/iNand")){
			mCList = mMediaRecord;
			mCList.mIndex = 3;
		}else if(args.contains("/storage/usb")){
			if(mUSBRecordArrayList.size() > 0) {
				if(mTW.mUSBRecordLevel >= mUSBRecordArrayList.size()) {
					mTW.mUSBRecordLevel = 0;
				}
				mCList = mUSBRecordArrayList.get(mTW.mUSBRecordLevel);
			} else {
				mCList = mUSBRecord;
			}    			
		}else if(args.contains("/storage/extsd")){
			if(mSDRecordArrayList.size() > 0) {
				if(mTW.mSDRecordLevel >= mSDRecordArrayList.size()) {
					mTW.mSDRecordLevel = 0;
				}
				mCList = mSDRecordArrayList.get(mTW.mSDRecordLevel);
			} else {
				mCList = mSDRecord;
			}
		}else{
			mCList = mTW.mPlaylistRecord;
			mCList.mIndex = 0;
		}
	}
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what) {
			case TWMusic.RETURN_MOUNT: {
				String volume = null;
				switch(msg.arg1) {
				case 1:
					volume = "/storage/" + msg.obj;
					if(msg.arg2 == 0) {
						removeRecordSD(volume);
					} else {
						addRecordSD(volume);
					}
					break;
				case 2:
					volume = "/storage/" + msg.obj;
					if(msg.arg2 == 0) {
						removeRecordUSB(volume);
					} else {
						addRecordUSB(volume);
					}
					break;
				case 3:
					volume = "/mnt/sdcard/iNand";
					if(msg.arg2 == 0) {
						mMediaRecord.clearRecord();
					} else {
						loadVolume(mMediaRecord, volume);
					}
					break;
				}
				if((mTW.mCurrentPath != null) && mTW.mCurrentPath.startsWith(volume)) {
					if(msg.arg2 != 0) {
						if((mService != null) && !mService.isPlaying() && (mTW.getService() == TWMusic.ACTIVITY_RUSEME)) {
							mService.start();
							mService.seekTo(mTW.mCurrentPos);
							mService.duck(false);
						}
					}
				}
				mAdapter.notifyDataSetChanged();
				break;
			}
			case TWMusic.NOTIFY_CHANGE:
				if(mService != null) {
					showMusicInfo();
				}
				break;
			case TWMusic.SHOW_PROGRESS:
				if(mService != null) {
					int duration = mService.getDuration();
					int position = mService.getCurrentPosition();
					if(duration < 0) {
						duration = 0;
					}
					if(position < 0) {
						position = 0;
					}
					/**
					 * 修复拖拉进度条，音乐结束多运行几秒的问题
					 * @author Truman
					 */
					if(position > duration){
						return;
					} else {
						mainView.showSeekBar(duration, position);
					}
					CharSequence artistName = mService.getArtistName();
					CharSequence albumName = mService.getAlbumName();
					CharSequence titleName = mService.getTrackName();
					CharSequence fileName = mService.getFileName();
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
					mainView.showID3((String)titleName, (String)artistName, (String)albumName);
					CollectionUtils.getCollectionMusicList(mContext,likeMusic);
				}
				break;
			}
		}
	};

	/**
	 * 
	 */
	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
			mService = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mService = ((MusicService.MusicBinder)service).getService();
			mService.setAHandler(mHandler);
			if(!mService.isPlaying()) {
				mService.start();
				mService.seekTo(mTW.mCurrentPos);
				mService.duck(false);
			}
			mBaseVisualizerView = new BaseVisualizerView(mContext);
			mBaseVisualizerView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));

			mHandler.sendEmptyMessage(mTW.NOTIFY_CHANGE);
		}
	};

	private void setupVisualizerFxAndUi() {
		try {
			if(mVisualizer != null){
				mVisualizer = null;
			}
			mVisualizer = new Visualizer(mService.getPlayer().getAudioSessionId());
			mVisualizer.setEnabled(false);
			mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
			//		mVisualizer.getCaptureSize();//频谱段位
			mBaseVisualizerView.setVisualizer(mVisualizer);
			mVisualizer.setEnabled(true);
			mainView.showVisualizerView(mBaseVisualizerView);
		}catch (Exception e){
			startPermission();
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
	}

	@Override
	public void onpause() {
		mTW.requestService(TWMusic.ACTIVITY_PAUSE);
	}

	@Override
	public void onresume() {
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
		}
	}


	@SuppressLint("NewApi")
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
		Log.i("md", "wallpoition: "+wallpoition);
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
			if (CollectionUtils.itBeenCollected(mContext, mTW.mCurrentAPath, likeMusic)) { 
				if(((ImageView) mAdapter.ivLove) != null){
					((ImageView) mAdapter.ivLove).getDrawable().setLevel(0);
				}
				mainView.showCollect(false);
				CollectionUtils.removeMusicFromCollectionList(mTW.mCurrentAPath,likeMusic);
			} else {
				if(((ImageView) mAdapter.ivLove) != null){
					((ImageView) mAdapter.ivLove).getDrawable().setLevel(1);
				}
				mainView.showCollect(true);
				if(!CollectionUtils.itBeenCollected(mContext,  mTW.mCurrentAPath, likeMusic)){
					CollectionUtils.addMusicToCollectionList(new MusicName(mService.getFileName(), mTW.mCurrentAPath), likeMusic);
				}
			}
			mAdapter.notifyDataSetChanged();
			CollectionUtils.saveCollectionMusicList(mContext,likeMusic);
		}
	}

	private class MyListAdapter extends BaseAdapter {
		public MyListAdapter(Context context) {
			mContext = context;
		}

		@Override
		public int getCount() {
			if(isCollectMusic){
				return likeMusic.size();
			}
			if(mCList == null) {
				return 0;
			} else if(mCList.mLevel == 0) {
				return mCList.mCLength;
			} else {
				return mCList.mCLength + 1;
			}
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v;
			if(convertView == null) {
				v = newView(parent);
			} else {
				v = convertView;
			}
			bindView(v, position, parent);
			return v;
		}

		@Override
		public int getViewTypeCount() {
			return 2;
		}
		private class ViewHolder {
			ImageView play_indicator;
			ImageView icon;
			ImageView music_icon;
			ImageView ivLove;
			ImageView ivItemDel;
			ImageView ivIsPlaying;
			TextView line;
			TextView tvArtist;
			TextView tvIndex;
			ImageView file_icon;
		}

		public ViewHolder vh;
		public View newView(ViewGroup parent) {
			View v = LayoutInflater.from(mContext).inflate(R.layout.music_list_item, parent, false);
			vh = new ViewHolder();
			vh.music_icon = (ImageView) v.findViewById(R.id.music_icon);
			vh.line = (TextView) v.findViewById(R.id.line);
			vh.play_indicator = (ImageView) v.findViewById(R.id.play_indicator);
			vh.tvArtist=(TextView) v.findViewById(R.id.tv_artist);
			vh.tvIndex=(TextView) v.findViewById(R.id.tv_index);
			vh.file_icon=(ImageView) v.findViewById(R.id.file_item);
			vh.ivIsPlaying=(ImageView) v.findViewById(R.id.iv_is_playing);
			vh.ivLove=(ImageView) v.findViewById(R.id.btn_item_love);
			vh.ivItemDel=(ImageView) v.findViewById(R.id.btn_item_delete);
			v.setTag(vh);
			return v;
		}

		private void bindView(View v, final int position, ViewGroup parent) {
			final ViewHolder vh = (ViewHolder) v.getTag();
			String name, path;

			vh.music_icon.setVisibility(View.GONE);
			vh.tvArtist.setVisibility(View.GONE);
			vh.tvIndex.setVisibility(View.GONE);
			vh.ivIsPlaying.setVisibility(View.GONE);
			vh.ivLove.setVisibility(View.VISIBLE);
			vh.ivItemDel.setVisibility(View.GONE);
			if (isCollectMusic) {
				vh.line.setText(likeMusic.get(position).mName);
				vh.tvIndex.setText(String.valueOf(position+1));
				vh.tvIndex.setVisibility(View.VISIBLE);
				vh.file_icon.getDrawable().setLevel(0);
				vh.play_indicator.setVisibility(View.GONE);
				vh.music_icon.setVisibility(View.VISIBLE);

				vh.ivLove.setVisibility(View.GONE);
				vh.ivItemDel.setVisibility(View.VISIBLE);
				//	            ivCollect.getDrawable().setLevel(1);

				if(mTW.mCurrentAPath.equals(likeMusic.get(position).mPath)){
					changeTextColor(vh,R.color.text_green);
					vh.music_icon.getDrawable().setLevel(1);
					//					vh.ivItemDel.setVisibility(View.VISIBLE);
					vh.ivIsPlaying.setVisibility(View.VISIBLE);
					//					((RelativeLayout)v).getBackground().setLevel(2);
				}else{
					changeTextColor(vh,R.color.text_white);
					vh.music_icon.getDrawable().setLevel(0);
					vh.ivIsPlaying.setVisibility(View.GONE);
					vh.play_indicator.setVisibility(View.GONE);
				}
				final String nameString;
				final String pathString ;
				if(mLikeRecord.mLevel == 0) {
					nameString=mLikeRecord.mLName[position].mName;
					pathString=mLikeRecord.mLName[position].mPath;
				} else if(position == 0) {
					nameString = mLikeRecord.mName;
					pathString = null;
				} else {
					nameString = mLikeRecord.mLName[position - 1].mName;
					pathString = mLikeRecord.mLName[position - 1].mPath;
				}
				final boolean isEmpty=TextUtils.isEmpty(nameString);

				vh.ivItemDel.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View arg0) {
						if(!isEmpty){
							vh.ivLove.getDrawable().setLevel(0);
							if (mTW.mCurrentAPath.equals(pathString)) {
								mainView.showCollect(false);
							}
							CollectionUtils.removeMusicFromCollectionList(pathString,likeMusic);
							CollectionUtils.saveCollectionMusicList(mContext,likeMusic);
							collectList();
							mTW.mCurrentPath = "/mnt/sdcard/iNand/.";
							mTW.mPlaylistRecord.copyLName(mLikeRecord);
						}

					}
				});
			} else {
				if(mCList.mLevel == 0) {
					name = mCList.mLName[position].mName;
					path = mCList.mLName[position].mPath;
				} else if(position == 0) {
					name = mCList.mName;
					path = null;
				} else {
					name = mCList.mLName[position - 1].mName;
					path = mCList.mLName[position - 1].mPath;
				}
				vh.line.setText(name);
				if((mCList.mLevel != 0) && (position == 0)) {
					//					((RelativeLayout)v).getBackground().setLevel(1);
					vh.play_indicator.getDrawable().setLevel(2);
					vh.file_icon.getDrawable().setLevel(1);
					vh.ivIsPlaying.setVisibility(View.GONE);
					vh.ivLove.setVisibility(View.GONE);
					//	                vh.ivItemDel.setVisibility(View.GONE);
				} else {
					vh.play_indicator.setVisibility(View.VISIBLE);
					//					((RelativeLayout)v).getBackground().setLevel(0);
					changeTextColor(vh,R.color.text_white);
					vh.music_icon.setVisibility(View.VISIBLE);
					vh.ivLove.setVisibility(View.VISIBLE);
					vh.music_icon.getDrawable().setLevel(0);
					vh.ivIsPlaying.setVisibility(View.GONE);
					if((mCList.mLevel == 1) || (mCList.mIndex == 0)) {
						vh.play_indicator.getDrawable().setLevel(0);
						vh.file_icon.getDrawable().setLevel(0);
						vh.tvIndex.setVisibility(View.VISIBLE);
						if((path != null) && path.equals(mTW.mCurrentAPath)) {
							//							((RelativeLayout)v).getBackground().setLevel(2);
							changeTextColor(vh,R.color.text_green);
							vh.ivIsPlaying.setVisibility(View.VISIBLE);
							//							vh.ivLove.setVisibility(View.VISIBLE);
							vh.music_icon.getDrawable().setLevel(1);
						}
					} else {
						vh.play_indicator.getDrawable().setLevel(1);
						vh.file_icon.getDrawable().setLevel(1);
						vh.tvIndex.setVisibility(View.GONE);
						vh.music_icon.setVisibility(View.GONE);
						vh.ivLove.setVisibility(View.GONE);
						if((path != null) && path.equals(mTW.mCurrentPath)) {
							//							((RelativeLayout)v).getBackground().setLevel(1);
							vh.ivIsPlaying.setVisibility(View.GONE);
						}

					}

					if ((mCList.mLevel !=0 && position==0)||(mCList.mIndex!=0 && position==0)) {
						vh.tvIndex.setVisibility(View.GONE);
						vh.music_icon.setVisibility(View.GONE);
						vh.ivLove.setVisibility(View.GONE);
					}

					if (mCList.mLevel!=0 && position!=0) {
						vh.tvIndex.setText(String.valueOf(position));
					}else {
						vh.tvIndex.setText(String.valueOf(position+1));
					}

					vh.ivIsPlaying.setBackgroundResource(R.drawable.lev_play_now);
					AnimationDrawable anim = (AnimationDrawable) vh.ivIsPlaying.getBackground();
					anim.start();


					final String nameString=name;
					final String pathString=path;
					final boolean isEmpty=TextUtils.isEmpty(name);

					if (CollectionUtils.itBeenCollected(mContext,pathString,likeMusic)){
						vh.ivLove.getDrawable().setLevel(1);
					}else {
						vh.ivLove.getDrawable().setLevel(0);
					}

					vh.ivLove.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View arg0) {
							try {
								if(!isEmpty){
									if (CollectionUtils.itBeenCollected(mContext,pathString,likeMusic)) {
										if (mTW.mCurrentAPath.equals(pathString)) {
											mainView.showCollect(false);
										}
										CollectionUtils.removeMusicFromCollectionList(pathString,likeMusic);
									} else {
										if (mTW.mCurrentAPath.equals(pathString)) {
											mainView.showCollect(true);
										}
										CollectionUtils.addMusicToCollectionList(new MusicName(nameString, pathString), likeMusic);
									}
									if(isCollectMusic){
										//										mList.requestLayout();
									}
									mAdapter.notifyDataSetChanged();
									CollectionUtils.saveCollectionMusicList(mContext,likeMusic);

								}
							} catch (Exception e) {
							}
						}
					});
				}
			}
		}

		private void changeTextColor(ViewHolder vh,int color) {
			vh.line.setTextColor(mContext.getResources().getColor(color));
			vh.tvIndex.setTextColor(mContext.getResources().getColor(color));
			vh.tvArtist.setTextColor(mContext.getResources().getColor(color));

		}

		private Context mContext;
		public Object ivLove;
	}

	private void collectList() {
		MusicName[] mLName = new MusicName[likeMusic.size()];
		for(int i = 0; i < likeMusic.size();i++){
			mLName[i] = new MusicName(likeMusic.get(i).mName,likeMusic.get(i).mPath);
		}
		mLikeRecord.mLName = mLName;
		mLikeRecord.mLength = likeMusic.size();
		mCList = mLikeRecord;
		mCList.mIndex = 4;
		mAdapter.notifyDataSetChanged();
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
		mCList = mTW.mPlaylistRecord;
		mCList.mIndex = 0;
		mAdapter.notifyDataSetChanged();
		isCollectMusic = false;
	}

	@Override
	public void openSDList() {
		mainView.showListDrawer(1);
		if (isContinuousClick(1)) {
			if(mSDRecordArrayList.size() > 0) {
				if(++mTW.mSDRecordLevel >= mSDRecordArrayList.size()) {
					mTW.mSDRecordLevel = 0;
				}
				mCList = mSDRecordArrayList.get(mTW.mSDRecordLevel);
			} else {
				mCList = mSDRecord;
			}
		}else{
			if(mSDRecordArrayList.size() > 0) {
				if(mTW.mSDRecordLevel >= mSDRecordArrayList.size()) {
					mTW.mSDRecordLevel = 0;
				}
				mCList = mSDRecordArrayList.get(mTW.mSDRecordLevel);
			} else {
				mCList = mSDRecord;
			}
		}
		mAdapter.notifyDataSetChanged();
		isCollectMusic = false;
	}
	@Override
	public void openUSBList() {
		mainView.showListDrawer(2);
		if (isContinuousClick(2)) {
			if(mUSBRecordArrayList.size() > 0) {
				if(++mTW.mUSBRecordLevel >= mUSBRecordArrayList.size()) {
					mTW.mUSBRecordLevel = 0;
				}
				mCList = mUSBRecordArrayList.get(mTW.mUSBRecordLevel);
			} else {
				mCList = mUSBRecord;
			}
		}else{
			if(mUSBRecordArrayList.size() > 0) {
				if(mTW.mUSBRecordLevel >= mUSBRecordArrayList.size()) {
					mTW.mUSBRecordLevel = 0;
				}
				mCList = mUSBRecordArrayList.get(mTW.mUSBRecordLevel);
			} else {
				mCList = mUSBRecord;
			}
		}
		Log.i("md", "mTW.mUSBRecordLevel: "+mTW.mUSBRecordLevel);
		mAdapter.notifyDataSetChanged();
		isCollectMusic = false;
	}
	@Override
	public void openiNandList() {
		isContinuousClick(3);
		mainView.showListDrawer(3);
		mCList = mMediaRecord;
		mCList.mIndex = 3;
		mAdapter.notifyDataSetChanged();
		isCollectMusic = false;
	}

	@Override
	public void openCollectList() {
		isContinuousClick(4);
		mainView.showListDrawer(4);
		collectList();
		isCollectMusic = true;
	}

	@Override
	public void setListitemlistener(int position) {
		try {
			if(isCollectMusic){
				mTW.mCurrentAPath = likeMusic.get(position).mPath;
				String path = mTW.mCurrentAPath.substring(0, mTW.mCurrentAPath.lastIndexOf("/"));
				mTW.mCurrentPath = path;
				mTW.mPlaylistRecord.copyLName(mLikeRecord);
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
				if((mCList.mLevel != 0) && (position == 0)){
					mCList = mCList.mPrev;
				} else {
					if(mCList.mLevel != 0) {
						position--;
					}
					if((mCList.mLevel == 0) && (mCList.mIndex != 0)) {
						Record r = mCList.getNext(position);
						if(r == null) {
							r = new Record(mCList.mLName[position].mName, position, mCList.mLevel + 1, mCList);
							mTW.loadFile(r, mCList.mLName[position].mPath);
						}
						mCList.setNext(r);
						mCList = r;
					} else {
						mTW.mCurrentIndex = position;
						mTW.mCurrentAPath = mCList.mLName[position].mPath;
						String path = mTW.mCurrentAPath.substring(0, mTW.mCurrentAPath.lastIndexOf("/"));
						if(path != null) {
							if(mCList.mLevel == 1) {
								mTW.mPlaylistRecord.copyLName(mCList);
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
	private void initRecord() {
		try {


			mSDRecord = new Record("SD", 1, 0);
			File[] fileSD = new File("/storage").listFiles(new FileFilter() {
				@Override
				public boolean accept(File f) {
					String n = f.getName();
					if(f.canRead() && f.isDirectory() && n.startsWith("extsd")) {
						return true;
					}
					return false;
				}
			});
			if(fileSD != null) {
				for(File f : fileSD) {
					addRecordSD(f.getAbsolutePath());
				}
			}
			mUSBRecord = new Record("USB", 2, 0);
			File[] fileUSB = new File("/storage").listFiles(new FileFilter() {
				@Override
				public boolean accept(File f) {
					String n = f.getName();
					if(f.canRead() && f.isDirectory() && n.startsWith("usb")) {
						return true;
					}
					return false;
				}
			});
			if(fileUSB != null) {
				for(File f : fileUSB) {
					addRecordUSB(f.getAbsolutePath());
				}
			}
			mMediaRecord = new Record("iNand", 3, 0);
			loadVolume(mMediaRecord, "/mnt/sdcard/iNand");
			mCList = mTW.mPlaylistRecord;
			if (mCList.mCLength == 0) {
				if(mSDRecordArrayList.size() > 0) {
					mCList = mSDRecordArrayList.get(0);
				} else {
					mCList = mSDRecord;
				}
				if (mCList.mCLength == 0) {
					if(mUSBRecordArrayList.size() > 0) {
						mCList = mUSBRecordArrayList.get(0);
					} else {
						mCList = mUSBRecord;
					}
					if (mCList.mCLength == 0) {
						mCList = mMediaRecord;
						if (mCList.mCLength == 0) {
							mCList = mTW.mPlaylistRecord;
						}
					}
				}
			}
		}catch (Exception e){
			Log.i("md",""+e.toString());
		}
	}

	private void loadVolume(Record record, String volume) {
		if ((record != null) && (volume != null)) {
			try {
				BufferedReader br = null;
				try {
					String xpath = null;
					if(volume.startsWith("/storage/usb") || volume.startsWith("/storage/extsd")) {
						xpath = "/data/tw/" + volume.substring(9);
					} else {
						xpath = volume + "/DCIM";
					}
					br = new BufferedReader(new FileReader(xpath + "/.music"));
					String path = null;
					ArrayList<MusicName> l = new ArrayList<MusicName>();
					while((path = br.readLine()) != null) {
						File f = new File(volume + "/" + path);
						if (f.canRead() && f.isDirectory()) {
							String n = f.getName();
							String p = f.getAbsolutePath();
							if(n.equals(".")) {
								String p2 = p.substring(0, p.lastIndexOf("/"));
								String p3 = p2.substring(p2.lastIndexOf("/") + 1);
								if(mTW.loadFileIsHas(p)){
									l.add(new MusicName(p3, p));
								}
							} else {
								if(mTW.loadFileIsHas(p)){
									l.add(new MusicName(n, p));
								}
							}
						}
					}
					record.setLength(l.size());
					for(MusicName n : l) {
						record.add(n);
					}
					l.clear();
				} catch (Exception e) {
				} finally {
					if(br != null) {
						br.close();
						br = null;
					}
				}
			} catch (Exception e) {
			}
		}
	}

	private void addRecordSD(String path) {
		for(Record r : mSDRecordArrayList) {
			if(path.equals(r.mName)) {
				return;
			}
		}
		Record r = new Record(path, 1, 0);
		loadVolume(r, path);
		mSDRecordArrayList.add(r);
		if((mCList != null) && mCList.mName.equals("SD")) {
			mCList = mSDRecordArrayList.get(0);
		}
	}

	private void addRecordUSB(String path) {
		for(Record r : mUSBRecordArrayList) {
			if(path.equals(r.mName)) {
				return;
			}
		}
		Record r = new Record(path, 2, 0);
		loadVolume(r, path);
		mUSBRecordArrayList.add(r);
		if((mCList != null) && mCList.mName.equals("USB")) {
			mCList = mUSBRecordArrayList.get(0);
		}
	}

	private void removeRecordSD(String path) {
		for(Record r : mSDRecordArrayList) {
			if(path.equals(r.mName)) {
				Record t = mCList;
				if(mCList.mLevel == 1) {
					t = mCList.mPrev;
				}
				String s = t.mName;
				r.clearRecord();
				mSDRecordArrayList.remove(r);
				if(mTW.mSDRecordLevel >= mSDRecordArrayList.size()){
					mTW.mSDRecordLevel = mSDRecordArrayList.size() - 1;
					if(mTW.mSDRecordLevel < 0) {
						mTW.mSDRecordLevel = 0;
					}
				}
				if(path.equals(s)) {
					if(mSDRecordArrayList.size() > 0) {
						mCList = mSDRecordArrayList.get(mTW.mSDRecordLevel);
					} else {
						mCList = mSDRecord;
					}
				}
				return;
			}
		}
	}

	private void removeRecordUSB(String path) {
		for(Record r : mUSBRecordArrayList) {
			if(path.equals(r.mName)) {
				Record t = mCList;
				if(mCList.mLevel == 1) {
					t = mCList.mPrev;
				}
				String s = t.mName;
				r.clearRecord();
				mUSBRecordArrayList.remove(r);
				if(mTW.mUSBRecordLevel >= mUSBRecordArrayList.size()){
					mTW.mUSBRecordLevel = mUSBRecordArrayList.size() - 1;
					if(mTW.mUSBRecordLevel < 0) {
						mTW.mUSBRecordLevel = 0;
					}
				}
				if(path.equals(s)) {
					if(mUSBRecordArrayList.size() > 0) {
						mCList = mUSBRecordArrayList.get(mTW.mUSBRecordLevel);
					} else {
						mCList = mUSBRecord;
					}
				}
				return;
			}
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
		CharSequence fileName = mService.getFileName();
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
		mainView.showSmoothScrollToPosition(mTW.mCurrentIndex + mCList.mLevel);
		mainView.showPlaypause(mService.isPlaying());
		mainView.showID3((String)titleName, (String)artistName, (String)albumName);
		mainView.showAlbumArt(mService.getAlbumArt());
	}

	private void updateCollectButtonState(){
		if (mTW.mCurrentAPath != null) {
			if (CollectionUtils.itBeenCollected(mContext, mTW.mCurrentAPath, likeMusic)) {
				mainView.showCollect(true);
			} else {
				mainView.showCollect(false);
			}
		}
	}
}
