package com.tw.music.lrc;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import com.tw.music.R;
import java.util.List;

public class LrcView extends View {
	
    int mLrvItemHight = 57; //设置歌词的间距
    int mLrvTextSize = 16; //设置歌词的字符大小
    int mLrvTextSize2 = 24; //设置歌词2的字符大小
    int mLrvColor = R.color.text_white; //设置歌词的默认颜色
    int mLrvPlayerColor = R.color.text_green; //设置歌词的进度颜色
    private List<LrcBean> list;
    private Paint gPaint;
    private Paint hPaint;
    private int width = 0, height = 0;
    private int currentPosition = 0;
    private MediaPlayer player;
    private int lastPosition = 0;
    private int highLineColor;
    private int lrcColor;
    private int mode = 0;
    public final static int KARAOKE = 1;
	private static final String TAG = "LrcView";

    public void setHighLineColor(int highLineColor) {
        this.highLineColor = highLineColor;
    }

    public void setLrcColor(int lrcColor) {
        this.lrcColor = lrcColor;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public void setPlayer(MediaPlayer player) {
        this.player = player;
    }

    /**
     * 标准歌词字符串
     *
     * @param lrc
     */
    public void setLrc(String lrc) {
    	list = LrcUtil.parseStr2List(lrc);
    }

    public LrcView(Context context) {
        this(context, null);
    }

    public LrcView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LrcView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.LrcView);
        highLineColor = ta.getColor(R.styleable.LrcView_hignLineColor, getResources().getColor(mLrvPlayerColor));
        lrcColor = ta.getColor(R.styleable.LrcView_lrcColor, getResources().getColor(mLrvColor));
        mode = ta.getInt(R.styleable.LrcView_lrcMode,mode);
        Log.i(TAG, "mode  "+mode);
        ta.recycle();
        gPaint = new Paint();
        gPaint.setAntiAlias(true);
        gPaint.setColor(lrcColor);
        gPaint.setTextSize(setTextSize(mLrvTextSize));
        gPaint.setTextAlign(Paint.Align.CENTER);
        hPaint = new Paint();
        hPaint.setAntiAlias(true);
        hPaint.setColor(highLineColor);
        hPaint.setTextSize(setTextSize(mLrvTextSize2));
        hPaint.setTextAlign(Paint.Align.CENTER);
       
    }

    private int setTextSize(int i){
    	 DisplayMetrics dm = this.getResources().getDisplayMetrics();
		return (int) (i* dm.density);
    	
    }
    @Override
    protected void onDraw(Canvas canvas) {
        if (width == 0 || height == 0) {
            width = getMeasuredWidth();
            height = getMeasuredHeight();
        }
        if (list == null || list.size() == 0) {
            canvas.drawText(""+getResources().getString(R.string.nolrc), width / 2, height / 2, gPaint);
            return;
        }

        getCurrentPosition();

//        drawLrc1(canvas);
        int currentMillis = player.getCurrentPosition();
        drawLrc2(canvas, currentMillis);
        long start = list.get(currentPosition).getStart();
        float v = (currentMillis - start) > 500 ? currentPosition * mLrvItemHight : lastPosition * mLrvItemHight + (currentPosition - lastPosition) * mLrvItemHight * ((currentMillis - start) / 500f);
        setScrollY((int) v);
        if (getScrollY() == currentPosition * mLrvItemHight) {
            lastPosition = currentPosition;
        }
        postInvalidateDelayed(100);
    }

    private void drawLrc2(Canvas canvas, int currentMillis) {
        if (mode == 0) {
            for (int i = 0; i < list.size(); i++) {
                if (i == currentPosition) { 
                    canvas.drawText(list.get(i).getLrc(), width / 2, height / 2 + mLrvItemHight * i, hPaint);
                } else {
                    canvas.drawText(list.get(i).getLrc(), width / 2, height / 2 + mLrvItemHight * i, gPaint);
                }
            }
        }else{
            for (int i = 0; i < list.size(); i++) {
                canvas.drawText(list.get(i).getLrc(), width / 2, height / 2 + mLrvItemHight * i, gPaint);
            }
            String highLineLrc = list.get(currentPosition).getLrc();
            int highLineWidth = (int) gPaint.measureText(highLineLrc);
            int leftOffset = (width - highLineWidth) / 2;
            LrcBean lrcBean = list.get(currentPosition);
            long start = lrcBean.getStart();
            long end = lrcBean.getEnd();
            int i = (int) ((currentMillis - start) * 1.0f / (end - start) * highLineWidth);
            if (i > 0) {
                Bitmap textBitmap = Bitmap.createBitmap(i, mLrvItemHight+5, Bitmap.Config.ARGB_8888);
                Canvas textCanvas = new Canvas(textBitmap);
                textCanvas.drawText(highLineLrc, highLineWidth / 2, mLrvItemHight, hPaint);
                canvas.drawBitmap(textBitmap, leftOffset, height / 2 + mLrvItemHight * (currentPosition - 1), null);
            }
        }
    }

    public void init() {
        currentPosition = 0;
        lastPosition = 0;
        setScrollY(0);
        invalidate();
    }

    private void getCurrentPosition() {
        try {
            int currentMillis = player.getCurrentPosition();
            if (currentMillis < list.get(0).getStart()) {
                currentPosition = 0;
                return;
            }
            if (currentMillis > list.get(list.size() - 1).getStart()) {
                currentPosition = list.size() - 1;
                return;
            }
            for (int i = 0; i < list.size(); i++) {
                if (currentMillis >= list.get(i).getStart() && currentMillis < list.get(i).getEnd()) {
                    currentPosition = i;
                    return;
                }
            }
        } catch (Exception e) {
//            e.printStackTrace();
            postInvalidateDelayed(100);
        }
    }
}
