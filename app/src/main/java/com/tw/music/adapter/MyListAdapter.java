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

public class MyListAdapter extends BaseAdapter {
    private TWMusic mTW;
    private Context mContext;
    public Object ivLove;

    public MyListAdapter(Context context, TWMusic mtw) {
        mTW = mtw;
        mContext = context;
    }

    @Override
    public int getCount() {
        try {
            if (MusicPresenter.isCollectMusic) {
                return mTW.likeMusic.size();
            }
            if (mTW.mCList == null) {
                return 0;
            } else if (mTW.mCList.mLevel == 0) {
                return mTW.mCList.mCLength;
            } else {
                return mTW.mCList.mCLength + 1;
            }
        } catch (Exception e) {
            Log.i("md", "ListAdapter  " + e.toString());
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
        if (convertView == null) {
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
        /**
         * 最右側的箭頭 > V
         */
        ImageView im_indicator;
        /**
         * 收藏按鈕
         */
        ImageView btn_collection;
        /**
         * 播放狀態動畫
         */
        ImageView iv_isPlaying;
        /**
         * 資源文件夾名
         */
        TextView tv_filename;
        /**
         * 資源名
         */
        TextView song;
        /**
         * 編號
         */
        TextView tvIndex;
    }

    public ViewHolder vh;

    public View newView(ViewGroup parent) {
        View v = LayoutInflater.from(mContext).inflate(R.layout.music_list_item, parent, false);
        vh = new ViewHolder();
        vh.tv_filename = (TextView) v.findViewById(R.id.tv_filename);
        vh.song = (TextView) v.findViewById(R.id.song);
        vh.im_indicator = (ImageView) v.findViewById(R.id.im_indicator);
        vh.tvIndex = (TextView) v.findViewById(R.id.tv_index);
        vh.btn_collection = (ImageView) v.findViewById(R.id.btn_collection);
        vh.iv_isPlaying = (ImageView) v.findViewById(R.id.iv_isPlaying);
        vh.iv_isPlaying.setBackgroundResource(R.drawable.lev_play_now);
        AnimationDrawable anim = (AnimationDrawable) vh.iv_isPlaying.getBackground();
        anim.start();
        v.setTag(vh);
        return v;
    }

    private void changeTextColor(ViewHolder vh, int color) {
        vh.song.setTextColor(mContext.getResources().getColor(color));
        vh.tvIndex.setTextColor(mContext.getResources().getColor(color));
    }

    private void bindView(View v, final int position, ViewGroup parent) {
        final ViewHolder vh = (ViewHolder) v.getTag();
        String name, path;

        vh.iv_isPlaying.setVisibility(View.GONE);

        /**
         *  收藏页面
         */
        if (MusicPresenter.isCollectMusic) {
            v.findViewById(R.id.item_close).setVisibility(View.GONE);
            v.findViewById(R.id.item_open).setVisibility(View.VISIBLE);
            final String nameString;
            final String pathString;
            if (mTW.mLikeRecord.mLevel == 0) {
                nameString = mTW.mLikeRecord.mLName[position].mName;
                pathString = mTW.mLikeRecord.mLName[position].mPath;
            } else if (position == 0) {
                nameString = mTW.mLikeRecord.mName;
                pathString = null;
            } else {
                nameString = mTW.mLikeRecord.mLName[position - 1].mName;
                pathString = mTW.mLikeRecord.mLName[position - 1].mPath;
            }
            final boolean isEmpty = TextUtils.isEmpty(nameString);

            vh.song.setText(mTW.likeMusic.get(position).mName);
            vh.tvIndex.setText(String.valueOf(position + 1));
            if (mTW.mCurrentAPath.equals(mTW.likeMusic.get(position).mPath)) {
                changeTextColor(vh, R.color.text_green);
                vh.iv_isPlaying.setVisibility(View.VISIBLE);
            } else {
                changeTextColor(vh, R.color.text_white);
            }
            /*
                从收藏列表中去除 /取消收藏
             */
            vh.btn_collection.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    if (!isEmpty) {
                        if (CollectionUtils.itBeenCollected(mContext, pathString, mTW.likeMusic)) {
                            if (mTW.mCurrentAPath.equals(pathString)) {
                                MusicPresenter.mainView.showCollect(false);
                            }
                            CollectionUtils.removeMusicFromCollectionList(pathString, mTW.likeMusic);
                        } else {
                            if (mTW.mCurrentAPath.equals(pathString)) {
                                MusicPresenter.mainView.showCollect(true);
                            }
                            CollectionUtils.addMusicToCollectionList(new MusicName(nameString, pathString), mTW.likeMusic);
                        }
                        CollectionUtils.saveCollectionMusicList(mContext, mTW.likeMusic);
                        mTW.mCurrentPath = "/mnt/sdcard/iNand/.";
                        mTW.mPlaylistRecord.copyLName(mTW.mLikeRecord);
                        notifyDataSetChanged();
                    }
                }
            });
            if (CollectionUtils.itBeenCollected(mContext, pathString, mTW.likeMusic)) {
                vh.btn_collection.getDrawable().setLevel(1);
            } else {
                vh.btn_collection.getDrawable().setLevel(0);
            }
        } else {
            if (mTW.mCList.mLevel == 0) {
                name = mTW.mCList.mLName[position].mName;
                path = mTW.mCList.mLName[position].mPath;
            } else if (position == 0) {
                name = mTW.mCList.mName;
                path = null;
            } else {
                name = mTW.mCList.mLName[position - 1].mName;
                path = mTW.mCList.mLName[position - 1].mPath;
            }

            final String nameString = name;
            final String pathString = path;
            final boolean isEmpty = TextUtils.isEmpty(name);

            if ((mTW.mCList.mLevel != 0) && (position == 0)) {
                /**
                 *  点开路径列表 需要加载的第一栏 路径
                 */
                vh.tv_filename.setText(name);
                vh.im_indicator.getDrawable().setLevel(1);
                v.findViewById(R.id.item_close).setVisibility(View.VISIBLE);
                v.findViewById(R.id.item_open).setVisibility(View.GONE);
            } else {
                changeTextColor(vh, R.color.text_white);
                vh.btn_collection.setVisibility(View.VISIBLE);
                vh.iv_isPlaying.setVisibility(View.GONE);
                if ((mTW.mCList.mLevel == 1) || (mTW.mCList.mIndex == 0)) {
                    /**
                     * 点开了路径列表 需要加载的歌曲列表
                     */
                    v.findViewById(R.id.item_close).setVisibility(View.GONE);
                    v.findViewById(R.id.item_open).setVisibility(View.VISIBLE);
                    vh.song.setText(name);
                    if ((path != null) && path.equals(mTW.mCurrentAPath)) {
                        changeTextColor(vh, R.color.text_green);
                        vh.iv_isPlaying.setVisibility(View.VISIBLE);
                    }
                    if (CollectionUtils.itBeenCollected(mContext, pathString, mTW.likeMusic)) {
                        vh.btn_collection.getDrawable().setLevel(1);
                    } else {
                        vh.btn_collection.getDrawable().setLevel(0);
                    }

                    if (mTW.mCList.mLevel != 0 && position != 0) {
                        vh.tvIndex.setText(String.valueOf(position));
                    } else {
                        vh.tvIndex.setText(String.valueOf(position + 1));
                    }
                } else {
                    /**
                     * 默认打开list界面状态  只显示路径列表
                     */
                    v.findViewById(R.id.item_close).setVisibility(View.VISIBLE);
                    v.findViewById(R.id.item_open).setVisibility(View.GONE);
                    vh.tv_filename.setText(name);
                    vh.im_indicator.getDrawable().setLevel(0);
                }

                vh.btn_collection.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        try {
                            if (!isEmpty) {
                                if (CollectionUtils.itBeenCollected(mContext, pathString, mTW.likeMusic)) {
                                    if (mTW.mCurrentAPath.equals(pathString)) {
                                        MusicPresenter.mainView.showCollect(false);
                                    }
                                    CollectionUtils.removeMusicFromCollectionList(pathString, mTW.likeMusic);
                                } else {
                                    if (mTW.mCurrentAPath.equals(pathString)) {
                                        MusicPresenter.mainView.showCollect(true);
                                    }
                                    CollectionUtils.addMusicToCollectionList(new MusicName(nameString, pathString), mTW.likeMusic);
                                }
                                notifyDataSetChanged();
                                CollectionUtils.saveCollectionMusicList(mContext, mTW.likeMusic);
                            }
                        } catch (Exception e) {
                        }
                    }
                });
            }
        }
    }
}
