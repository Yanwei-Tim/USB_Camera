package luo.uvc.jni;


import java.text.SimpleDateFormat;
import java.util.Date;

import luo.uvc.jni.ImageProc.FrameInfo;
import android.R.bool;
import android.R.string;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Bitmap.Config;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Runnable
{
	public static final String TAG = "UVCCameraPreview";
	protected Context context;

	private SurfaceHolder holder;
	Thread mainLoop = null;
	
	public static final int SET_PREVIEW_TEXT = 0;
	public static final int SET_RECORD_TEXT = 1;

	private boolean mIsOpened = false;
	private boolean mIsRecording = false;
	private boolean shouldStop = false;

	public callback textCallback;

	// The following variables are used to draw camera images.
	private int winWidth = 0;
	private int winHeight = 0;
	private Rect rect;
	private int dw, dh;
	private float rate;

	public CameraPreview(Context context)
	{
		super(context);
		// TODO Auto-generated constructor stub
		this.context = context;
		Log.d(TAG, "CameraPreview constructed");
		setFocusable(true);

		holder = getHolder();
		holder.addCallback(this);
		holder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);

	}

	//ע�⣺ʹ��findViewById��ȡCameraPreview�������������캯��
	public CameraPreview(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		this.context = context;
		Log.d(TAG, "CameraPreview constructed");
		setFocusable(true);

		holder = getHolder();
		holder.addCallback(this);
		holder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);
		
	}	

	public void initPreview()
	{
		int index = -1;
		if (mIsOpened == false)
		{
			if (0 == ImageProc.connectCamera(index))
			{
				Log.i(TAG, "open uvc success!!!");
				mIsOpened = true;
				textCallback.setViewText(SET_PREVIEW_TEXT, "�ر�");
				if (null != mainLoop)
				{
					shouldStop = false;
					Log.i(TAG, "preview mainloop starting...");
					mainLoop.start();
				}

				Toast.makeText(context.getApplicationContext(), "�ɹ�������ͷ", Toast.LENGTH_SHORT).show();
			} else
			{
				Log.i(TAG, "open uvc fail!!!");
				mIsOpened = false;
				Toast.makeText(context.getApplicationContext(), "����ͷ��ʧ��", Toast.LENGTH_SHORT).show();
			}

		} else
		{
			uninitPreview();
		}
	}

	public void uninitPreview()
	{
		//����¼��
		uninitRecord();
		
		//ֹͣԤ���߳�
		if (null != mainLoop)
		{
			Log.i(TAG, mainLoop.isAlive() ? "mainloop is alive!" : "mainloop is not alive!");
			if (mainLoop.isAlive())
			{
				shouldStop = true;
				while (shouldStop)
				{
					try
					{
						Thread.sleep(100); // wait for thread stopping
					} catch (Exception e)
					{
					}
				}
			}
		}

		//�ر�camera
		if (mIsOpened)
		{
			mIsOpened = false;
			ImageProc.releaseCamera();
			textCallback.setViewText(SET_PREVIEW_TEXT, "��");
			Log.i(TAG, "release camera...");
		}
	}
	
	public void initRecord()
	{
		if(mIsOpened)
		{
			if(mIsRecording == false)
			{
				Log.i(TAG, "init camera record!");
				Date date = new Date();
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");				
				String dateString = simpleDateFormat.format(date);
				if(null == dateString)
				{
					dateString = "luoyouren";
				}
				Log.i(TAG, dateString);
				
				if(0 == ImageProc.startRecord(dateString))
				{
					mIsRecording = true;
					textCallback.setViewText(SET_RECORD_TEXT, "ֹͣ");
					Toast.makeText(context.getApplicationContext(), "��ʼ¼��...", Toast.LENGTH_SHORT).show();
				}
				else 
				{
					mIsRecording = false;
					Log.e(TAG, "init camera record failed!");
					Toast.makeText(context.getApplicationContext(), "¼������ʧ�ܣ�", Toast.LENGTH_SHORT).show();
				}
				return;
			}
			else 
			{
				uninitRecord();
				return;
			}
		}
		else 
		{
			Log.e(TAG, "camera has not been opened!");
			return;
		}
	}
	
	public void uninitRecord()
	{
		if(mIsRecording)
		{
			Log.i(TAG, "camera is already recording! So we stop it.");
			ImageProc.stopRecord();
			mIsRecording = false;
			textCallback.setViewText(SET_RECORD_TEXT, "¼��");
			return;
		}
	}

	public boolean isOpen()
	{
		return mIsOpened;
	}
	
	public boolean isRecording()
	{
		return mIsRecording;
	}

	@Override
	public void run()
	{
		// TODO Auto-generated method stub
		while (true && mIsOpened)
		{
			// get camera frame
			FrameInfo frame = ImageProc.getFrame();
			if (null == frame)
			{
				continue;
			}

			int w = frame.getWidth();
			int h = frame.getHeigth();
			Log.i(TAG, "frame.width = " + w + " frame.height = " + h);
			// ����ͼ���С������ʾ�����С
			// һ����˵ͼ���С����仯: 640x480
			updateRect(w, h);

			Bitmap resultImg = Bitmap.createBitmap(w, h, Config.ARGB_8888);

			resultImg.setPixels(frame.getPixels(), 0, w, 0, 0, w, h);

			// ˢsurfaceview��ʾ
			Canvas canvas = getHolder().lockCanvas();
			if (canvas != null)
			{
				// draw camera bmp on canvas
				canvas.drawBitmap(resultImg, null, rect, null);

				getHolder().unlockCanvasAndPost(canvas);
			}

			if (shouldStop)
			{
				shouldStop = false;
				Log.i(TAG, "mainloop will stop!");
				break;
			}
		}
		
		Log.i(TAG, "mainloop break while!");
	}

	public void updateRect(int frame_w, int frame_h)
	{
		// obtaining display area to draw a large image
		if (winWidth == 0)
		{
			winWidth = this.getWidth();
			winHeight = this.getHeight();

			if (winWidth * 3 / 4 <= winHeight)
			{
				dw = 0;
				dh = (winHeight - winWidth * 3 / 4) / 2;
				rate = ((float) winWidth) / frame_w;
				rect = new Rect(dw, dh, dw + winWidth - 1, dh + winWidth * 3 / 4 - 1);
			} else
			{
				dw = (winWidth - winHeight * 4 / 3) / 2;
				dh = 0;
				rate = ((float) winHeight) / frame_h;
				rect = new Rect(dw, dh, dw + winHeight * 4 / 3 - 1, dh + winHeight - 1);
			}
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void surfaceCreated(SurfaceHolder arg0)
	{
		// TODO Auto-generated method stub
		mainLoop = new Thread(this);
		
		updateRect(512, 512);
		// ��lenaͼ����س����в�������ʾ
		Bitmap resultImg = BitmapFactory.decodeResource(getResources(), R.drawable.lena);

		// ˢsurfaceview��ʾ
		Canvas canvas = getHolder().lockCanvas();
		if (canvas != null)
		{
			// draw camera bmp on canvas
			canvas.drawBitmap(resultImg, null, rect, null);

			getHolder().unlockCanvasAndPost(canvas);
		}

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0)
	{
		// TODO Auto-generated method stub

	}

}
