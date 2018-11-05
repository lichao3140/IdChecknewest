package com.telpo.typeAcheck;		

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import com.common.pos.api.util.posutil.PosUtil;
import com.telpo.idcheck.R;
import com.telpo.tps550.api.TelpoException;
import com.telpo.tps550.api.idcard.IdCard;
import com.telpo.tps550.api.util.StringUtil;
import com.telpo.tps550.api.util.SystemUtil;
import com.telpo.tps550.api.typea.*;
import com.telpo.tps550.api.util.ShellUtils;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class UsbTACardActivity extends Activity{
	
	private byte[] Password;
	private byte[] Newwritedata;
	private Button tgetPermission, pwConfirm, writeData,writeDataConfirm,read_again,tps350_power_off,read_ta_circle;
	private TextView tcardInfo, thint_view, tdef_view, ttest_info_view;
	private EditText pwCheck, dataWrite;
	private TAInfo tinfo;
	private TASectorInfo tsectorinfo;
	private SoundPool tsoundPool;
	private View tinfo_view;
	private PendingIntent tPermissionIntent;//related to usb
	private static final String TACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
	private UsbManager tUsbManager;
	private UsbDevice tcard_reader;
	private UsbTypeA treader;
	private GetTAInfoTask tAsyncTask;
	private PwCheckTask checkAsyncTask;
	private WriteDataTask dataAsyncTask;
	private boolean hastPermission = false;
	private boolean isUsingtUsb = false;
	private boolean hastReader = false;
	private boolean istFinish = false;
	private UsbTACard usbTACard;
	private String newwritedata;
	//private boolean readCircle = false;
	//private TextView count_text;
	//private int count_time,success_count;
	
	
	private Handler handler3 = new Handler() {
		
		@Override
		public void handleMessage(Message msg) {
		    if (istFinish) {
			    return;
		    }
		    switch (msg.what) {
		    case 1:
		        tAsyncTask = new GetTAInfoTask();
		        tAsyncTask.execute();		        
			    break;
		    case 2:
		    	checkAsyncTask = new PwCheckTask();
			    checkAsyncTask.execute();
			    break;
		    case 3:
		    	dataAsyncTask = new WriteDataTask();
		    	dataAsyncTask.execute();
		    	break;
		    case 4:
		    	openUsbTACard();
				if(usbTACard == null) {
					usbTACard = new UsbTACard(tcard_reader, tUsbManager);
				}
				writeData.setVisibility(View.GONE);		
				writeDataConfirm.setVisibility(View.GONE);
				istFinish = false;
		    	break;
		    case 5:
		    	read_again.performClick();
		    	break;
			}
		    super.handleMessage(msg);
		}
	};
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.idcard_usb);
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					IdCard.open(UsbTACardActivity.this);
				} catch (TelpoException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}).start();
		
		pwCheck = (EditText) findViewById(R.id.edittext1);
		
		tgetPermission = (Button) findViewById(R.id.requestPermission);
		pwConfirm = (Button) findViewById(R.id.passwordconfirm);
		writeData = (Button) findViewById(R.id.writedata);
		dataWrite = (EditText) findViewById(R.id.edittext2);		
		tcardInfo = (TextView) findViewById(R.id.showData);
		tinfo_view = findViewById(R.id.relativeLayout);
		thint_view = (TextView) findViewById(R.id.hint_view);
		tdef_view = (TextView) findViewById(R.id.default_view);
		ttest_info_view = (TextView) findViewById(R.id.test_info_view);
		writeDataConfirm = (Button) findViewById(R.id.writedataconfirm);
		tps350_power_off = (Button) findViewById(R.id.tps350_power_off);
		read_ta_circle = (Button) findViewById(R.id.read_ta_circle);
		read_ta_circle.setVisibility(View.VISIBLE);
		//count_text = (TextView) findViewById(R.id.count_text);
		//count_text.setVisibility(View.VISIBLE);
		
		if(SystemUtil.getDeviceType() == StringUtil.DeviceModelEnum.TPS350_4G.ordinal()) {
			tps350_power_off.setVisibility(View.VISIBLE);
		}
		read_again = (Button) findViewById(R.id.read_again);
		read_again.setVisibility(View.VISIBLE);
		tsoundPool = new SoundPool(10, AudioManager.STREAM_SYSTEM, 5);
		tsoundPool.load(this, R.raw.read_card, 1);
		tsoundPool.load(this, R.raw.verify_fail, 2);
		tsoundPool.load(this, R.raw.verify_success, 3);
		tsoundPool.load(this, R.raw.please_verify, 4);
		thint_view.setText(getString(R.string.idcard_typeA));
		
		if(SystemUtil.getDeviceType() != StringUtil.DeviceModelEnum.TPS462.ordinal()
				&& SystemUtil.getDeviceType() != StringUtil.DeviceModelEnum.TPS650.ordinal()) {
			ShellUtils.execCommand("echo 3 > /sys/class/telpoio/power_status", false);
			ShellUtils.execCommand("echo 2 > /dev/telpoio", false);
		}
		
		tgetPermission.setOnClickListener(new View.OnClickListener(){
			
			@Override
			public void onClick(View v) {
				openUsbTACard();
				if(usbTACard == null) {
					usbTACard = new UsbTACard(tcard_reader, tUsbManager);
				}
			}
		});

		tPermissionIntent = PendingIntent.getBroadcast(UsbTACardActivity.this, 0, new Intent(TACTION_USB_PERMISSION), 0);
		IntentFilter tfilter = new IntentFilter();
		tfilter.addAction(TACTION_USB_PERMISSION);
		tfilter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		tfilter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
		tfilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
		tfilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		registerReceiver(tReceiver, tfilter);
		
		pwConfirm.setOnClickListener(new View.OnClickListener(){
			
			@Override
			public void onClick(View v) {
				openLockCheck();
				pwConfirm.setEnabled(false);
				read_again.setEnabled(false);
			}
			
		});
		
		/*read_ta_circle.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				count_time=0;
				success_count=0;
				if(!readCircle) {
					readCircle = true;
					read_again.setEnabled(false);
					read_again.performClick();
					read_ta_circle.setText("停止循环");
				}else {
					readCircle = false;
					read_again.setEnabled(true);
					read_ta_circle.setText("循环读卡");
				}
			}
		});*/
		
		writeData.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				openWriteData();
			}
		});
		
		writeDataConfirm.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				confirmData();
			}
		});
		
		read_again.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				//finish();
				//startActivity(new Intent(UsbTACardActivity.this,UsbTACardActivity.class));
				pwCheck.setVisibility(View.GONE);
				dataWrite.setVisibility(View.GONE);
				tgetPermission.setVisibility(View.GONE);
				pwConfirm.setVisibility(View.GONE);
				writeData.setVisibility(View.GONE);
				writeDataConfirm.setVisibility(View.GONE);
				tcardInfo.setVisibility(View.GONE);
				tdef_view.setVisibility(View.GONE);
				tcardInfo.setVisibility(View.GONE);
				
				tAsyncTask = new GetTAInfoTask();
		        tAsyncTask.execute();
				
			}
		});
		
		tps350_power_off.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				PosUtil.setFingerPrintPower(PosUtil.FINGERPRINT_POWER_OFF);
			}
		});
	}
	
	protected void onDestroy(){
		super.onDestroy();
		while(isUsingtUsb);
		unregisterReceiver(tReceiver);
		if(SystemUtil.getDeviceType() != StringUtil.DeviceModelEnum.TPS462.ordinal()
			&& SystemUtil.getDeviceType() != StringUtil.DeviceModelEnum.TPS650.ordinal()) {
			ShellUtils.execCommand("echo 4 > /sys/class/telpoio/power_status", false);
		}
		IdCard.close();
	}
	
	@Override
	protected void onStart(){
		super.onStart();
		
		if(SystemUtil.getDeviceType() != StringUtil.DeviceModelEnum.TPS462.ordinal()
			&& SystemUtil.getDeviceType() != StringUtil.DeviceModelEnum.TPS650.ordinal()) {
			ShellUtils.execCommand("echo 2 > /dev/telpoio", false);
			ShellUtils.execCommand("echo 3 > /sys/class/telpoio/power_status", false);
		}
		
		if(SystemUtil.getDeviceType() == StringUtil.DeviceModelEnum.TPS350_4G.ordinal())
			PosUtil.setFingerPrintPower(PosUtil.FINGERPRINT_POWER_ON);
		
		handler3.sendEmptyMessageDelayed(4, 1000);
	}
	
	protected void onStop(){
		super.onStop();
		if(handler3.hasMessages(1)) {
			handler3.removeMessages(1);
		}	
		if(handler3.hasMessages(2)) {
			handler3.removeMessages(2);
		}
		if(handler3.hasMessages(3)) {
			handler3.removeMessages(3);
		}
		/*if(SystemUtil.getDeviceType() == StringUtil.DeviceModelEnum.TPS350_4G.ordinal()) {
			PosUtil.setFingerPrintPower(PosUtil.FINGERPRINT_POWER_OFF);
		}*/
		pwCheck.setText("");
		dataWrite.setText("");
		if(SystemUtil.getDeviceType() != StringUtil.DeviceModelEnum.TPS462.ordinal()
			&& SystemUtil.getDeviceType() != StringUtil.DeviceModelEnum.TPS650.ordinal()) {
			ShellUtils.execCommand("echo 4 > /sys/class/telpoio/power_status", false);
		}
		istFinish = true;
	}		
	//process of getting permission
	public void openUsbTACard(){
		isUsingtUsb = true;
		tUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		HashMap<String, UsbDevice> tdeviceHashMap = tUsbManager.getDeviceList();
		Iterator<UsbDevice> iterator = tdeviceHashMap.values().iterator();
		hastReader = false;
		
		while (iterator.hasNext()) {
			UsbDevice tusbDevice = iterator.next();
			int tpid = tusbDevice.getProductId();
			int tvid = tusbDevice.getVendorId();
			if ((tpid == IdCard.READER_PID_SMALL && tvid == IdCard.READER_VID_SMALL) 
					|| (tpid == IdCard.READER_PID_BIG && tvid == IdCard.READER_VID_BIG)
					|| (tpid == IdCard.READER_PID_WINDOWS && tvid == IdCard.READER_VID_WINDOWS)){
				hastReader = true;
				tcard_reader = tusbDevice;
				if (tUsbManager.hasPermission(tusbDevice)){
					tgetPermission.setVisibility(View.GONE);
					tdef_view.setText(getString(R.string.idcard_hqqxcg));
					thint_view.setText(getString(R.string.idcard_qfk));
					tdef_view.setVisibility(View.VISIBLE);
					pwConfirm.setVisibility(View.GONE);
					pwCheck.setVisibility(View.GONE);
					dataWrite.setVisibility(View.GONE);
					tcardInfo.setVisibility(View.GONE);
					hastPermission = true;
					try {
						treader = new UsbTypeA(tUsbManager, tcard_reader);
					} catch (TelpoException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					handler3.sendMessageDelayed(handler3.obtainMessage(1,""),0);
					break;
				}else{
					tgetPermission.setVisibility(View.VISIBLE);
					tdef_view.setText(getString(R.string.idcard_qhqqx));
					hastPermission = false;
					tUsbManager.requestPermission(tusbDevice, tPermissionIntent);//***弹出对话框问是否允许的时�?
				}
			}
		}
		isUsingtUsb = false;
		if (!hastReader){
			thint_view.setText(getString(R.string.idcard_typeA));
			if(handler3.hasMessages(1)){
				handler3.removeMessages(1);
			}
		}
	}
		
	public void openLockCheck() {
		
		String password = new String(pwCheck.getText().toString());
		Password = hexStringToBytes(password);
		treader.transmitpassword(Password);
		if ( hastReader && tcard_reader != null ) {
			handler3.sendMessageDelayed(handler3.obtainMessage(2,""), 0);
		}else if (!hastReader){
			thint_view.setText(getString(R.string.idcard_wfljdkq));
		}else if ( pwCheck.getText() == null) {
			thint_view.setText(getString(R.string.tacard_mybnwk));
		}
	}	
	
	
	public void openWriteData() {
		
		if ( hastReader && tcard_reader != null ) {
			thint_view.setText(getString(R.string.tacard_qxrsj));
			dataWrite.setText("");
		    tcardInfo.setVisibility(View.VISIBLE);
		    pwConfirm.setVisibility(View.INVISIBLE);
		    pwCheck.setVisibility(View.INVISIBLE);
		    writeData.setVisibility(View.INVISIBLE);
		    dataWrite.setVisibility(View.VISIBLE);
		    writeDataConfirm.setVisibility(View.VISIBLE);
		}		
	}
	
	
    public void confirmData() {
		
		newwritedata = new String(dataWrite.getText().toString());
		Newwritedata = hexStringToBytes(newwritedata);
		treader.transmitdata(Newwritedata);
		if ( hastReader && tcard_reader != null ) {
			handler3.sendMessageDelayed(handler3.obtainMessage(3,""), 0);
		}else if (!hastReader){
			thint_view.setText(getString(R.string.idcard_wfljdkq));
		}
	}
	
	private class WriteDataTask extends AsyncTask<Void, Integer, Boolean> {
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();			
		}
		
		@Override
		protected Boolean doInBackground(Void...  arg2) {
			
			Boolean writeright = new Boolean(false);
			try {
				writeright = usbTACard.writeInData(newwritedata);
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
				return false;
			}
			return writeright;
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			try {
				super.onPostExecute(result);
				if(result == null) {
					return;
				}else if (result == true) {
					thint_view.setText(getString(R.string.tacard_xrsjcg));
				}else if (result == false) {
					if( Newwritedata == null ) {
						openWriteData();
						thint_view.setText(getString(R.string.tacard_xrsjbnwk));
					}else {
						openWriteData();
						thint_view.setText(getString(R.string.tacard_xrsjsb));
					}				
				}
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
		}
	}
	
	
	private class PwCheckTask extends AsyncTask<Void, Integer, Boolean>{
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();			
		}
		
		@Override
		protected Boolean doInBackground(Void...  arg1) {
			Boolean pwright = new Boolean(false);
			try {
				pwright = usbTACard.checkPW(Password);
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
				return false;
			}
			return pwright;				
		}
		
		@Override 
		protected void onPostExecute(Boolean result) {
			try {
				super.onPostExecute(result);
				if(result == null) {
					return;
				}else if (result == true) {
					thint_view.setText(getString(R.string.tacard_myyzcg));
					pwConfirm.setVisibility(View.INVISIBLE);
					pwCheck.setVisibility(View.INVISIBLE);
					TASectorInfo teSectorInfo = usbTACard.readData();
					if(teSectorInfo == null) {
						return;
					}
					tcardInfo.setText(getString(R.string.idcard_kh) + tinfo.getNum()+ "\n"
							+ getString(R.string.tacard_knsj) + teSectorInfo.getSectorData());
					writeData.setVisibility(View.VISIBLE);
					pwConfirm.setEnabled(true);
					read_again.setEnabled(true);
				}else if (result == false){
					thint_view.setText(getString(R.string.tacard_mycw));				
					pwConfirm.setEnabled(true);
					read_again.setEnabled(true);
				}
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
		}
	}
	
	private class GetTAInfoTask extends AsyncTask<Void, Integer, Boolean>{
			
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			tinfo = null;
			//start_time = System.nanoTime();
			//count_time++;
		}
			
		@Override
		protected Boolean doInBackground(Void... arg){
			try {
				tinfo = usbTACard.checkTACard();
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
				return false;
			}
			if (tinfo != null){
				return true;				
			}
			return false;
		}
			
		@Override
		protected void onPostExecute(Boolean result){
			try {
				super.onPostExecute(result);
				if (result) {
					//success_count++;
					tcardInfo.setText(getString(R.string.idcard_kh) + tinfo.getNum());
					tcardInfo.setVisibility(View.VISIBLE);
					showtInfoView();
					thint_view.setText(getString(R.string.tacard_myrz));
					pwCheck.setVisibility(View.VISIBLE);
					pwConfirm.setVisibility(View.VISIBLE);
					/*if(readCircle) {
						handler3.sendEmptyMessageDelayed(5, 500);
					}*/
				}else {
					handler3.sendMessageDelayed(handler3.obtainMessage(1, ""), 500);
				}
				//count_text.setText("总次数："+count_time+"  成功次数："+success_count+"  失败次数："+(count_time-success_count));
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
		}
	}		
	
	private final BroadcastReceiver tReceiver = new BroadcastReceiver(){
		
		public void onReceive(Context context, Intent intent){
			String action = intent.getAction();
			if (TACTION_USB_PERMISSION.equals(action)){
				synchronized(this){
					if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)){
						if (tcard_reader != null){
							hastPermission = true;
							tgetPermission.setVisibility(View.INVISIBLE);				
							tdef_view.setText(getString(R.string.idcard_hqqxcg));
							thint_view.setText(getString(R.string.idcard_qfk));
							try {
								treader = new UsbTypeA(tUsbManager, tcard_reader);
							} catch (TelpoException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							handler3.sendMessageDelayed(handler3.obtainMessage(1, ""), 0);
						}
					}else{
						if (handler3.hasMessages(1)){
							handler3.removeMessages(1);
						}
						hastPermission = false;
						tcard_reader = null;
						tgetPermission.setVisibility(View.VISIBLE);
						tdef_view.setText(getString(R.string.idcard_qhqqx));
					}
                }					
			}else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
				// openUsbTACard();
				// fingercamera.isworking = false;
			} else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
				// openUsbTACard();
			}	
		}
	};	
												
	
	private void showtInfoView() {
		tinfo_view.setVisibility(View.VISIBLE);
		tdef_view.setVisibility(View.GONE);
		tdef_view.setText("");
	}
	
	private void showtDefaultView() {
		tinfo_view.setVisibility(View.GONE);
	    tdef_view.setVisibility(View.VISIBLE);
	    tgetPermission.setVisibility(View.VISIBLE);
	    tcardInfo.setText("");
	}
		
	public static byte[] hexStringToBytes(String hexString) {   
	    if (hexString == null || hexString.equals("")) {   
	        return null;   
	    }   
	    hexString = hexString.toUpperCase();   
	    int length = hexString.length() / 2;   
	    char[] hexChars = hexString.toCharArray();   
	    byte[] d = new byte[length];   
	    for (int i = 0; i < length; i++) {   
	        int pos = i * 2;   
	        d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));   
	    }   
	    return d;   
	}   
	
	private static byte charToByte(char c) {   
	    return (byte) "0123456789ABCDEF".indexOf(c);   
	}  
	
}