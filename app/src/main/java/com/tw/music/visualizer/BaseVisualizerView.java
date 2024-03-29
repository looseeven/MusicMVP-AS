package com.tw.music.visualizer;

import java.util.Random;

import com.tw.music.MusicActivity;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Shader;
import android.media.audiofx.Visualizer;
import android.view.View;

public class BaseVisualizerView extends View implements Visualizer.OnDataCaptureListener {

	private static final int DN_W = 470;//view宽度与单个音频块占比 - 正常480 需微调  
	private static final int DN_H = 360;//view高度与单个音频块占比  
	private static final int DN_SL = 15;//单个音频块宽度  
	private static final int DN_SW = 25;//单个音频块高度

	private int hgap = 0;
	private int vgap = 0;
	private int levelStep = 0;
	private float strokeWidth = 0;
	private float strokeLength = 0;

	protected final static int MAX_LEVEL = 30;//音量柱·音频块 - 最大个数  

	protected final static int CYLINDER_NUM = 13;//音量柱 - 最大个数  

	protected Visualizer mVisualizer = null;//频谱器  

	protected Paint mPaint = null;//画笔  

	protected byte[] mData = new byte[CYLINDER_NUM];//音量柱 数组  

	boolean mDataEn = true;

	//构造函数初始化画笔  
	public BaseVisualizerView(Context context) {
		super(context);

		mPaint = new Paint();//初始化画笔工具  
		//        mPaint.setAntiAlias(true);//抗锯齿  
		//        mPaint.setColor(Color.WHITE);//画笔颜色  

		//        mPaint.setStrokeJoin(Join.ROUND); //频块圆角  
		//        mPaint.setStrokeCap(Cap.ROUND); //频块圆角  
	}

	//执行 Layout 操作  
	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);

		float w, h, xr, yr;

		w = right - left;
		h = bottom - top;
		xr = w / (float) DN_W;
		yr = h / (float) DN_H;

		strokeWidth = DN_SW * yr;
		strokeLength = DN_SL * xr;
		hgap = (int) ((w - strokeLength * CYLINDER_NUM) / (CYLINDER_NUM + 1));
		vgap = (int) ((h / (MAX_LEVEL + 2))*2);//频谱块高度

		mPaint.setStrokeWidth(strokeWidth); //设置频谱块宽度  
	}

	//绘制频谱块和倒影  
	protected void drawCylinder(Canvas canvas, float x, byte value) {
		if (value == 0) {value = 1;}//最少有一个频谱块  
		for (int i = 0; i < value; i++) { //每个能量柱绘制value个能量块  
			float y = (getHeight() - i * vgap - vgap);//计算y轴坐标  
			float y1=(getHeight()/2+i * vgap + vgap);
			//绘制频谱块  
			mPaint.setColor(Color.WHITE);//画笔颜色  
			canvas.drawLine(x, y, (x + strokeLength), y, mPaint);//绘制频谱块

			//绘制音量柱倒影  
			//            if (i <= 6 && value > 0) {  
			//                mPaint.setColor(Color.WHITE);//画笔颜色  
			//                mPaint.setAlpha(100 - (100 / 6 * i));//倒影颜色  
			//                canvas.drawLine(x, y1, (x + strokeLength), y1, mPaint);//绘制频谱块  
			//}  
		}
	}

	@Override
	public void onDraw(Canvas canvas) {
		//做渐变频谱
		Shader mShader = new LinearGradient(0, 0, 0, (MusicActivity.fx_height),
				new int[] {
						Color.rgb(151, 196, 251),
						Color.rgb(114, 156, 230),
						Color.rgb(61, 96, 198),
						Color.rgb(21, 53, 174)
				},
				null
				, Shader.TileMode.MIRROR);
		mPaint.setShader(mShader);// 用Shader中定义定义的颜色来话

		//频率快
//      for (int i = 0; i < CYLINDER_NUM; i++) { //绘制32个能量柱
//          if(i < 17){
//              if(mData[i] != 0)
//                  mData[i]= (byte) new Random().nextInt(14);
//              else
//                  mData[i]= mData[16];
//          }
//          float x = strokeWidth / 2 + hgap + i * (hgap + strokeLength);
//          drawCylinder(canvas, x, mData[i]);
//      }
		//真实频率
		int j=-4;
		for (int i = 0; i < CYLINDER_NUM/2-4; i++) {

			drawCylinder(canvas, strokeWidth / 2 + hgap + i * (hgap + strokeLength), mData[i]);
		}
		for(int i =CYLINDER_NUM; i>=CYLINDER_NUM/2-4; i--){
			j++;
			drawCylinder(canvas, strokeWidth / 2 + hgap + (CYLINDER_NUM/2+j-1 )* (hgap + strokeLength), mData[i-1]);
		}
	}

	/**
	 * It sets the visualizer of the view. DO set the viaulizer to null when exit the program. 
	 *
	 * @parma visualizer It is the visualizer to set. 
	 */
	public void setVisualizer(Visualizer visualizer) {
		if (visualizer != null) {
			if (!visualizer.getEnabled()) {
				visualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[0]);
			}
			levelStep = 230 / MAX_LEVEL;
			visualizer.setDataCaptureListener(this, Visualizer.getMaxCaptureRate() / 2, false, true);

		} else {

			if (mVisualizer != null) {
				mVisualizer.setEnabled(false);
				mVisualizer.release();
			}
		}
		mVisualizer = visualizer;
	}

	//这个回调应该采集的是快速傅里叶变换有关的数据  
	@Override
	public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
		byte[] model = new byte[fft.length / 2 + 1];
		if (mDataEn) {
			model[0] = (byte) Math.abs(fft[1]);
			int j = 1;
			for (int i = 2; i < fft.length; ) {
				model[j] = (byte) Math.hypot(fft[i], fft[i + 1]);
				i += 2;
				j++;
			}
		} else {
			for (int i = 0; i < CYLINDER_NUM; i++) {
				model[i] = 0;
			}
		}
		for (int i = 0; i < CYLINDER_NUM; i++) {
			final byte a = (byte) (Math.abs(model[CYLINDER_NUM - i]) / levelStep);

			final byte b = mData[i];
			if (a > b) {
				mData[i] = a;
			} else {
				if (b > 0) {
					mData[i]--;
				}
			}
		}
		postInvalidate();//刷新界面  
	}

	//这个回调应该采集的是波形数据  
	@Override
	public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
		// Do nothing...  
	}
}  
