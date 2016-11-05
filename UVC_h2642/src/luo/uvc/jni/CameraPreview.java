package luo.uvc.jni;

import java.text.SimpleDateFormat;
import java.util.Date;

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

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, MyRunnable
{
	public static final String TAG = "UVCCameraPreview";
	protected Context context;

	private SurfaceHolder holder;
	Thread mainLoop = null;

	public static final int SET_PREVIEW_TEXT = 0;
	public static final int SET_RECORD_TEXT = 1;

	private boolean mIsOpened = false;
	private boolean mIsPreview = false;
	private boolean mIsRecording = false;
	private boolean shouldStop = false;

	public callback textCallback;

	// The following variables are used to draw camera images.
	private int winWidth = 0;
	private int winHeight = 0;
	private Rect rect;
	private int dw, dh;
	private float rate;

	private static final int ImageWidth = 640;
	private static final int ImageHeight = 480;

	private Bitmap mBitmap = null;

	public boolean suspend = false;
	public String control = ""; // ֻ����Ҫһ��������ѣ��������û��ʵ������

	// �Ƿ�����߳�
	@Override
	public void setSuspend(boolean susp)
	{
		if (false == susp)
		{
			// ����
			synchronized (control)
			{
				control.notifyAll();
			}
		}
		suspend = susp;
	}

	@Override
	public boolean isSuspend()
	{
		return suspend;
	}

	@Override
	public void run()
	{
		while (true)
		{
			synchronized (control)
			{
				if (true == shouldStop)
				{
					shouldStop = false;
					Log.e(TAG,"run() stop...");
					return;
				}

				if (suspend)
				{
					try
					{
						control.wait();
					} catch (InterruptedException e)
					{
						// TODO: handle exception
						e.printStackTrace();
					}
				}
			}
			runPesonelLogic();
		}
	}

	@Override
	public void runPesonelLogic()
	{
		// get camera frame
		ImageProc.getPreviewFrame(mBitmap);

		// ͼ��ʹ�ù̶��ֱ���
		updateRect(ImageWidth, ImageHeight);

		// ˢsurfaceview��ʾ
		Canvas canvas = getHolder().lockCanvas();
		if (canvas != null)
		{
			// draw camera bmp on canvas
			canvas.drawBitmap(mBitmap, null, rect, null);

			getHolder().unlockCanvasAndPost(canvas);
		}

	}

	public void initPreview()
	{
		if (false == mIsOpened)
		{
			if (0 == ImageProc.connectCamera())
			{
				Log.i(TAG, "open uvc success!!!");
				mIsOpened = true;
				mIsPreview = false;
				shouldStop = false;
				setSuspend(true);

				Toast.makeText(context.getApplicationContext(), "�ɹ�������ͷ", Toast.LENGTH_SHORT).show();
			} else
			{
				Log.i(TAG, "open uvc fail!!!");
				mIsOpened = false;
				mIsPreview = false;
				shouldStop = false;
				setSuspend(true);

				Toast.makeText(context.getApplicationContext(), "����ͷ��ʧ��", Toast.LENGTH_SHORT).show();
			}
		}
	}

	public void uninitPreview()
	{
		if (true == mIsOpened)
		{
			// ����¼��
			if (mIsRecording)
			{
				uninitRecord();
			}

			if (false == mIsPreview)
			{
				// ���Ԥ���߳������ˣ��Ȼ���
				setSuspend(false);
			}

			// ����Ԥ���߳�
			shouldStop = true;
			while (true == shouldStop);

			Log.i(TAG, "start release camera...");
			// �ر�camera
			mIsPreview = false;
			ImageProc.releaseCamera();
			textCallback.setViewText(SET_PREVIEW_TEXT, "��");
			Log.i(TAG, "release camera...");

			mIsOpened = false;
		}
	}

	public void startPreview()
	{
		if (false == mIsOpened)
		{
			initPreview();
		}

		if (true == mIsOpened)
		{
			if (false == mIsPreview)
			{
				setSuspend(false);
				textCallback.setViewText(SET_PREVIEW_TEXT, "ֹͣ");
				mIsPreview = true;
			} else
			{
				// ����¼��
				if (mIsRecording)
				{
					uninitRecord();
				}

				setSuspend(true);
				textCallback.setViewText(SET_PREVIEW_TEXT, "��ʼ");
				mIsPreview = false;
			}
		}
	}	

	public void initRecord()
	{
		if (true == mIsPreview)
		{
			if (false == mIsRecording)
			{
				Log.i(TAG, "init camera record!");
				Date date = new Date();
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
				String dateString = simpleDateFormat.format(date);
				if (null == dateString)
				{
					dateString = "luoyouren";
				}
				Log.i(TAG, dateString);

				if (0 == ImageProc.startRecord(dateString))
				{
					mIsRecording = true;
					textCallback.setViewText(SET_RECORD_TEXT, "ֹͣ");
					Toast.makeText(context.getApplicationContext(), "��ʼ¼��...", Toast.LENGTH_SHORT).show();
				} else
				{
					mIsRecording = false;
					Log.e(TAG, "init camera record failed!");
					Toast.makeText(context.getApplicationContext(), "¼������ʧ�ܣ�", Toast.LENGTH_SHORT).show();
				}
			} else
			{
				uninitRecord();
			}
		} else
		{
			Log.e(TAG, "camera has not new frame data!");
		}

		return;
	}

	public void uninitRecord()
	{
		if (true == mIsRecording)
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
		return mIsPreview;
	}

	public boolean isRecording()
	{
		return mIsRecording;
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

	// ע�⣺ʹ��findViewById��ȡCameraPreview�������������캯��
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
		if (null != mainLoop)
		{
			setSuspend(true);
			mainLoop.start();
		} else
		{
			return;
		}

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

		// ͼ��ʹ�ù̶��ֱ���
		updateRect(ImageWidth, ImageHeight);

		if (mBitmap == null)
		{
			mBitmap = Bitmap.createBitmap(ImageWidth, ImageHeight, Config.ARGB_8888);
		}

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0)
	{
		// TODO Auto-generated method stub

	}

}
