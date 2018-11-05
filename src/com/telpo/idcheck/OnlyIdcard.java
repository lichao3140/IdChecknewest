package com.telpo.idcheck;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import com.telpo.tps550.api.TelpoException;
import com.telpo.tps550.api.idcard.CountryMap;
import com.telpo.tps550.api.idcard.IdCard;
import com.telpo.tps550.api.idcard.IdentityInfo;
import com.zkteco.android.IDReader.IDPhotoHelper;
import com.zkteco.android.IDReader.WLTService;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class OnlyIdcard extends Activity {
	private TextView idcardInfo, hint_view, def_view, test_info_view;
	private ImageView imageView1, imageView2;
	private Button getPermission, btn_read_once, btn_read_circle,read_version;
	private IdentityInfo info;
	private SoundPool soundPool;
	private Bitmap bitmap;
	private CountryMap countryMap = CountryMap.getInstance();
	private View info_view;
	private byte[] image;
	private byte[] fringerprint;
	private String fringerprintData;
	private PendingIntent mPermissionIntent;// luyq
	private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
	private UsbManager mUsbManager;
	private UsbDevice idcard_reader;
	private int contentLength, imageDatalength, fplength;
	private GetIDInfoTask mAsyncTask;
	private static boolean isCardPressing = false;
	private boolean hasPermission = false;
	private boolean isUsingUsb = false;
	private boolean hasReader = false;
	private boolean isFinish = false;
	private boolean isCount = false;
	private boolean isCircle = false;
	private boolean hasCircle = false;
	private int count_time = 0;
	private int success = 0;
	private Timer timer = new Timer();
	private TimerTask task;
	private static long play_time = 0;
    private boolean isStop = false;
    private boolean pressBack = false;
    
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
				hint_view.setText(text + "\n" + getString(R.string.idcard_djs) + msg.arg1);
				break;
			case 3:
				// 超时
				//imageView2.setVisibility(View.INVISIBLE);
				isCardPressing = false;
				if (handler2.hasMessages(1)) {
					handler2.removeMessages(1);
				}
				hint_view.setText(getString(R.string.idcard_qfk));
				test_info_view
						.setText(getString(R.string.idcard_cscs) + count_time + "  " + getString(R.string.idcard_cgcs)
								+ success);
				showDefaultView(getString(R.string.idcard_dqsbhcs));
				handler1.sendMessage(handler1.obtainMessage(1, ""));
				break;
			}
			super.handleMessage(msg);
		}
	};

	private Handler handler2 = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			if (isFinish) {
				Log.e("sss", "return1");
				return;
			}
			if (!isCardPressing) {
				Log.e("sss", "return2");
				return;
			}
			switch (msg.what) {
			case 1:
				//imageView2.setVisibility(View.INVISIBLE);
				break;
			case 2:
				// Matchingsucceeded
				if (!isCount && isCircle) {
					if (handler2.hasMessages(1)) {
						handler2.removeMessages(1);
					}
					return;
				}
				imageView2.setVisibility(View.VISIBLE);
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
				//playSound(2);
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
				Log.e("sss", "notfounduvcdevice");
				break;
			case -13:
				// outoftime
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
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);//设置窗体始终点亮
		setContentView(R.layout.idcard_usb);
		
		getPermission = (Button) findViewById(R.id.requestPermission);
		idcardInfo = (TextView) findViewById(R.id.showData);
		imageView1 = (ImageView) findViewById(R.id.imageView1);
		imageView2 = (ImageView) findViewById(R.id.imageView2);
		info_view = findViewById(R.id.relativeLayout);
		hint_view = (TextView) findViewById(R.id.hint_view);
		def_view = (TextView) findViewById(R.id.default_view);
		test_info_view = (TextView) findViewById(R.id.test_info_view);

		soundPool = new SoundPool(10, AudioManager.STREAM_SYSTEM, 5);
		soundPool.load(this, R.raw.read_card, 1);
		soundPool.load(this, R.raw.verify_fail, 2);
		soundPool.load(this, R.raw.verify_success, 3);
		soundPool.load(this, R.raw.please_verify, 4);

		getPermission.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				openUsbIdCard();
			}
		});

		btn_read_once = (Button) findViewById(R.id.read_once);
		btn_read_once.setVisibility(View.VISIBLE);
		btn_read_once.setEnabled(false);
		btn_read_once.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				isCircle = false;
				showDefaultView("");
				btn_read_once.setEnabled(false);
				btn_read_circle.setEnabled(false);
				handler1.sendMessage(handler1.obtainMessage(1, ""));
			}
		});

		btn_read_circle = (Button) findViewById(R.id.read_circle);
		btn_read_circle.setVisibility(View.VISIBLE);
		btn_read_circle.setEnabled(false);
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
					btn_read_circle.setText(getString(R.string.idcard_stop_read));
					btn_read_once.setEnabled(false);
					handler1.sendMessage(handler1.obtainMessage(1, ""));
				} else {
					btn_read_circle.setText(getString(R.string.idcard_start_read));
					finish();
					Intent intent = new Intent(OnlyIdcard.this, OnlyIdcard.class);
					startActivity(intent);
				}
			}
		});
		
		read_version = (Button) findViewById(R.id.read_version);
		read_version.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				
			}
		});

		mPermissionIntent = PendingIntent.getBroadcast(OnlyIdcard.this, 0, new Intent(ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_USB_PERMISSION);
		registerReceiver(mReceiver, filter);
		
		// luyq add 20170904 end
		test_info_view.setText(getString(R.string.idcard_cscs) + count_time + "  " + getString(R.string.idcard_cgcs)
				+ success);
	
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		pressBack = false;
		ShellUtils.execCommand("echo 3 > /sys/class/telpoio/power_status", false);//usb get charge
		getPermission.setVisibility(View.VISIBLE);
		def_view.setText(getString(R.string.idcard_qhqqx));
		hint_view.setText("");
		isFinish = false;
		
		try {
			IdCard.open(IdCard.IDREADER_TYPE_USB, OnlyIdcard.this);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		if (handler1.hasMessages(1)) {
			handler1.removeMessages(1);
		}
		if (handler2.hasMessages(1)) {
			handler2.removeMessages(1);
		}
		ShellUtils.execCommand("echo 4 > /sys/class/telpoio/power_status", false);
		isCount = false;
		isStop = true;
		isFinish = true;
		if(isCircle && !pressBack) {
			finish();
			Intent intent = new Intent(OnlyIdcard.this, OnlyIdcard.class);
			startActivity(intent);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		isCardPressing = false;
		isCount = false;
		unregisterReceiver(mReceiver);//shut down broadcastreceiver
	}

	private class GetIDInfoTask extends AsyncTask<Void, Integer, Boolean> {

		@Override
		protected void onPreExecute() {//before thread
			super.onPreExecute();
			info = null;
			bitmap = null;
			fringerprintData = "";
			fringerprint = null;
			isStop = false;
			isCount = false;
			idcardInfo.setText("");
			imageView1.setVisibility(View.GONE);
		}

		@Override
		protected Boolean doInBackground(Void... arg0) {//in thread

				try {
					info = IdCard.checkIdCard(1000);
				} catch (TelpoException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			
			if (info != null) {
				if("".equals(info.getName())) {
					return false;
				}
				image = info.getHead_photo();
				
				if ("I".equals(info.getCard_type())) {
					if(image.length != 1024) {
						return false;
					}
				}else {
					if(image.length == 2048 || image.length == 1024) {
					}else {
						return false;
					}
				}
				
				fringerprint = Arrays.copyOfRange(image, imageDatalength, image.length);
				fringerprintData = getFingerInfo(fringerprint);
				return true;
			}
			return false;
		}

		@Override
		protected void onPostExecute(Boolean result) {//after thread
			super.onPostExecute(result);
			if (result) {
				// beepManager.playBeepSoundAndVibrate();
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
				byte[] buf = new byte[WLTService.imgLength];
				if (1 == WLTService.wlt2Bmp(image, buf)) {
					imageView1.setImageBitmap(IDPhotoHelper.Bgr2Bitmap(buf));
					imageView1.setVisibility(View.VISIBLE);
				}
				showInfoView();//show another view
				isCardPressing = true;
				isCount = false;
				success++;
				if (isCircle) {
					handler1.sendEmptyMessageDelayed(1, 100);
				}else {
					btn_read_once.setEnabled(true);
					btn_read_circle.setEnabled(true);
				}
				count_time++;
			} else {
				if (isCircle) {
					isCount = false;
					handler1.sendMessageDelayed(handler1.obtainMessage(1, ""), 100);
				} else {
					hasCircle = false;
					btn_read_once.setEnabled(true);
					btn_read_circle.setEnabled(true);
					showDefaultView("");
				}
				count_time++;
			}
			test_info_view
			.setText(getString(R.string.idcard_cscs) + count_time + "  " + getString(R.string.idcard_cgcs)
					+ success);
		}
	}

	private String GetFingerName(int fingerPos) {
		String fingerName = "";
		switch (fingerPos) {
		case 11:
			// 右手拇指
			fingerName = getString(R.string.idcard_ysmz);
			break;
		case 12:
			// 右手食指
			fingerName = getString(R.string.idcard_yssz);
			break;
		case 13:
			// 右手中指
			fingerName = getString(R.string.idcard_yszz);
			break;
		case 14:
			// 右手环指
			fingerName = getString(R.string.idcard_yshz);
			break;
		case 15:
			// 右手小指
			fingerName = getString(R.string.idcard_ysxz);
			break;
		case 16:
			// 左手拇指
			fingerName = getString(R.string.idcard_zsmz);
			break;
		case 17:
			// 左手食指
			fingerName = getString(R.string.idcard_zssz);
			break;
		case 18:
			// 左手中指
			fingerName = getString(R.string.idcard_zszz);
			break;
		case 19:
			// 左手环指
			fingerName = getString(R.string.idcard_zshz);
			break;
		case 20:
			// 左手小指
			fingerName = getString(R.string.idcard_zsxz);
			break;
		case 97:
			// 右手不确定指位
			fingerName = getString(R.string.idcard_ysbqdzw);
			break;
		case 98:
			// 左手不确定指位
			fingerName = getString(R.string.idcard_zsbqdzw);
			break;
		case 99:
			// 其他不确定指位
			fingerName = getString(R.string.idcard_qtbqdzw);
			break;
		default:
			// 指位未知
			fingerName = getString(R.string.idcard_zwwz);
			break;
		}
		return fingerName;
	}

	// 第5字节为注册结果代码，0x01-注册成功，0x02--注册失败, 0x03--未注册, 0x09--未知
	private String GetFingerStatus(int fingerStatus) {
		String fingerStatusName = "";
		switch (fingerStatus) {
		case 0x01:
			// 注册成功
			fingerStatusName = getString(R.string.idcard_zccg);
			break;
		case 0x02:
			// 注册失败
			fingerStatusName = getString(R.string.idcard_zcsb);
			break;
		case 0x03:
			// 未注册
			fingerStatusName = getString(R.string.idcard_wzc);
			break;
		case 0x09:
			// 注册状态未知
			fingerStatusName = getString(R.string.idcard_zcztwz);
			break;
		default:
			// 注册状态未知
			fingerStatusName = getString(R.string.idcard_zcztwz);
			break;
		}
		return fingerStatusName;
	}

	private String getFingerInfo(byte[] fpData) {
		// 解释第1枚指纹，总长度512字节，部分数据格式：
		// 第1字节为特征标识'C'
		// 第5字节为注册结果代码，0x01-注册成功，0x02--注册失败, 0x03--未注册, 0x09--未知
		// 第6字节为指位代码
		// 第7字节为指纹质量值，0x00表示未知，1~100表示质量值
		// 第512字节 crc8值
		String fingerInfo = "";
		if (fpData != null && fpData.length == 1024 && fpData[0] == 'C') {
			fingerInfo = fingerInfo + GetFingerName(fpData[5]);

			if (fpData[4] == 0x01) {
				fingerInfo = fingerInfo + " " + getString(R.string.idcard_zwzl) + String.valueOf(fpData[6]);
			} else {
				fingerInfo = fingerInfo + GetFingerStatus(fpData[4]);
			}

			fingerInfo = fingerInfo + "  ";
			if (fpData[512] == 'C') {
				fingerInfo = fingerInfo + GetFingerName(fpData[512 + 5]);

				if (fpData[512 + 4] == 0x01) {
					fingerInfo = fingerInfo + " " + getString(R.string.idcard_zwzl) + String.valueOf(fpData[512 + 6]);
				} else {
					fingerInfo = fingerInfo + GetFingerStatus(fpData[512 + 4]);
				}
			}
		} else {
			fingerInfo = getString(R.string.idcard_wdqhbhzw);
		}

		return fingerInfo;
	}

	public void openUsbIdCard() {
		isUsingUsb = true;
		mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		HashMap<String, UsbDevice> deviceHashMap = mUsbManager.getDeviceList();
		Iterator<UsbDevice> iterator = deviceHashMap.values().iterator();
		hasReader = false;
		test_info_view.setVisibility(View.VISIBLE);
		while (iterator.hasNext()) {
			UsbDevice usbDevice = iterator.next();
			int pid = usbDevice.getProductId();
			int vid = usbDevice.getVendorId();
			if ((pid == IdCard.READER_PID_SMALL && vid == IdCard.READER_VID_SMALL) 
					|| (pid == IdCard.READER_PID_BIG && vid == IdCard.READER_VID_BIG)) {
				hasReader = true;
				idcard_reader = usbDevice;
				if (mUsbManager.hasPermission(usbDevice)) {
					getPermission.setVisibility(View.INVISIBLE);
					def_view.setText(getString(R.string.idcard_hqqxcg));
					hasPermission = true;
					btn_read_circle.setEnabled(true);
					btn_read_once.setEnabled(true);
					break;
				} else {
					//get permission
					getPermission.setVisibility(View.VISIBLE);
					def_view.setText(getString(R.string.idcard_qhqqx));
					hasPermission = false;
					mUsbManager.requestPermission(usbDevice, mPermissionIntent);//guding de
				}
			}
		}
		isUsingUsb = false;
		if (!hasReader) {
			hint_view.setText(getString(R.string.idcard_wfljdkq));
			if (handler1.hasMessages(1)) {
				handler1.removeMessages(1);
			}
		}
	}

	private void showDefaultView(String content) {
		info_view.setVisibility(View.GONE);
		imageView1.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));
		imageView1.setVisibility(View.VISIBLE);
		idcardInfo.setText("");
		def_view.setVisibility(View.VISIBLE);
		def_view.setText(content);
	}

	private void showInfoView() {
		info_view.setVisibility(View.VISIBLE);
		def_view.setVisibility(View.GONE);
		def_view.setText("");
	}

	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (ACTION_USB_PERMISSION.equals(action)) {
				synchronized (this) {
					if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {//if have permission??
						if (idcard_reader != null) {
							hasPermission = true;
							getPermission.setVisibility(View.INVISIBLE);
							//change layout
							def_view.setText(getString(R.string.idcard_hqqxcg));
							hint_view.setText(getString(R.string.idcard_qfk));
							btn_read_circle.setEnabled(true);
							btn_read_once.setEnabled(true);
						}
					} else {
						if (handler1.hasMessages(1)) {
							handler1.removeMessages(1);
						}
						hasPermission = false;
						idcard_reader = null;
						getPermission.setVisibility(View.VISIBLE);
						def_view.setText(getString(R.string.idcard_qhqqx));
						if (handler1.hasMessages(1)) {
							handler1.removeMessages(1);
						}
					}
				}
			} else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
				// openUsbIdCard();
			} else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
				// openUsbIdCard();
			}
		}
	};

	void playSound(int src) {
		if (src == 4) {
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			soundPool.play(4, 1, 1, 0, 0, 1);
			return;
		}
		if (System.currentTimeMillis() - play_time >= 2 * 1000) {
			play_time = System.currentTimeMillis();
			soundPool.play(src, 1, 1, 0, 0, 1);
		}
	}
	
	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		super.onBackPressed();
		pressBack = true;
	}
	
}