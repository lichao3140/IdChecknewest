package com.telpo.idcheck;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.telpo.tps550.api.TelpoException;
import com.telpo.tps550.api.idcard.BlueToothIdCard;
import com.telpo.tps550.api.idcard.CountryMap;
import com.telpo.tps550.api.idcard.IdentityInfo;
import com.telpo.tps550.api.util.ReaderUtils;
import com.telpo.tps550.api.util.StringUtil;
import com.telpo.tps550.api.util.SystemUtil;
import com.zkteco.android.IDReader.IDPhotoHelper;
import com.zkteco.android.IDReader.WLTService;
import com.zkteco.android.biometric.core.device.ParameterHelper;
import com.zkteco.android.biometric.core.device.TransportType;
import com.zkteco.android.biometric.core.utils.LogHelper;
import com.zkteco.android.biometric.core.utils.ToolUtils;
import com.zkteco.android.biometric.nidfpsensor.NIDFPFactory;
import com.zkteco.android.biometric.nidfpsensor.NIDFPSensor;
import com.zkteco.android.biometric.nidfpsensor.exception.NIDFPException;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("NewApi")
public class BluetoothIdCardActivity extends Activity{
	static String[] nation_list = new String[] { "汉", "蒙古", "回", "藏", "维吾尔", "苗", "彝", "壮", "布依", "朝鲜", "满", "侗", "瑶",
			"白", "土家", "哈尼", "哈萨克", "傣", "黎", "傈僳", "佤", "畲", "高山", "拉祜", "水", "东乡", "纳西", "景颇", "柯尔克孜", "土", "达斡尔",
			"仫佬", "羌", "布朗", "撒拉", "毛南", "仡佬", "锡伯", "阿昌", "普米", "塔吉克", "怒", "乌孜别克", "俄罗斯", "鄂温克", "德昂", "保安", "裕固",
			"京", "塔塔尔", "独龙", "鄂伦春", "赫哲", "门巴", "珞巴", "基诺", "其他", "外国血统中国籍人士" };
	private static final String TAG = "BluetoothIdCardActivity";
	private TextView idcardInfo, hint_view, def_view, test_info_view,mTxtReport,showVersion;
	private ImageView imageView1, imageView2;
	private Button getDevice, btn_read_once,btn_read_circle;
	private CountryMap countryMap = CountryMap.getInstance();
	private View info_view;
	private byte[] image;
	private byte[] fringerprint;
	private String fringerprintData;
	private boolean isCardPressing = false;
	private boolean isUsingUsb = false;
	
	private boolean isCount = false;
	private boolean isCircle = false;
	private boolean hasCircle = false;
	private int count_time = 0;
	private int success = 0;
	private Timer timer = new Timer();
	private TimerTask task;

	private List<BluetoothDeviceInfo> discover_devices_list;
	private Dialog dialog;
	private BlueToothIdCard blueToothIdCard;
	private MyAdapter adapter;
	private BluetoothAdapter mBluetoothAdapter;
    private String getVersion = null;
    
