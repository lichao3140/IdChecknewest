package com.telpo.idcheck;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.google.zxing.other.BeepManager;
import com.telpo.tps550.api.TelpoException;
import com.telpo.tps550.api.idcard.CountryMap;
import com.telpo.tps550.api.idcard.IdCard;
import com.telpo.tps550.api.idcard.IdentityInfo;
import com.telpo.tps550.api.idcard.UsbIdCard;
import com.telpo.tps550.api.idcard.Utils;
import com.telpo.tps550.api.serial.Serial;
import com.telpo.tps550.api.util.ReaderUtils;
import com.telpo.tps550.api.util.StringUtil;
import com.telpo.tps550.api.util.SystemUtil;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SerialIdCardActivity extends Activity {
	private TextView idcardInfo, hint_view, def_view, test_info_view,time_view;
	private ImageView imageView1, imageView2;
	private Button btn_read_once, btn_read_circle;
	private IdentityInfo info;
	private SoundPool soundPool;
    private int music;
	Bitmap bitmap;
	byte[] image;
	View info_view;
	CountryMap countryMap = CountryMap.getInstance();
	byte[] fringerprint;
	private String fringerprintData;
	private static boolean isCardPressing = false;
	private boolean hasReader = false;
	private boolean isFinish = false;
	private boolean isCount = false;
	private int count_time = 0;
	private boolean isCircle = false;
	private boolean hasCircle = false;
	private int success = 0;
	private int failCount = 0;
	private GetIDInfoTask mAsyncTask;
	private Timer timer = new Timer();
	private TimerTask task;
	private static long play_time = 0;
	
	private boolean isSerialOpen = false;
	private boolean isOnce = false;
	private boolean finishSign = false;
	
	private LinearLayout cellphone_view;
	private ImageView people_view;
	private TextView cellphone_showData;
	private boolean isCellPhone = false;
	
	private long start_time = 0L;
	private long end_time = 0L;

	private Handler handler1 = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			if (isFinish) {
				return;
			}
			switch (msg.what) {
			case 1:
				mAsyncTask = new GetIDInfoTask();
				mAsyncTask.execute();
				break;
			case 2:
				String text = hint_view.getText().toString();
				int index = text.indexOf("\n");
				if (index != -1) {
					text = text.substring(0, index);
				}
				break;
			case 3:
				// 超时
				if(isCellPhone) {
					//finger_view.setVisibility(View.GONE);
				}else {
					imageView2.setVisibility(View.INVISIBLE);
				}
				isCardPressing = false;
				if (handler2.hasMessages(1)) {
					handler2.removeMessages(1);
				}
				hint_view.setText(getString(R.string.idcard_qfk));
				test_info_view.setText(getString(R.string.idcard_cscs) 
						+ count_time + "  " + getString(R.string.idcard_cgcs)+ success);
				/*test_info_view.setText(getString(R.string.idcard_cscs) 
						+ count_time + "  " + getString(R.string.idcard_cgcs)+ (count_time-failCount)+"  失败次数"+failCount);*/
				showDefaultView(getString(R.string.idcard_dqsbhcs));
				handler1.sendMessage(handler1.obtainMessage(1, ""));
				break;
			case 4:
				btn_read_once.setEnabled(true);
				btn_read_circle.setEnabled(true);
				finish();
				startActivity(new Intent(SerialIdCardActivity.this,SerialIdCardActivity.class));
				break;
			}
			super.handleMessage(msg);
		}
	};
	private Handler handler2 = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			if (isFinish) {
				return;
			}
			if (!isCardPressing) {
				return;
			}
			switch (msg.what) {
			case 1:
				if(!isCellPhone){
					imageView2.setVisibility(View.INVISIBLE);
				}
				break;
			case 2:
				// Matchingsucceeded
				if (!isCount && isCircle) {
					if (handler2.hasMessages(1)) {
						handler2.removeMessages(1);
					}
					return;
				}
				if(!isCellPhone){
					imageView2.setVisibility(View.VISIBLE);
				}
				//playSound(3);
				hint_view.setText(getString(R.string.fprint_ppcg));
				isCount = false;
				fringerprint = null;
				if(!isCircle) {
					btn_read_once.setEnabled(true);
					btn_read_circle.setEnabled(true);
				}
				break;
			case -2:
				// Matchingfailed
				if (!isCount && isCircle) {
					handler2.sendMessageDelayed(handler2.obtainMessage(1, ""), 0);
					return;
				}
				hint_view.setText(getString(R.string.fprint_ppsb));
				fringerprint = null;
				if(!isCircle) {
					btn_read_once.setEnabled(true);
					btn_read_circle.setEnabled(true);
				}else {
					handler2.sendMessageDelayed(handler2.obtainMessage(1, ""), 0);
				}
				break;
			case 3:
				// founduvcdevice
				Log.e("sss", "founduvcdevice");
				break;
			case -3:
				// notfounduvcdevice
				hint_view.setText(getString(R.string.fprint_wzwy));
				Log.e("sss", "notfounduvcdevice");
				break;
			case -13:
				// outoftime
				if(!isCellPhone){
					imageView2.setVisibility(View.INVISIBLE);
				}
				handler2.sendMessageDelayed(handler2.obtainMessage(1, ""), 500);
				break;
			case -1:
				finish();
				break;
			}
			super.handleMessage(msg);
		}
	};

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.idcard_main);
		
		if(getScreenWidth(this)<getRealHeight(this)) {
			isCellPhone = true;
		}
		
		idcardInfo = (TextView) findViewById(R.id.showData);
		time_view = (TextView) findViewById(R.id.time_view);
		imageView1 = (ImageView) findViewById(R.id.imageView1);
		imageView2 = (ImageView) findViewById(R.id.imageView2);
		hint_view = (TextView) findViewById(R.id.hint_view);
		info_view = findViewById(R.id.relativeLayout);
		def_view = (TextView) findViewById(R.id.default_view);
		test_info_view = (TextView) findViewById(R.id.test_info_view);
		btn_read_once = (Button) findViewById(R.id.read_once);
		btn_read_circle = (Button) findViewById(R.id.read_circle);
		
		cellphone_view = (LinearLayout) findViewById(R.id.cellphone_view);
		people_view = (ImageView) findViewById(R.id.people_view);
		cellphone_showData = (TextView) findViewById(R.id.cellphone_showData);
		cellphone_showData.setVisibility(View.VISIBLE);
		if(isCellPhone) {
			cellphone_view.setVisibility(View.VISIBLE);
		}
		
		btn_read_once.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				showDefaultView("");
				count_time = 0;
				success = 0;
				failCount = 0;
				btn_read_once.setEnabled(false);
				btn_read_circle.setEnabled(false);
				isOnce = true;
				if (SystemUtil.getDeviceType() == StringUtil.DeviceModelEnum.TPS350.ordinal()) {
					if (checkPackage("com.example.idcard.hid")) {
						switchToTPS350AUsbIdCard();
					} else {
						Toast.makeText(SerialIdCardActivity.this, getString(R.string.idcard_not_find_usb_demo),
								Toast.LENGTH_SHORT).show();
					}
				} else {
					handler1.sendMessage(handler1.obtainMessage(1, ""));
				}
			}
		});
		
		btn_read_circle.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				isCircle = !isCircle;
				if (isCircle) {
					hasCircle = true;
					showDefaultView("");
					count_time = 0;
					success = 0;
					failCount = 0;
					btn_read_circle.setText(getString(R.string.idcard_stop_read));
					btn_read_once.setEnabled(false);
					if (SystemUtil.getDeviceType() == StringUtil.DeviceModelEnum.TPS350.ordinal()) {
						if (checkPackage("com.example.idcard.hid")) {
							switchToTPS350AUsbIdCard();
						} else {
							Toast.makeText(SerialIdCardActivity.this, getString(R.string.idcard_not_find_usb_demo),
									Toast.LENGTH_SHORT).show();
						}
					} else {
						handler1.sendMessageDelayed(handler1.obtainMessage(1, ""), 0);
					}
				} else {
					if(handler1.hasMessages(1)) {
						handler1.removeMessages(1);
					}
					btn_read_circle.setText(getString(R.string.idcard_start_read));
					if(isCellPhone) {
						people_view.setVisibility(View.GONE);
					}else {
						imageView1.setVisibility(View.GONE);
					}
					info_view.setVisibility(View.GONE);
					Message msg = new Message();
					msg.what = 4;
					handler1.sendMessageDelayed(msg, 1500);
					btn_read_once.setEnabled(false);
					btn_read_circle.setEnabled(false);
				}
			}
		});
		
		initVoice2();

		test_info_view.setText(getString(R.string.idcard_cscs) 
				+ count_time + "  " + getString(R.string.idcard_cgcs)+ success);
		/*test_info_view.setText(getString(R.string.idcard_cscs) 
				+ count_time + "  " + getString(R.string.idcard_cgcs)+ (count_time-failCount)+"  失败次数"+failCount);*/
		detectUsbDeviceAttach();
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		isFinish = false;
		finishSign = false;
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		isSerialOpen = false;
		finishSign = true;
		if (handler1.hasMessages(1)) {
			handler1.removeMessages(1);
		}
		if (handler2.hasMessages(1)) {
			handler2.removeMessages(1);
		}
		isFinish = true;
		isCount = false;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		isCardPressing = false;
		unregisterReceiver(mUsbReceiver);
	}

	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		
		//IdCard.close();
		isSerialOpen = false;
		
		if (handler1.hasMessages(1)) {
			handler1.removeMessages(1);
		}
		if (handler2.hasMessages(1)) {
			handler2.removeMessages(1);
		}
		super.onBackPressed();
			
	}

	private class GetIDInfoTask extends AsyncTask<Void, Integer, TelpoException> {
		
		private ProgressDialog dialog;
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			count_time++;
			info = null;
			bitmap = null;
			fringerprintData = "";
			fringerprint = null;
			if(isCellPhone) {
				people_view.setVisibility(View.GONE);
				cellphone_showData.setText("");
			}else {
				imageView1.setVisibility(View.GONE);
				idcardInfo.setText("");
			}
			start_time = System.currentTimeMillis();
		}

		@Override
		protected TelpoException doInBackground(Void... arg0) {
			if(SystemUtil.getDeviceType() == StringUtil.DeviceModelEnum.TPS900.ordinal()) {
				return new TelpoException();
			}
			TelpoException result = null;
			try {
				if(!isSerialOpen || finishSign) {
					//IdCard.open(SerialIdCardActivity.this);
					IdCard.open(115200,"/dev/ttyS3");
					isSerialOpen = true;
				}
				info =  IdCard.checkIdCard(2000);
				if ("".equals(info.getName())) {
					return new TelpoException();
				}
				image = IdCard.getIdCardImage();
				bitmap = IdCard.decodeIdCardImage(image);
				//bitmap = UsbIdCard.decodeIdCardImage(image);
				if (!"I".equals(info.getCard_type())) {
					// luyq add 增加指纹信息 中国籍才有指纹
					fringerprint = IdCard.getFringerPrint();
					fringerprintData = ReaderUtils.get_finger_info(SerialIdCardActivity.this, fringerprint);
				}
			} catch (TelpoException e) {
				e.printStackTrace();
				result = e;
			} finally {
				//IdCard.close();
			}
			
			
			return result;
		}

		@Override
		protected void onPostExecute(TelpoException result) {
			super.onPostExecute(result);
			if (result == null) {
				//count_time++;
				success++;
				showReadTime();
				if(finishSign) {
					IdCard.close();
				}
				if(!info.getName().contains("timeout")) {
					play_voice();
					if(isCellPhone) {
						people_view.setVisibility(View.VISIBLE);
						imageView1.setVisibility(View.GONE);
						if ("I".equals(info.getCard_type())) {
							// luyq add 20170823 增加外籍身份证信息显示
							cellphone_showData.setText(getString(R.string.idcard_xm) + info.getName() + "\n"
									+ getString(R.string.idcard_cn_name) + info.getCn_name() + "\n" // Chinese name
									+ getString(R.string.idcard_xb) + info.getSex() + "\n" + getString(R.string.idcard_csrq)
									+ info.getBorn() + "\n" + getString(R.string.idcard_country)
									+ countryMap.getCountry(info.getCountry()) + " / " + info.getCountry() + "\n"
									+ getString(R.string.idcard_yxqx) + info.getPeriod() + "\n"
									+ getString(R.string.idcard_qzjg) + info.getApartment() + "\n"
									+ getString(R.string.idcard_sfhm) + info.getNo() + "\n"
									+ getString(R.string.idcard_version) + info.getIdcard_version() + "\n");
						} else {
							cellphone_showData.setText(getString(R.string.idcard_xm) + info.getName() + "\n"
									+ getString(R.string.idcard_xb) + info.getSex() + "\n" + getString(R.string.idcard_mz)
									+ info.getNation() + "\n" + getString(R.string.idcard_csrq) + info.getBorn() + "\n"
									+ getString(R.string.idcard_dz) + info.getAddress() + "\n"
									+ getString(R.string.idcard_sfhm) + info.getNo() + "\n" + getString(R.string.idcard_qzjg)
									+ info.getApartment() + "\n" + getString(R.string.idcard_yxqx) + info.getPeriod() + "\n"
									+ getString(R.string.idcard_zwxx) + fringerprintData);
						}
						people_view.setImageBitmap(bitmap);
					}else {
						imageView1.setVisibility(View.VISIBLE);
						if ("I".equals(info.getCard_type())) {
							// luyq add 20170823 增加外籍身份证信息显示
							idcardInfo.setText(getString(R.string.idcard_xm) + info.getName() + "\n"
									+ getString(R.string.idcard_cn_name) + info.getCn_name() + "\n" // Chinese name
									+ getString(R.string.idcard_xb) + info.getSex() + "\n" + getString(R.string.idcard_csrq)
									+ info.getBorn() + "\n" + getString(R.string.idcard_country)
									+ countryMap.getCountry(info.getCountry()) + " / " + info.getCountry() + "\n"
									+ getString(R.string.idcard_yxqx) + info.getPeriod() + "\n"
									+ getString(R.string.idcard_qzjg) + info.getApartment() + "\n"
									+ getString(R.string.idcard_sfhm) + info.getNo() + "\n"
									+ getString(R.string.idcard_version) + info.getIdcard_version() + "\n");
						} else {
							idcardInfo.setText(getString(R.string.idcard_xm) + info.getName() + "\n"
									+ getString(R.string.idcard_xb) + info.getSex() + "\n" + getString(R.string.idcard_mz)
									+ info.getNation() + "\n" + getString(R.string.idcard_csrq) + info.getBorn() + "\n"
									+ getString(R.string.idcard_dz) + info.getAddress() + "\n"
									+ getString(R.string.idcard_sfhm) + info.getNo() + "\n" + getString(R.string.idcard_qzjg)
									+ info.getApartment() + "\n" + getString(R.string.idcard_yxqx) + info.getPeriod() + "\n"
									+ getString(R.string.idcard_zwxx) + fringerprintData);
						}
						imageView1.setImageBitmap(bitmap);
					}
					showInfoView();
					isCardPressing = true;
				}
				
				if (isCircle) {
					handler1.sendEmptyMessageDelayed(1, 100);
				}else {
					if(isOnce) {
						btn_read_once.setEnabled(true);
						btn_read_circle.setEnabled(true);
						isOnce = false;
					}else {
						
					}
				}
			} else {
				failCount++;
				if (isCircle) {
					handler1.sendMessageDelayed(handler1.obtainMessage(1, ""), 100);
				} else {
					if(!hasCircle) {
						//playSound(5);
					}
					hasCircle = false;
					btn_read_once.setEnabled(true);
					btn_read_circle.setEnabled(true);
					showDefaultView("");
				}
			}
			test_info_view.setText(getString(R.string.idcard_cscs) 
					+ count_time + "  " + getString(R.string.idcard_cgcs)+ success);
			/*test_info_view.setText(getString(R.string.idcard_cscs) 
					+ count_time + "  " + getString(R.string.idcard_cgcs)+ (count_time-failCount)+"  失败次数"+failCount);*/
		}
	}

	private void switchToTPS350AUsbIdCard() {
		Intent intent = new Intent();
		intent.setClassName("com.example.idcard.hid", "com.example.demo.MiniIDReaderActivityNew");
		startActivity(intent);
	}

	private boolean checkPackage(String packageName) {
		PackageManager manager = this.getPackageManager();
		Intent intent = new Intent().setPackage(packageName);
		List<ResolveInfo> infos = manager.queryIntentActivities(intent, PackageManager.GET_INTENT_FILTERS);
		if (infos == null || infos.size() < 1) {
			return false;
		}
		return true;
	}

	private void detectUsbDeviceAttach() {
		// listen usb device attach
		IntentFilter usbDeviceStateFilter = new IntentFilter();

		usbDeviceStateFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
		usbDeviceStateFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		usbDeviceStateFilter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
		usbDeviceStateFilter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);

		registerReceiver(mUsbReceiver, usbDeviceStateFilter);
	}

	private void showDefaultView(String content) {
		info_view.setVisibility(View.GONE);
		if(isCellPhone) {
			people_view.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));
			cellphone_showData.setText("");
		}else {
			imageView1.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));
			idcardInfo.setText("");
		}
		def_view.setVisibility(View.VISIBLE);
		def_view.setText(content);
	}

	private void showInfoView() {
		
		info_view.setVisibility(View.VISIBLE);
		def_view.setVisibility(View.GONE);
		def_view.setText("");
	}

	BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			String curItentActionName = intent.getAction();

			if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(curItentActionName)) {
				if (!isCardPressing) {
					return;
				}
				String path = "/dev/video";
				for (int i = 0; i < 15; i++) {
					Utils.upgradeRootPermission(path + i);
				}
			}
		}
	};
	
	public void initVoice2(){
        soundPool= new SoundPool(10, AudioManager.STREAM_MUSIC, 2);//第一个参数为同时播放数据流的最大个数，第二数据流类型，第三为声音质量
        music = soundPool.load(SerialIdCardActivity.this, R.raw.beep, 1); //把你的声音素材放到res/raw里，第2个参数即为资源文件，第3个为音乐的优先级
    }
	
	public void play_voice() {
        soundPool.play(music, 1, 1, 0, 0, 1);
    }
	
	public static int getScreenWidth(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(dm);
        return dm.widthPixels;
    }

    public static int getRealHeight(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        int screenHeight = 0;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            DisplayMetrics dm = new DisplayMetrics();
            display.getRealMetrics(dm);
            screenHeight = dm.heightPixels;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            try {
                screenHeight = (Integer) Display.class.getMethod("getRawHeight").invoke(display);
            } catch (Exception e) {
                DisplayMetrics dm = new DisplayMetrics();
                display.getMetrics(dm);
                screenHeight = dm.heightPixels;
            }
        }
        return screenHeight;
    }
    
    private void showReadTime() {
    	end_time = System.currentTimeMillis();
		long runTime = end_time - start_time;
		time_view.setText(String.format("读卡时间 %d ms", runTime));
    }
}