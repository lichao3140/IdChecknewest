package com.telpo.idcheck;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

import com.common.pos.api.util.posutil.PosUtil;
import com.sdt.Common;
import com.sdt.Sdtapi;
import com.sdt.UsbWelIDUtil;
import com.sdt.UsbWelIDUtil.OnUsbPermissionCallback;
import com.telpo.tps550.api.TelpoException;
import com.telpo.tps550.api.fingerprint.FingerPrint;
import com.telpo.tps550.api.idcard.CountryMap;
import com.telpo.tps550.api.idcard.FingerReader;
import com.telpo.tps550.api.idcard.IdCard;
import com.telpo.tps550.api.idcard.IdentityInfo;
import com.telpo.tps550.api.util.ReaderUtils;
import com.telpo.tps550.api.util.StringUtil;
import com.telpo.tps550.api.util.SystemUtil;
import com.telpo.typeAcheck.UsbTACardActivity;
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
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class UsbIdCardActivity extends Activity {
	private TextView idcardInfo, hint_view, def_view, test_info_view,time_view;
	private ImageView imageView1, imageView2;
	private Button getPermission, btn_read_once, btn_read_circle,read_version;
	private IdentityInfo info;
	private SoundPool soundPool;
	private SoundPool soundPool2;
	private int music;
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
	private int imageDatalength;
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
	private static long play_time = 0;
	private int failedCount = 0;
	private boolean isLeave = false;
   
    private UsbManager zk_musbManager = null;
    
    private volatile boolean isStop = false;
    private boolean hasZk = false;
    private boolean pressBack = false;
    
    private FingerReader fingerReader;
    private Common common;
    private UsbWelIDUtil mUtil;
    private Sdtapi sdta;
    
    private LinearLayout cellphone_view;
	private ImageView people_view;
	private TextView cellphone_showData;
	private boolean isCellPhone = false;
	
	private byte[] id_phy_addr = null;
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
				hint_view.setText(text + "\n" + getString(R.string.idcard_djs) + msg.arg1);
				break;
			case 3:
				// 超时
				if(fingerReader!=null)
					fingerReader.closeFingerReader();
				
				isCardPressing = false;
				if (handler2.hasMessages(1)) {
					handler2.removeMessages(1);
				}
				hint_view.setText(getString(R.string.idcard_qfk));
				/*test_info_view
						.setText(getString(R.string.idcard_cscs) + count_time + "  " + getString(R.string.idcard_cgcs)
								+ success);*/
				test_info_view.setText(getString(R.string.idcard_cscs) + count_time + "  " + getString(R.string.idcard_cgcs)
				+ (count_time-failedCount)+"  失败次数"+failedCount);
				showDefaultView(getString(R.string.idcard_dqsbhcs));
				handler1.sendMessage(handler1.obtainMessage(1, ""));
				break;
			case 401:
				Toast.makeText(UsbIdCardActivity.this, "!=null", Toast.LENGTH_SHORT).show();
				break;
			case 402:
				Toast.makeText(UsbIdCardActivity.this, "==null", Toast.LENGTH_SHORT).show();
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
				playSound(3);
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
				playSound(2);
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
		
		//LogCatHelper logCatHelper = LogCatHelper.getInstance(UsbIdCardActivity.this, Environment.getExternalStorageDirectory().getAbsolutePath());
		//logCatHelper.start();
		
		if(getScreenWidth(this)<getRealHeight(this)) {
			isCellPhone = true;
		}
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					IdCard.open(UsbIdCardActivity.this);
				} catch (TelpoException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}).start();
		
		cellphone_view = (LinearLayout) findViewById(R.id.cellphone_view);
		people_view = (ImageView) findViewById(R.id.people_view);
		cellphone_showData = (TextView) findViewById(R.id.cellphone_showData);
		cellphone_showData.setVisibility(View.VISIBLE);
		if(isCellPhone) {
			cellphone_view.setVisibility(View.VISIBLE);
		}
		
		time_view = (TextView) findViewById(R.id.time_view);
		getPermission = (Button) findViewById(R.id.requestPermission);
		idcardInfo = (TextView) findViewById(R.id.showData);
		imageView1 = (ImageView) findViewById(R.id.imageView1);
		imageView2 = (ImageView) findViewById(R.id.imageView2);
		info_view = findViewById(R.id.relativeLayout);
		hint_view = (TextView) findViewById(R.id.hint_view);
		def_view = (TextView) findViewById(R.id.default_view);
		test_info_view = (TextView) findViewById(R.id.test_info_view);

		soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 3);
		soundPool.load(this, R.raw.read_card, 1);
		soundPool.load(this, R.raw.verify_fail, 2);
		soundPool.load(this, R.raw.verify_success, 3);
		soundPool.load(this, R.raw.please_verify, 4);

		getPermission.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if(SystemUtil.getDeviceType() == StringUtil.DeviceModelEnum.TPS900.ordinal()) {
					getPermission.setVisibility(View.INVISIBLE);
					btn_read_once.setEnabled(true);
					btn_read_circle.setEnabled(true);
					hasPermission = true;
				}else {
					openUsbIdCard();
				}
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
				count_time = 0;
				success = 0;
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
					isLeave = true;
					btn_read_circle.setText(getString(R.string.idcard_start_read));
					if(SystemUtil.getDeviceType() != StringUtil.DeviceModelEnum.TPS350_4G.ordinal()
							||SystemUtil.getDeviceType() != StringUtil.DeviceModelEnum.TPS350L.ordinal()) {
						finish();
						Intent intent = new Intent(UsbIdCardActivity.this, UsbIdCardActivity.class);
						startActivity(intent);
					}
					btn_read_once.setEnabled(true);
					btn_read_circle.setEnabled(true);
					count_time = 0;
					success = 0;
				}
				
			}
		});
		
		read_version = (Button) findViewById(R.id.read_version);
		//read_version.setVisibility(View.VISIBLE);
		read_version.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				
			}
		});

		mPermissionIntent = PendingIntent.getBroadcast(UsbIdCardActivity.this, 0, new Intent(ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_USB_PERMISSION);
		registerReceiver(mReceiver, filter);
	
		// luyq add 20170904 end

		/*test_info_view.setText(getString(R.string.idcard_cscs) + count_time + "  " + getString(R.string.idcard_cgcs)
				+ success);*/
		test_info_view.setText(getString(R.string.idcard_cscs) + count_time + "  " + getString(R.string.idcard_cgcs)
		+ (count_time-failedCount)+"  失败次数"+failedCount);
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		if(SystemUtil.getDeviceType() == StringUtil.DeviceModelEnum.TPS350_4G.ordinal()
				||SystemUtil.getDeviceType() == StringUtil.DeviceModelEnum.TPS350L.ordinal()) {
			PosUtil.setFingerPrintPower(PosUtil.FINGERPRINT_POWER_ON);
		}else if(SystemUtil.getDeviceType() == StringUtil.DeviceModelEnum.TPS450.ordinal()
        		|| SystemUtil.getDeviceType() == StringUtil.DeviceModelEnum.TPS450C.ordinal()) {
        	FingerPrint.fingerPrintPower(1);
        }else{
			ShellUtils.execCommand("echo 3 > /sys/class/telpoio/power_status", false);//usb get charge
		}
		pressBack = false;
		getPermission.setVisibility(View.VISIBLE);
		def_view.setText(getString(R.string.idcard_qhqqx));
		initVoice2();
		checkZkUsb();
		openUsbIdCard();//if has permission, button hide
		hint_view.setText("");
		isFinish = false;
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
		
		if(SystemUtil.getDeviceType() == StringUtil.DeviceModelEnum.TPS350_4G.ordinal()
				||SystemUtil.getDeviceType() == StringUtil.DeviceModelEnum.TPS350L.ordinal()) {
			PosUtil.setFingerPrintPower(PosUtil.FINGERPRINT_POWER_OFF);
		} else if(SystemUtil.getDeviceType() == StringUtil.DeviceModelEnum.TPS450.ordinal()
        		|| SystemUtil.getDeviceType() == StringUtil.DeviceModelEnum.TPS450C.ordinal()) {
        	FingerPrint.fingerPrintPower(0);
        }else {
			ShellUtils.execCommand("echo 4 > /sys/class/telpoio/power_status", false);
		}
		isCount = false;
		isStop = true;
		isFinish = true;
		
		if(fingerReader!=null)
			fingerReader.closeFingerReader();
		
		if(isCircle && !pressBack) {
			finish();
			Intent intent = new Intent(UsbIdCardActivity.this, UsbIdCardActivity.class);
			startActivity(intent);
		}
		IdCard.close();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		isCardPressing = false;
		isCount = false;
		isFinish = true;
		unregisterReceiver(mReceiver);//shut down broadcastreceiver
		IdCard.close();
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
			id_phy_addr = null;
			if(isCellPhone) {
				people_view.setVisibility(View.GONE);
			}else {
				imageView1.setVisibility(View.GONE);
			}
			info_view.setVisibility(View.GONE);
			isLeave = false;
			if(SystemUtil.getDeviceType() == StringUtil.DeviceModelEnum.TPS350_4G.ordinal()
					||SystemUtil.getDeviceType() == StringUtil.DeviceModelEnum.TPS350L.ordinal()) {
				hint_view.setText("");
			}
			start_time = System.currentTimeMillis();
			count_time++;
		}

		@Override
		protected Boolean doInBackground(Void... arg0) {//in thread
			
			if(!hasPermission) {
				return false;
			}
			
			if(SystemUtil.getDeviceType() == StringUtil.DeviceModelEnum.TPS900.ordinal()) {
				try {
					IdCard.open(UsbIdCardActivity.this);
					info =  IdCard.checkIdCard(2000);
					if ("".equals(info.getName())) {
						return false;
					}
					image = IdCard.getIdCardImage();
					bitmap = IdCard.decodeIdCardImage(image);
				} catch (TelpoException e) {
					e.printStackTrace();
					return false;
				}
				return true;
			}else {
				try {
					IdCard.open(IdCard.IDREADER_TYPE_USB,UsbIdCardActivity.this);
					info = IdCard.checkIdCard(1000);
					//id_phy_addr = IdCard.getPhyAddr();
				} catch (TelpoException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
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
					try {
						fringerprint = Arrays.copyOfRange(image, 1024, image.length);
					} catch (Exception e) {
						// TODO: handle exception
						e.printStackTrace();
						return false;
					}
					fringerprintData = ReaderUtils.get_finger_info(UsbIdCardActivity.this, fringerprint);
					return true;
				}
			}
			return false;
		}

		@Override
		protected void onPostExecute(Boolean result) {//after thread
			super.onPostExecute(result);
			
			if(SystemUtil.getDeviceType() == StringUtil.DeviceModelEnum.TPS900.ordinal() && info.getName().contains("timeout")) {
				handler1.sendEmptyMessageDelayed(1, 100);
				return;
			}else {
				if (result) {
					if(isLeave) {
						return;
					}
					play_voice();
					//count_time++;
					success++;
					end_time = System.currentTimeMillis();
					long runTime = end_time - start_time;
					time_view.setText(String.format("读卡时间 %d ms", runTime));
					if(isCellPhone) {
						if (SystemUtil.getDeviceType() != StringUtil.DeviceModelEnum.TPS900.ordinal() && "I".equals(info.getCard_type())) {
							// luyq add 20170823 增加外籍身份证信息显示
							cellphone_showData.setText(getString(R.string.idcard_xm) + info.getName() + "\n"
									+ getString(R.string.idcard_cn_name) + info.getCn_name() + "\n" // Chinese name
									+ getString(R.string.idcard_xb) + info.getSex() + "\n" + getString(R.string.idcard_csrq)
									+ info.getBorn() + "\n" + getString(R.string.idcard_country)
									+ countryMap.getCountry(info.getCountry()) + " / " + info.getCountry() + "\n"
									+ getString(R.string.idcard_yxqx) + info.getPeriod() + "\n"
									+ getString(R.string.idcard_qzjg) + info.getApartment() + "\n"
									+ getString(R.string.idcard_sfhm) + info.getNo() + "\n"
									+ getString(R.string.idcard_version) + info.getIdcard_version());
						} else {
							cellphone_showData.setText(getString(R.string.idcard_xm) + info.getName() + "\n"
									+ getString(R.string.idcard_xb) + info.getSex() + "\n" + getString(R.string.idcard_mz)
									+ info.getNation() + "\n" + getString(R.string.idcard_csrq) + info.getBorn() + "\n"
									+ getString(R.string.idcard_dz) + info.getAddress() + "\n"
									+ getString(R.string.idcard_sfhm) + info.getNo() + "\n" + getString(R.string.idcard_qzjg)
									+ info.getApartment() + "\n" + getString(R.string.idcard_yxqx) + info.getPeriod() + "\n");
							if(SystemUtil.getDeviceType() != StringUtil.DeviceModelEnum.TPS900.ordinal()) {
								cellphone_showData.append(getString(R.string.idcard_zwxx) + fringerprintData + "\n");//判断一下失败时的长度
								if(!(StringUtil.toHexString(id_phy_addr).length()<=22)) {
									cellphone_showData.append("物理地址："+StringUtil.toHexString(id_phy_addr).substring(14,34));
								}
							}
						}
					}else {
						if (SystemUtil.getDeviceType() != StringUtil.DeviceModelEnum.TPS900.ordinal() && "I".equals(info.getCard_type())) {
							// luyq add 20170823 增加外籍身份证信息显示
							idcardInfo.setText(getString(R.string.idcard_xm) + info.getName() + "\n"
									+ getString(R.string.idcard_cn_name) + info.getCn_name() + "\n" // Chinese name
									+ getString(R.string.idcard_xb) + info.getSex() + "\n" + getString(R.string.idcard_csrq)
									+ info.getBorn() + "\n" + getString(R.string.idcard_country)
									+ countryMap.getCountry(info.getCountry()) + " / " + info.getCountry() + "\n"
									+ getString(R.string.idcard_yxqx) + info.getPeriod() + "\n"
									+ getString(R.string.idcard_qzjg) + info.getApartment() + "\n"
									+ getString(R.string.idcard_sfhm) + info.getNo() + "\n"
									+ getString(R.string.idcard_version) + info.getIdcard_version());
						} else {
							idcardInfo.setText(getString(R.string.idcard_xm) + info.getName() + "\n"
									+ getString(R.string.idcard_xb) + info.getSex() + "\n" + getString(R.string.idcard_mz)
									+ info.getNation() + "\n" + getString(R.string.idcard_csrq) + info.getBorn() + "\n"
									+ getString(R.string.idcard_dz) + info.getAddress() + "\n"
									+ getString(R.string.idcard_sfhm) + info.getNo() + "\n" + getString(R.string.idcard_qzjg)
									+ info.getApartment() + "\n" + getString(R.string.idcard_yxqx) + info.getPeriod() + "\n");
							if(SystemUtil.getDeviceType() != StringUtil.DeviceModelEnum.TPS900.ordinal()) {
								idcardInfo.append(getString(R.string.idcard_zwxx) + fringerprintData + "\n");
								if(!(StringUtil.toHexString(id_phy_addr).length()<=22)) {
									idcardInfo.append("物理地址："+StringUtil.toHexString(id_phy_addr).substring(14,34));
								}
							}
						}
					}
					
					byte[] buf = new byte[WLTService.imgLength];
					if (1 == WLTService.wlt2Bmp(image, buf)) {
						if(isCellPhone) {
							if(SystemUtil.getDeviceType() == StringUtil.DeviceModelEnum.TPS900.ordinal()) {
								people_view.setImageBitmap(bitmap);
							}else {
								people_view.setImageBitmap(IDPhotoHelper.Bgr2Bitmap(buf));
							}
							people_view.setVisibility(View.VISIBLE);
						}else {
							if(SystemUtil.getDeviceType() == StringUtil.DeviceModelEnum.TPS900.ordinal()) {
								imageView1.setImageBitmap(bitmap);
							}else {
								imageView1.setImageBitmap(IDPhotoHelper.Bgr2Bitmap(buf));
							}
							imageView1.setVisibility(View.VISIBLE);
						}
					}
					showInfoView();//show another view
					isCardPressing = true;
					isCount = false;
					
					if(hasZk) {
						if (fringerprintData == null || "".equals(fringerprintData) || fringerprint == null
								|| getString(R.string.idcard_wdqhbhzw).equals(fringerprintData.trim())) {
							// 关闭指纹仪
							if (handler2.hasMessages(1)) {
								handler2.removeMessages(1);
							}
							hint_view.setText("");
							if (isCircle) {
								handler1.sendEmptyMessageDelayed(1, 100);
							}else {
								btn_read_once.setEnabled(true);
								btn_read_circle.setEnabled(true);
							}
						} else {
								hint_view.setText(getString(R.string.fprint_qyzzw));
								playSound(4);
								isCount = false;
								imageView2.setVisibility(View.VISIBLE);
								
								if(!isFinish) {
									fingerReader = FingerReader.getInstance(UsbIdCardActivity.this);
									fingerReader.openFingerReader(fringerprint);
									fingerReader.getFingerDetect(imageView2);
									fingerReader.verifyFinger(true);
								}
								
								new Thread(new Runnable() {
									
									@Override
									public void run() {
										// TODO Auto-generated method stub
										while(!isStop) {
											if(fingerReader.isMatchSuccess()==1) {
												isStop = true;
												runOnUiThread(new Runnable() {
													public void run() {
														playSound(3);
					        	                		idcardInfo.setText("");
					        	                		if(isCellPhone) {
					        	                			people_view.setVisibility(View.GONE);
					        	                		}else {
					        	                			imageView1.setVisibility(View.GONE);
					        	                		}
					        	                		btn_read_once.setEnabled(true);
					        	                		btn_read_circle.setEnabled(true);
					        	                		imageView2.setVisibility(View.GONE);
					        	                		hint_view.setText(R.string.idcard_qfk);
													}
												});
												break;
											}else if(fingerReader.isMatchSuccess()==2) {
												isStop = true;
												runOnUiThread(new Runnable() {
													public void run() {
														playSound(2);
					        	                		idcardInfo.setText("");
					        	                		if(isCellPhone) {
					        	                			people_view.setVisibility(View.GONE);
					        	                		}else {
					        	                			imageView1.setVisibility(View.GONE);
					        	                		}
					        	                		btn_read_once.setEnabled(true);
					        	                		btn_read_circle.setEnabled(true);
					        	                		imageView2.setVisibility(View.GONE);
					        	                		hint_view.setText(R.string.idcard_qfk);
													}
												});
												break;
											}
										}
									}
								}).start();
								
								if (!isCount && isCircle) {
									isCount = true;
									timer = new Timer();
									TimerTask task = new TimerTask() {
										int count_down = 3;

										@Override
										public void run() {
											if(isStop) {
												isCount = true;
												timer.cancel();
												if(!handler1.hasMessages(1))
													handler1.sendEmptyMessageDelayed(1, 1000);
												return;
											}
											Message msg = new Message();
											msg.what = 2;
											msg.arg1 = count_down;
											handler1.sendMessage(msg);
											count_down--;
											if (count_down < 0 || !isCount) {
												isCount = false;
												timer.cancel();
												handler1.sendEmptyMessage(3);
											}
										}
									};
									timer.schedule(task, 0, 1000);
								}
						}
					}else {
						if(isCircle) {
							handler1.sendEmptyMessageDelayed(1, 100);
						}else {
							btn_read_once.setEnabled(true);
							btn_read_circle.setEnabled(true);
						}
					}
				} else {
					failedCount++;
					if (isCircle) {
						isCount = false;
						getPermission.performClick();
						handler1.sendMessageDelayed(handler1.obtainMessage(1, ""), 100);
					} else {
						hasCircle = false;
						getPermission.performClick();
						//handler1.sendMessageDelayed(handler1.obtainMessage(1, ""), 100);
						btn_read_once.setEnabled(true);
						btn_read_circle.setEnabled(true);
						showDefaultView("");
					}
				}
				/*test_info_view
				.setText(getString(R.string.idcard_cscs) + count_time + "  " + getString(R.string.idcard_cgcs)
						+ success);*/
				test_info_view.setText(getString(R.string.idcard_cscs) + count_time + "  " + getString(R.string.idcard_cgcs)
				+ (count_time-failedCount)+"  失败次数"+failedCount);
			}
		}
	}
	
	private void checkZkUsb() {
		zk_musbManager = (UsbManager)UsbIdCardActivity.this.getSystemService(Context.USB_SERVICE);
        for (UsbDevice device : zk_musbManager.getDeviceList().values())
        {
            if ((device.getVendorId() == FingerReader.VID && device.getProductId() == FingerReader.PID)||(device.getVendorId() == FingerReader.VID && device.getProductId() == FingerReader.PID_110))
            {
            	hasZk = true;
            }
        }
	}

	// 
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
			Log.d("idcard demo", "pid:"+pid);
			Log.d("idcard demo", "vid:"+vid);
			if ((pid == IdCard.READER_PID_SMALL && vid == IdCard.READER_VID_SMALL) 
					|| (pid == IdCard.READER_PID_BIG && vid == IdCard.READER_VID_BIG)
					|| (pid == IdCard.READER_PID_WINDOWS && vid == IdCard.READER_VID_WINDOWS)) {
				hasReader = true;
				idcard_reader = usbDevice;
				if (mUsbManager.hasPermission(usbDevice)) {
					getPermission.setVisibility(View.INVISIBLE);
					def_view.setText(getString(R.string.idcard_hqqxcg));
					hasPermission = true;
					if(!isCircle) {
						btn_read_circle.setEnabled(true);
						btn_read_once.setEnabled(true);
					}else {
						btn_read_circle.setEnabled(true);
						btn_read_once.setEnabled(false);
					}
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
		if(SystemUtil.getDeviceType() == StringUtil.DeviceModelEnum.TPS900.ordinal()) {
			getPermission.setVisibility(View.INVISIBLE);
			btn_read_once.setEnabled(true);
			btn_read_circle.setEnabled(true);
			hasPermission = true;
		}
		if (!hasReader) {
			hint_view.setText(getString(R.string.idcard_wfljdkq));
			if (handler1.hasMessages(1)) {
				handler1.removeMessages(1);
			}
		}
	}

	private void showDefaultView(String content) {
		info_view.setVisibility(View.GONE);
		if(isCellPhone) {
			people_view.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));
			people_view.setVisibility(View.VISIBLE);
		}else {
			imageView1.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));
			imageView1.setVisibility(View.VISIBLE);
		}
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
	
	OnUsbPermissionCallback callback = new OnUsbPermissionCallback() {

		@Override
		public void onUsbPermissionEvent(UsbDevice dev, boolean granted) {
			if (granted) {
				try {
					if (sdta == null) {
						sdta = new Sdtapi(UsbIdCardActivity.this, dev);
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					if (sdta == null) {
						//Toast.makeText(UsbIdCardActivity.this, "模块连接失败", Toast.LENGTH_SHORT).show();
					} else {
						//Toast.makeText(UsbIdCardActivity.this, "模块连接成功", Toast.LENGTH_SHORT).show();
					}
				}
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
    	isLeave = true;
    	//PosUtil.setFingerPrintPower(PosUtil.FINGERPRINT_POWER_OFF);
    	super.onBackPressed();
    	pressBack = true;
    }
    
    
    public void initVoice2(){
        soundPool2= new SoundPool(10, AudioManager.STREAM_MUSIC, 3);//第一个参数为同时播放数据流的最大个数，第二数据流类型，第三为声音质量
        music = soundPool2.load(UsbIdCardActivity.this, R.raw.beep, 1); //把你的声音素材放到res/raw里，第2个参数即为资源文件，第3个为音乐的优先级
    }
	
	public void play_voice() {
        soundPool2.play(music, 1, 1, 0, 0, 1);
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
	
}