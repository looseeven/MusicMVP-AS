package com.tw.music.adapter;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.tw.music.R;
import com.tw.music.TWMusic;
import com.tw.music.bean.MusicName;
import com.tw.music.presenter.MusicPresenter;
import com.tw.music.utils.CollectionUtils;

public class MyListAdapter extends BaseAdapter{
	private TWMusic mTW;
	private Context mContext;
	public Object ivLove;

	public MyListAdapter(Context context,TWMusic mtw) {
		mTW = mtw;
		mContext = context;
	}

	@Override
	public int getCount() {
		try {
			if(MusicPresenter.isCollectMusic){
				return mTW.likeMusic.size();
			}
			if(mTW.mCList == null) {
				return 0;
			} else if(mTW.mCList.mLevel == 0) {
				return mTW.mCList.mCLength;
			} else {
				return mTW.mCList.mCLength + 1;
			}
		} catch (Exception e) {
			Log.i("md", "ListAdapter  "+e.toString());
			return 0;
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
		if (MusicPresenter.isCollectMusic) {
			vh.line.setText(mTW.likeMusic.get(position).mName);
			vh.tvIndex.setText(String.valueOf(position+1));
			vh.tvIndex.setVisibility(View.VISIBLE);
			vh.file_icon.getDrawable().setLevel(0);
			vh.play_indicator.setVisibility(View.GONE);
			vh.music_icon.setVisibility(View.VISIBLE);

			vh.ivLove.setVisibility(View.GONE);
			vh.ivItemDel.setVisibility(View.VISIBLE);
			//	            ivCollect.getDrawable().setLevel(1);

			if(mTW.mCurrentAPath.equals(mTW.likeMusic.get(position).mPath)){
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
			if(mTW.mLikeRecord.mLevel == 0) {
				nameString=mTW.mLikeRecord.mLName[position].mName;
				pathString=mTW.mLikeRecord.mLName[position].mPath;
			} else if(position == 0) {
				nameString = mTW.mLikeRecord.mName;
				pathString = null;
			} else {
				nameString = mTW.mLikeRecord.mLName[position - 1].mName;
				pathString = mTW.mLikeRecord.mLName[position - 1].mPath;
			}
			final boolean isEmpty=TextUtils.isEmpty(nameString);

			vh.ivItemDel.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					if(!isEmpty){
						vh.ivLove.getDrawable().setLevel(0);
						if (mTW.mCurrentAPath.equals(pathString)) {
							MusicPresenter.mainView.showCollect(false);
						}
						CollectionUtils.removeMusicFromCollectionList(pathString,mTW.likeMusic);
						CollectionUtils.saveCollectionMusicList(mContext,mTW.likeMusic);
						MusicPresenter.collectList();
						mTW.mCurrentPath = "/mnt/sdcard/iNand/.";
						mTW.mPlaylistRecord.copyLName(mTW.mLikeRecord);
					}

				}
			});
		} else {
			if(mTW.mCList.mLevel == 0) {
				name = mTW.mCList.mLName[position].mName;
				path = mTW.mCList.mLName[position].mPath;
			} else if(position == 0) {
				name = mTW.mCList.mName;
				path = null;
			} else {
				name = mTW.mCList.mLName[position - 1].mName;
				path = mTW.mCList.mLName[position - 1].mPath;
			}
			vh.line.setText(name);
			if((mTW.mCList.mLevel != 0) && (position == 0)) {
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
				if((mTW.mCList.mLevel == 1) || (mTW.mCList.mIndex == 0)) {
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

				if ((mTW.mCList.mLevel !=0 && position==0)||(mTW.mCList.mIndex!=0 && position==0)) {
					vh.tvIndex.setVisibility(View.GONE);
					vh.music_icon.setVisibility(View.GONE);
					vh.ivLove.setVisibility(View.GONE);
				}

				if (mTW.mCList.mLevel!=0 && position!=0) {
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

				if (CollectionUtils.itBeenCollected(mContext,pathString,mTW.likeMusic)){
					vh.ivLove.getDrawable().setLevel(1);
				}else {
					vh.ivLove.getDrawable().setLevel(0);
				}

				vh.ivLove.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View arg0) {
						try {
							if(!isEmpty){
								if (CollectionUtils.itBeenCollected(mContext,pathString,mTW.likeMusic)) {
									if (mTW.mCurrentAPath.equals(pathString)) {
										MusicPresenter.mainView.showCollect(false);
									}
									CollectionUtils.removeMusicFromCollectionList(pathString,mTW.likeMusic);
								} else {
									if (mTW.mCurrentAPath.equals(pathString)) {
										MusicPresenter.mainView.showCollect(true);
									}
									CollectionUtils.addMusicToCollectionList(new MusicName(nameString, pathString), mTW.likeMusic);
								}
								notifyDataSetChanged();
								CollectionUtils.saveCollectionMusicList(mContext,mTW.likeMusic);

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
}
