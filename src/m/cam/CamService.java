package m.cam;

import java.io.IOException;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.IBinder;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.RelativeLayout;

public class CamService extends Service implements OnTouchListener, Callback {
	private RelativeLayout rl;
	private WindowManager wm;
	private LayoutParams params;
	private long lastTime;
	private Camera cam;
	private SurfaceView sv;
	private Recorder recorder;

	public IBinder onBind(Intent intent) {
		return null;
	}
	
	public void onCreate() {
		Context context = getApplicationContext();
		wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		params = new LayoutParams();
		params.type = LayoutParams.TYPE_SYSTEM_ALERT;
		params.format = PixelFormat.RGBA_8888;
		params.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL | LayoutParams.FLAG_NOT_FOCUSABLE;
		params.width = Recorder.HEIGHT * 3 / 10;
		params.height = Recorder.WIDTH * 3 / 10;
		Display display = wm.getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		params.x = (size.x - params.width) / 2;
		params.y = (params.height - size.y) / 2;
		initView(context);
		wm.addView(rl, params);
	}
	
	public void onDestroy() {
		rl.postDelayed(new Runnable() {
			public void run() {
				cam.release();
				recorder.stop();
				System.exit(0);
			}
		}, 500);
		wm.removeView(rl);
	}
	
	private void initView(Context context) {
		rl = new RelativeLayout(context);
		rl.setOnTouchListener(this);
		
		cam = Camera.open(0);
		cam.setDisplayOrientation(90);
		recorder = new Recorder();
		cam.setPreviewCallback(recorder);
		
		sv = new SurfaceView(context);
		sv.getHolder().addCallback(this);
		rl.addView(sv, new RelativeLayout.LayoutParams(params.width, params.height));
	}
	
	public boolean onTouch(View v, MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			long time = System.currentTimeMillis();
			if (time - lastTime < 500) {
				stopSelf();
			} else {
				lastTime = time;
			}
		}
		return false;
	}
	
	public void surfaceCreated(SurfaceHolder holder) {
		try {
			cam.setPreviewDisplay(holder);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		Parameters parameters = cam.getParameters();
		parameters.setPreviewSize(Recorder.WIDTH, Recorder.HEIGHT);
		parameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
		sv.requestLayout();

		cam.setParameters(parameters);
		cam.startPreview();
	}
	
	public void surfaceDestroyed(SurfaceHolder holder) {
		cam.stopPreview();
	}

}
