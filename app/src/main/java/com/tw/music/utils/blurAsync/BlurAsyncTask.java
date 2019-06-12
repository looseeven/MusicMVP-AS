package com.tw.music.utils.blurAsync;


import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.widget.ImageView;

import android.view.View;


/**
 * 实现高斯模糊的任务
 */
public class BlurAsyncTask extends AsyncTask {
    private Activity mOwnerActivity;
    private Bitmap mBitmap;
    private View mView;
    private View mImageView;

    public BlurAsyncTask(Activity activity, Bitmap bitmap,View imageView){
        mOwnerActivity = activity;
        mBitmap = bitmap;
//        mView = view;
        mImageView = imageView;
    }
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        // 截图
//        mOwnerActivity.getWindow().getDecorView().setDrawingCacheEnabled(true);
//        Rect frame = new Rect();
//        mOwnerActivity.getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);
//        int statusBarHeight = frame.top;
//        int statusBarWidth = frame.left;
//        int statusBarRight = frame.right;
//        Bitmap b1 = mOwnerActivity.getWindow().getDecorView().getDrawingCache();
//        int width = mOwnerActivity.getWindowManager().getDefaultDisplay().getWidth();
//        int height = mOwnerActivity.getWindowManager().getDefaultDisplay().getHeight();
//        mBitmap = Bitmap.createBitmap(b1,  0, statusBarHeight, width,  height);
//        mView.destroyDrawingCache();
    }

    @Override
    protected Object doInBackground(Object[] params) {
        // 进行高斯模糊
        if (mBitmap != null) {
            mBitmap = RenderScriptBlurHelper.doBlur(mBitmap, 10, false, mOwnerActivity);
        }
        return null;
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);
        if (mBitmap != null) {
        	BitmapDrawable bd= new BitmapDrawable(mOwnerActivity.getResources(), mBitmap);
            mImageView.setBackground(bd);
        }
    }
}
