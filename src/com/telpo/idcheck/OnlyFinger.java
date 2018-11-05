package com.telpo.idcheck;

import com.telpo.tps550.api.fingerprint.FingerPrint;
import com.telpo.tps550.api.idcard.FingerReader;
import com.telpo.tps550.api.led.LedPowerManager;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class OnlyFinger extends Activity {

	private ImageView reader_view,finger_picture;
	private TextView quality_score,compare_result;
	private FingerReader fingerReader;
	private byte[] fingerByte = null;
	private boolean getting_score = false;
	private boolean scoreThreadHasBuild = false;
	private boolean openStatus = false;
	private Button open,getFingerDetect,getFingerPicture,getFingerByte,compareFinger,getQualityScore,close;
	
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 1:
				hasClosed();
				break;
			default:
				break;
			}
		};
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.only_finger);
		reader_view = (ImageView) findViewById(R.id.reader_view);
		finger_picture = (ImageView) findViewById(R.id.finger_picture);
		quality_score = (TextView) findViewById(R.id.quality_score);
		compare_result = (TextView) findViewById(R.id.compare_result);
		initView();
		
	}
	
	private void initView() {
		open = (Button) findViewById(R.id.open);
		getFingerDetect = (Button) findViewById(R.id.getFingerDetect);
		getFingerDetect.setEnabled(false);
		getFingerPicture = (Button) findViewById(R.id.getFingerPicture);
		getFingerPicture.setEnabled(false);
		getFingerByte = (Button) findViewById(R.id.getFingerByte);
		getFingerByte.setEnabled(false);
		compareFinger = (Button) findViewById(R.id.compareFinger);
		compareFinger.setEnabled(false);
		getQualityScore = (Button) findViewById(R.id.getQualityScore);
		getQualityScore.setEnabled(false);
		close = (Button) findViewById(R.id.close);
		close.setEnabled(false);
	}
	
	public void open(View view) {
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				fingerReader = FingerReader.getInstance(OnlyFinger.this);
				openStatus = fingerReader.openFingerReader();
				runOnUiThread(new Runnable() {
					public void run() {
						if(openStatus)
							hasInit();
					}
				});
			}
		}).start();
	}
	
	public void getFingerDetect(View view) {
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				fingerReader.getFingerDetect(reader_view);
				runOnUiThread(new Runnable() {
					public void run() {
						hasOpenDetect();
					}
				});
			}
		}).start();
	}
	
	public void getFingerPicture(View view) {
		finger_picture.setImageBitmap(fingerReader.getFingerBitmap());
	}
	
	public void getFingerByte(View view) {
		fingerByte = fingerReader.getReaderFingerByte();
	}
	
	public void compareFinger(View view) {

		beforeCompare();
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				final int ret = fingerReader.fingerprintCompare(new byte[1024],fingerReader.getReaderFingerByte(), (byte) 80);
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							// TODO Auto-generated method stub
							if(ret == 1)
								compare_result.setText("匹配成功");
							else if (ret == 2)
								compare_result.setText("匹配失败");
							else
								compare_result.setText("待匹配指纹质量低于指定值或模板指纹错误");
							afterCompare();
						}
					});
			}
		}).start();
	}
	
	public void getQualityScore(View view) {
		getting_score = true;
		if(!scoreThreadHasBuild) {
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					// TODO Auto-generated method stub
					while(getting_score) {
						try {
							Thread.sleep(50);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						final int score = fingerReader.getQualityScore();
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								// TODO Auto-generated method stub
								if(getting_score)
									quality_score.setText("score:"+score);
							}
						});
					}
				}
			}).start();
		}
		scoreThreadHasBuild = true;
	}
	
	public void close(View view) {
		getting_score = false;
		scoreThreadHasBuild = false;
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				fingerReader.closeFingerReader();
				runOnUiThread(new Runnable() {
					public void run() {
						hasClosed();
					}
				});
			}
		}).start();
	}
	
	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		getting_score = false;
		scoreThreadHasBuild = false;
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				if(fingerReader != null) {
					fingerReader.closeFingerReader();
				}
			}
		}).start();
		handler.sendEmptyMessageDelayed(1, 1000);
	}
	
	private void hasInit() {
		open.setEnabled(false);
		getFingerDetect.setEnabled(true);
		getFingerPicture.setEnabled(false);
		getFingerByte.setEnabled(false);
		compareFinger.setEnabled(false);
		getQualityScore.setEnabled(false);
		close.setEnabled(true);
	}
	
	private void hasOpenDetect() {
		open.setEnabled(false);
		getFingerDetect.setEnabled(false);
		getFingerPicture.setEnabled(true);
		getFingerByte.setEnabled(true);
		compareFinger.setEnabled(true);
		getQualityScore.setEnabled(true);
		close.setEnabled(true);
	}
	
	private void hasClosed() {
		open.setEnabled(true);
		getFingerDetect.setEnabled(false);
		getFingerPicture.setEnabled(false);
		getFingerByte.setEnabled(false);
		compareFinger.setEnabled(false);
		getQualityScore.setEnabled(false);
		close.setEnabled(false);
		quality_score.setText("score:");
		compare_result.setText("指纹比对结果：");
		reader_view.setImageResource(R.drawable.logo);
		finger_picture.setImageResource(R.drawable.logo);
	}
	
	private void beforeCompare() {
		open.setEnabled(false);
		getFingerDetect.setEnabled(false);
		getFingerPicture.setEnabled(false);
		getFingerByte.setEnabled(false);
		compareFinger.setEnabled(false);
		getQualityScore.setEnabled(false);
		close.setEnabled(false);
	}
	
	private void afterCompare() {
		open.setEnabled(false);
		getFingerDetect.setEnabled(false);
		getFingerPicture.setEnabled(true);
		getFingerByte.setEnabled(true);
		compareFinger.setEnabled(true);
		getQualityScore.setEnabled(true);
		close.setEnabled(true);
	}
	
	public void openfinger(View view) {
		if(FingerPrint.fingerPrintPower(1))
			compare_result.setText("open success");
		else
			compare_result.setText("open failed");
	}
	
	public void fingerclose(View view) {
		if(FingerPrint.fingerPrintPower(0))
			compare_result.setText("close success");
		else
			compare_result.setText("close failed");
	}
}