    private Handler handler1 = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 1:
				new TestAsyncTask().execute();
				break;
			case 2:
				btn_read_once.setEnabled(true);
				btn_read_circle.setEnabled(true);
				break;
			default:
				break;
			}
		};
	};
	
	public static String toHexString1(byte b){
        String s = Integer.toHexString(b & 0xFF);
        if (s.length() == 1){
            return "0" + s;
        }else{
            return s;
        }
    }
	
	private class TestAsyncTask extends AsyncTask<Void, Void, IdentityInfo>{
		
		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();
			btn_read_once.setEnabled(false);
			fringerprint = null;
			fringerprintData = null;
			image = null;
			showDefaultView("");
		}

		@Override
		protected IdentityInfo doInBackground(Void... params) {
			// TODO Auto-generated method stub
			try {
				getVersion = blueToothIdCard.getVersion();
			} catch (TelpoException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return blueToothIdCard.checkIdCard();
		}
		
		@Override
		protected void onPostExecute(IdentityInfo result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			if(result!=null) {
				showVersion.setText("version is"+getVersion);
				decodeIdCardInfo(result);
			}else {
				btn_read_once.setEnabled(true);
				if(isCircle) {
					Log.d("idcard demo", "checkidcard null && is circle");
					handler1.sendMessageDelayed(handler1.obtainMessage(1, ""), 500);
				}
			}
		}
		
	}

	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
		@Override
		public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (discover_devices_list == null) {
						discover_devices_list = new ArrayList<BluetoothIdCardActivity.BluetoothDeviceInfo>();
					}
					int device_class = device.getBluetoothClass().getMajorDeviceClass();
					Log.d(TAG, "device class = " + device_class);
					BluetoothDeviceInfo info = new BluetoothDeviceInfo();
					info.rssi = rssi;
					info.device = device;
					Log.d(TAG, info.toString());
					for (int i = 0; i < discover_devices_list.size(); i++) {
						if (device.getAddress().equals(discover_devices_list.get(i).device.getAddress())) {
							return;
						}
					}
					if (!discover_devices_list.contains(info)) {
						discover_devices_list.add(info);
						adapter.notifyDataSetChanged();
					}
				}
			});
		}
	};

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.bt_idcard);
		showVersion = (TextView) findViewById(R.id.showVersion);
		idcardInfo = (TextView) findViewById(R.id.showData);
		imageView1 = (ImageView) findViewById(R.id.imageView1);
		imageView2 = (ImageView) findViewById(R.id.imageView2);
		info_view = findViewById(R.id.relativeLayout);
		hint_view = (TextView) findViewById(R.id.hint_view);
		def_view = (TextView) findViewById(R.id.default_view);
		test_info_view = (TextView) findViewById(R.id.test_info_view);
		mTxtReport = (TextView) findViewById(R.id.bt_finReport);
		mBluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();

		test_info_view.setVisibility(View.GONE);

		discover_devices_list = new ArrayList<BluetoothIdCardActivity.BluetoothDeviceInfo>();
		adapter = new MyAdapter(discover_devices_list, this);

		getDevice = (Button) findViewById(R.id.requestPermission);
		getDevice.setVisibility(View.VISIBLE);
		getDevice.setText("搜索设备");
		getDevice.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				openDiscoverDiaolg();
			}
		});

		btn_read_once = (Button) findViewById(R.id.read_once);
		btn_read_once.setEnabled(false);
		btn_read_once.setVisibility(View.VISIBLE);
		btn_read_once.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				fringerprintData = "";
				fringerprint = null;
				showDefaultView("");
				btn_read_once.setEnabled(false);
				btn_read_circle.setEnabled(false);
				imageView1.setVisibility(View.VISIBLE);
				new TestAsyncTask().execute();
			}
		});
			
		btn_read_circle = (Button) findViewById(R.id.btn_read_circle);
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
					imageView1.setVisibility(View.VISIBLE);
					handler1.sendMessage(handler1.obtainMessage(1, ""));
				} else {
					btn_read_circle.setText(getString(R.string.idcard_start_read));
					btn_read_once.setEnabled(true);
					btn_read_circle.setEnabled(true);
					count_time = 0;
					success = 0;
				}
			}
		});
		

		IntentFilter intent = new IntentFilter();
		intent.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		registerReceiver(mReceiver, intent);
		
		test_info_view.setText(getString(R.string.idcard_cscs) + count_time + "  " + getString(R.string.idcard_cgcs)
				+ success + "  " + getString(R.string.idcard_sbcs) + (count_time - success));

		if (!mBluetoothAdapter.isEnabled()) {
			if (!mBluetoothAdapter.enable()) {
				Toast.makeText(BluetoothIdCardActivity.this, "无法打开蓝牙", Toast.LENGTH_SHORT).show();
				getDevice.setEnabled(false);
			}
		}
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		detectUsbDeviceAttach();
		def_view.setText("");
		hint_view.setText("");
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		isCount = false;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		isCardPressing = false;
		isCount = false;
		unregisterReceiver(mReceiver);
		unregisterReceiver(mUsbReceiver);
		while (isUsingUsb);
		if (mBluetoothAdapter != null) {
			mBluetoothAdapter.disable();
		}
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			AlertDialog.Builder localBuilder = new AlertDialog.Builder(this);
			localBuilder.setCancelable(false).setTitle(" ").setMessage("您的设备不支持BLE特性，请使用Android4.3以上系统且具有低功耗蓝牙的设备!")
					.setNeutralButton("确定", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface paramAnonymousDialogInterface, int paramAnonymousInt) {
							finish();
						}
					});
			localBuilder.create().show();
		}
	}

	private void decodeIdCardInfo(IdentityInfo info) {
		// TODO Auto-generated method stub
		Log.d(TAG, "start decode info");
		boolean result = false;
		if (info != null && !"".equals(info.getName())) {
			Log.d(TAG, "info != null");
			image = info.getHead_photo();
			
			
			if(image.length > 1024) {
				fringerprint = Arrays.copyOfRange(image, 1024, image.length);
			}
			
			fringerprintData = ReaderUtils.get_finger_info(BluetoothIdCardActivity.this, fringerprint);
			result = true;
		}else {
			Log.d(TAG, "info == null");
		}
		if (result) {
			imageView2.setVisibility(View.INVISIBLE);
			if ("I".equals(info.getCard_type())) {
				// luyq add 20170823 增加外籍身份证信息显示
				idcardInfo.setText(getString(R.string.idcard_xm) + info.getName() + "\n"
						+ getString(R.string.idcard_cn_name) + info.getCn_name() + "\n" // Chinese name
						+ getString(R.string.idcard_xb) + info.getSex() + "\n" + getString(R.string.idcard_csrq)
						+ info.getBorn() + "\n" + getString(R.string.idcard_country)
						+ countryMap.getCountry(info.getCountry()) + " / " + info.getCountry() + "\n"
						+ getString(R.string.idcard_yxqx) + info.getPeriod() + "\n" + getString(R.string.idcard_qzjg)
						+ info.getApartment() + "\n" + getString(R.string.idcard_sfhm) + info.getNo() + "\n"
						+ getString(R.string.idcard_version) + info.getIdcard_version() + "\n");
			} else {
				idcardInfo.setText(getString(R.string.idcard_xm) + info.getName() + "\n" + getString(R.string.idcard_xb)
						+ info.getSex() + "\n" + getString(R.string.idcard_mz) + info.getNation() + "\n"
						+ getString(R.string.idcard_csrq) + info.getBorn() + "\n" + getString(R.string.idcard_dz)
						+ info.getAddress() + "\n" + getString(R.string.idcard_sfhm) + info.getNo() + "\n"
						+ getString(R.string.idcard_qzjg) + info.getApartment() + "\n" + getString(R.string.idcard_yxqx)
						+ info.getPeriod() + "\n" + getString(R.string.idcard_zwxx) + fringerprintData);
			}
			Log.d(TAG, "decode photo");
			byte[] buf = new byte[WLTService.imgLength];
			if (1 == WLTService.wlt2Bmp(image, buf)) {
				imageView1.setImageBitmap(IDPhotoHelper.Bgr2Bitmap(buf));
			}
			Log.d(TAG, "decode info success");
			btn_read_once.setEnabled(true);
			showInfoView();
			isCardPressing = true;
			//playSound(1);
			success++;
			if (fringerprintData == null || "".equals(fringerprintData) || fringerprint == null
					|| getString(R.string.idcard_wdqhbhzw).equals(fringerprintData.trim())) {
				
				
				btn_read_circle.setEnabled(false);
				
				hint_view.setText("");
				if (!isCount && isCircle) {
					isCount = true;
					timer = new Timer();
					TimerTask task = new TimerTask() {
						int count_down = 3;

						@Override
						public void run() {
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
				} else {
					btn_read_once.setEnabled(true);
				}
				count_time++;
			} else {
				// 打开指纹仪
				count_time++;
				imageView2.setVisibility(View.INVISIBLE);
				btn_read_once.setEnabled(true);
				btn_read_circle.setEnabled(true);
				hint_view.setText(getString(R.string.fprint_qyzzw));
			}
			if(isCircle) {
				Log.e("idcard demo", "result != null && is circle");
				handler1.sendMessageDelayed(handler1.obtainMessage(1, ""), 500);
			}
		} else {
			if (isCircle) {
				Log.e("idcard demo", "result == null && is circle");
				handler1.sendMessageDelayed(handler1.obtainMessage(1, ""), 500);
			} else {
				if (!hasCircle) {
					//playSound(5);
				}
				hasCircle = false;
				btn_read_once.setEnabled(true);
				showDefaultView("");
			}
		}
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
		imageView1.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.logo));
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
			if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
				int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
				if (state == BluetoothAdapter.STATE_OFF && mBluetoothAdapter != null) {
					mBluetoothAdapter.enable();
				}
			}
		}
	};

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
					//Utils.upgradeRootPermission(path + i);
				}
				new Thread(new Runnable() {
					@Override
					public void run() {
						//fingercamera.Init();
						if (!isCount && isCircle) {
							isCount = true;
							timer = new Timer();
							task = new TimerTask() {
								int count_down = 10;

								@Override
								public void run() {
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
				}).start();

			} else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(curItentActionName)) {
			}
		}
	};

	private class MyAdapter extends BaseAdapter {
		private LayoutInflater inflater = null;
		private List<BluetoothDeviceInfo> map;
		// DecimalFormat decimalFormat;

		public MyAdapter(List<BluetoothDeviceInfo> map, Context context) {
			this.map = map;
			inflater = LayoutInflater.from(context);
		}

		@Override
		public int getCount() {
			return map.size();
		}

		@Override
		public Object getItem(int position) {
			return map.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder = null;
			if (convertView == null) {
				holder = new ViewHolder();
				convertView = inflater.inflate(R.layout.bt_device_item, null);
				holder.name = (TextView) convertView.findViewById(R.id.device_name);
				holder.mac = (TextView) convertView.findViewById(R.id.device_mac);
				holder.rssi = (TextView) convertView.findViewById(R.id.device_rssi);
				// 为view设置标签
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			String name = map.get(position).device.getName();
			holder.name.setText(name == null ? "Unknown" : name);
			holder.mac.setText(map.get(position).device.getAddress());
			// float power = (float) ((Math.abs(map.get(position).rssi) - 59) / (10 * 2.0));
			// float pow = (float) Math.pow(10, power);
			holder.rssi.setText(map.get(position).rssi + "dbm");
			return convertView;
		}

		public class ViewHolder {
			TextView name;
			TextView mac;
			TextView rssi;
		}
	}

	private class BluetoothDeviceInfo {
		public BluetoothDevice device;
		public int rssi;

		@Override
		public String toString() {
			// TODO Auto-generated method stub
			return "name = " + device.getName() + ", mac = " + device.getAddress() + ", rssi = " + rssi;
		}
	}

	private void openDiscoverDiaolg() {
		// TODO Auto-generated method stub
		if (dialog == null) {
			dialog = new Dialog(this);
			dialog.setContentView(R.layout.dialog_devicelist);
			dialog.setTitle("附近设备");
			dialog.setCanceledOnTouchOutside(false);
			dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {

				@Override
				public void onDismiss(DialogInterface d) {
					// TODO Auto-generated method stub
					mBluetoothAdapter.stopLeScan(mLeScanCallback);
					getDevice.setEnabled(true);
					discover_devices_list.clear();
					if (dialog != null && dialog.isShowing()) {
						dialog.findViewById(R.id.button_scan).setEnabled(true);
						dialog.dismiss();
					}
				}
			});
			ListView listView = (ListView) dialog.findViewById(R.id.discover_devices);
			listView.addHeaderView(getLayoutInflater().inflate(R.layout.bt_device_item, null));
			listView.setAdapter(adapter);
			listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					// TODO Auto-generated method stub
					if (position == 0) {
						return;
					}
					mBluetoothAdapter.stopLeScan(mLeScanCallback);
					getDevice.setEnabled(true);
					if (dialog != null && dialog.isShowing()) {
						dialog.findViewById(R.id.button_scan).setEnabled(true);
						dialog.dismiss();
					}
					//connectDevice(discover_devices_list.get(position - 1).device);
					blueToothIdCard = new BlueToothIdCard(BluetoothIdCardActivity.this);
					blueToothIdCard.connectDevice(discover_devices_list.get(position - 1).device);
					if(blueToothIdCard.checkConnect()) {
						handler1.sendEmptyMessageDelayed(2, 3000);
						getDevice.setVisibility(View.GONE);
					}else {
						btn_read_once.setEnabled(false);
						btn_read_circle.setEnabled(false);
					}
					
				}
			});
			((Button) dialog.findViewById(R.id.button_scan)).setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					scanDevices();
				}
			});
		}
		dialog.show();
		scanDevices();
	}

	private void scanDevices() {
		// TODO Auto-generated method stub
		if (mBluetoothAdapter != null) {
			mBluetoothAdapter.startLeScan(mLeScanCallback);
			if (dialog != null && dialog.isShowing()) {
				dialog.findViewById(R.id.button_scan).setEnabled(false);
			}
			handler1.postDelayed(new Runnable() {

				@Override
				public void run() {
					// TODO Auto-generated method stub
					mBluetoothAdapter.stopLeScan(mLeScanCallback);
					getDevice.setEnabled(true);
					if (dialog != null && dialog.isShowing()) {
						dialog.findViewById(R.id.button_scan).setEnabled(true);
					}
				}
			}, 10 * 1000);
		}
	}

	public int charSendBufferPos = 0;
}
