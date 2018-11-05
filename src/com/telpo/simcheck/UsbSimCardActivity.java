package com.telpo.simcheck;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import com.telpo.simcheck.ShellUtils;
import com.telpo.idcheck.R;
import com.telpo.simcheck.IcCardInfo;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class UsbSimCardActivity extends Activity {
	
	private Button getpermission, getef, getid, poweron, poweroff, clear;
	private TextView cardInfo, hint_view, def_view, test_info_view;	
	private SoundPool tsoundPool;
	private IcCardInfo icinfo;
	private View tinfo_view;
	private PendingIntent PermissionIntent;//related to usb
	private static final String TACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
	private UsbManager tUsbManager;
	private UsbDevice card_reader;
	private boolean hastReader = false;
	private EditText mEditTextApdu;
	private PoweronTask pAsyncTask;
	private GetIccidTask iccidAsyncTask;
	private GetRealIccidTask realiccidAsyncTask;
	private PoweroffTask offAsyncTask;
	private Handler handler3 = new Handler() {
		
		@Override
		public void handleMessage(Message msg) {
		    switch (msg.what) {
		    
		    case 1:		        
		    	pAsyncTask = new PoweronTask();
		    	pAsyncTask.execute();		        
			    break;
		    case 2:
		    	iccidAsyncTask = new GetIccidTask();
		    	iccidAsyncTask.execute();
		    	break;
			
		    case 3:
		    	realiccidAsyncTask = new GetRealIccidTask();
		    	realiccidAsyncTask.execute();
		    	
		    	
		    	
		    	break;		    
		    case 4:
		    	offAsyncTask = new PoweroffTask();
		    	offAsyncTask.execute();
		    	break;
		    }	
		    super.handleMessage(msg);
		}
	};	
	
	
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.simcheck);
		def_view = (TextView) findViewById(R.id.default_view);
		hint_view = (TextView) findViewById(R.id.hint_view);
		ShellUtils.execCommand("echo 3 > /sys/class/telpoio/power_status", false);
//		opencardreader = (Button) findViewById(R.id.open_cardreader);
//		poweron = (Button) findViewById(R.id.power_on);		
		getef = (Button) findViewById(R.id.open_getef);
		getid = (Button) findViewById(R.id.open_getid);
		getpermission = (Button) findViewById(R.id.open_qhqqx);
		poweroff = (Button) findViewById(R.id.open_poweroff);
		poweron = (Button) findViewById(R.id.open_getatr);
		clear = (Button) findViewById(R.id.open_clear);
		cardInfo = (TextView) findViewById(R.id.showData);
		hint_view.setText(getString(R.string.idcard_qhqqx));
		hint_view.setVisibility(View.VISIBLE);
		icinfo = new IcCardInfo();
		getpermission.setOnClickListener(new View.OnClickListener(){
			
			@Override
			public void onClick(View v) {
				
				openUsb();			
			}
		});
		
		getef.setOnClickListener(new View.OnClickListener(){
			public void onClick(View v) {
				
				handler3.sendMessageDelayed(handler3.obtainMessage(2,""),0);
			}
		});
		
		poweron.setOnClickListener(new View.OnClickListener(){
			public void onClick(View v) {
				handler3.sendMessageDelayed(handler3.obtainMessage(1,""),0);
			}			
		});
		
		getid.setOnClickListener(new View.OnClickListener(){
			public void onClick(View v) {
				
				handler3.sendMessageDelayed(handler3.obtainMessage(3,""),0);
			}
		});
		
		clear.setOnClickListener(new View.OnClickListener(){
			public void onClick(View v) {
				
				openUsb();
				cardInfo.setText(null);
				getid.setEnabled(false);
				poweroff.setEnabled(false);
				getef.setEnabled(false);
				poweron.setEnabled(true);
			}
		});
		
		poweroff.setOnClickListener(new View.OnClickListener(){
			public void onClick(View v) {
				handler3.sendMessageDelayed(handler3.obtainMessage(4,""),0);
			}			
		});
		
		PermissionIntent = PendingIntent.getBroadcast(UsbSimCardActivity.this, 0, new Intent(TACTION_USB_PERMISSION), 0);
		IntentFilter tfilter = new IntentFilter();
		tfilter.addAction(TACTION_USB_PERMISSION);
		tfilter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		tfilter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
		tfilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
		tfilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		registerReceiver(tReceiver, tfilter);
	
	}
	
	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		openUsb();//if has permission, button hide
		cardInfo.setText(null);
		getid.setEnabled(false);
		poweroff.setEnabled(false);
		getef.setEnabled(false);
		poweron.setEnabled(true);
	}
	
	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		if (handler3.hasMessages(1)) {
			handler3.removeMessages(1);
		}
		if (handler3.hasMessages(2)) {
			handler3.removeMessages(2);
		}
		if (handler3.hasMessages(3)) {
			handler3.removeMessages(3);
		}
		if (handler3.hasMessages(4)) {
			handler3.removeMessages(4);
		}
	}

	
	public void openUsb(){
		
		tUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		HashMap<String, UsbDevice> tdeviceHashMap = tUsbManager.getDeviceList();
		Iterator<UsbDevice> iterator = tdeviceHashMap.values().iterator();
		hastReader = false;
		
		while (iterator.hasNext()) {
			UsbDevice tusbDevice = iterator.next();
			int tpid = tusbDevice.getProductId();
			int tvid = tusbDevice.getVendorId();
			if ((tpid == 0x5750 && tvid == 0x0483)
					||(tpid == 0xc35a && tvid == 0x0400)
					||(tpid == 0x028A && tvid == 0x28E9)){
				hastReader = true;
				card_reader = tusbDevice;
				if (tUsbManager.hasPermission(tusbDevice)){
					getpermission.setVisibility(View.INVISIBLE);				
					def_view.setVisibility(View.INVISIBLE);
					poweron.setVisibility(View.VISIBLE);
					getid.setVisibility(View.VISIBLE);
					getef.setVisibility(View.VISIBLE);
					hint_view.setText(getString(R.string.bl_hqqxcg));
					hint_view.setVisibility(View.VISIBLE);
					clear.setVisibility(View.VISIBLE);
					getid.setEnabled(false);
					poweroff.setEnabled(false);
					getef.setEnabled(false);
					poweroff.setVisibility(View.VISIBLE);
					break;
				}else{

					tUsbManager.requestPermission(tusbDevice, PermissionIntent);//***弹出对话框问是否允许的时�?
				}
			}
		}
//		
//
	}
	
	private final BroadcastReceiver tReceiver = new BroadcastReceiver(){
		
		public void onReceive(Context context, Intent intent){
			String action = intent.getAction();
			if (TACTION_USB_PERMISSION.equals(action)){
				synchronized(this){
					if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)){
						if (card_reader != null){

							getpermission.setVisibility(View.INVISIBLE);				
							def_view.setVisibility(View.INVISIBLE);
							poweron.setVisibility(View.VISIBLE);
							getid.setVisibility(View.VISIBLE);
							getef.setVisibility(View.VISIBLE);
							hint_view.setText(getString(R.string.bl_hqqxcg));
							hint_view.setVisibility(View.VISIBLE);
							getef.setEnabled(false);
							getid.setEnabled(false);
							poweroff.setEnabled(false);
							poweroff.setVisibility(View.VISIBLE);
							clear.setVisibility(View.VISIBLE);
						}
					}else{
						if (handler3.hasMessages(4)){
							handler3.removeMessages(4);
						}
						getpermission.setVisibility(View.VISIBLE);
						
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
	
	
	private class PoweronTask extends AsyncTask<Void, Integer, Boolean>{
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			
		}
			
		@Override
		protected Boolean doInBackground(Void... arg){		
			
			powerOn();
			if (icinfo.getatr() != null) {
				return true;
			}else return false;
		}
			
		@Override
		protected void onPostExecute(Boolean result){
			super.onPostExecute(result);
			if (result) {
				cardInfo.setVisibility(View.VISIBLE);
				cardInfo.setText(getString(R.string.bl_atr) +" "+ icinfo.getatr());
				getef.setEnabled(true);
				poweroff.setEnabled(true);
				poweron.setEnabled(false);
				hint_view.setText(getString(R.string.bl_simysd));
			}else if(!result){
				if(icinfo.getatr() == null) {
					hint_view.setText(getString(R.string.bl_wck));
				}else {
					handler3.sendMessageDelayed(handler3.obtainMessage(1, ""), 100);
				}												
			}else {
				hint_view.setText(getString(R.string.bl_sdsb));
			}
		}
	}
	
	private class GetIccidTask extends AsyncTask<Void, Integer, Boolean>{
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			
		}
			
		@Override
		protected Boolean doInBackground(Void... arg){
			
			byte[] firststep = getIccid();
			if(firststep != null) {
				icinfo.setresp(decodetcarduid(firststep)); 
				return true;
			}else return false;
			
		}
			
		@Override
		protected void onPostExecute(Boolean result){
			super.onPostExecute(result);
			if (result) {
				cardInfo.setText(getString(R.string.bl_atr) +" "+ icinfo.getatr() + "\n\n"
						+ getString(R.string.bl_iccidresp) +" "+ icinfo.getresp() );
				getid.setEnabled(true);
				hint_view.setText(getString(R.string.bl_jrcg));
			}else if (!result){
				handler3.sendMessageDelayed(handler3.obtainMessage(2, ""), 100);				
			}else {
				hint_view.setText(getString(R.string.bl_jrsb));
			}
		}
	}
	
	private class GetRealIccidTask extends AsyncTask<Void, Integer, Boolean>{
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			
		}
			
		@Override
		protected Boolean doInBackground(Void... arg){
			
			getrealiccid();
			if(icinfo.geticcid() != null) {
				return true;
			}else return false;
		}
			
		@Override
		protected void onPostExecute(Boolean result){
			super.onPostExecute(result);
			if (result) {
				cardInfo.setText(getString(R.string.bl_atr)+" " + icinfo.getatr() + "\n\n"
						+ getString(R.string.bl_iccidresp)+" " + icinfo.getresp() + "\n\n"						
						+ getString(R.string.bl_iccid)+" " + icinfo.geticcid());
				hint_view.setText(getString(R.string.bl_iccidcg));
			}else if(!result) {
				handler3.sendMessageDelayed(handler3.obtainMessage(3, ""), 100);
			}else {
				hint_view.setText(getString(R.string.bl_iccidsb));				
			}			
		}
		
	}
	
	private class PoweroffTask extends AsyncTask<Void, Integer, Boolean>{
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			
		}
			
		@Override
		protected Boolean doInBackground(Void... arg){		
			
			Boolean ifpoweroff = poweroff();
			return ifpoweroff;
		}
			
		@Override
		protected void onPostExecute(Boolean result){
			super.onPostExecute(result);
			if (result) {
				cardInfo.setVisibility(View.VISIBLE);
				cardInfo.setText(null);
				getef.setEnabled(false);
				poweroff.setEnabled(false);
				poweron.setEnabled(true);
				getid.setEnabled(false);
				hint_view.setText(getString(R.string.bl_simxdcg));
			}else {
				hint_view.setText(getString(R.string.bl_simxdsb));				
			}			
		}
	}
	
	
	private void powerOn(){
		
		byte[] cmd_read_uid = new byte[]{0x62, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x62};
		byte[] uid = requestatr( cmd_read_uid, new byte[]{ (byte) 0x80});		
		String atr = decodetcarduid(uid);
		Log.e("atr", ""+atr);
		icinfo.setatr(atr);
	}
		
	public byte[] requestatr(byte[] tcmd, byte[] sw) {  
		
		if (card_reader != null && sw.length == 1) {
			
			UsbInterface tInterface = card_reader.getInterface(0);
			UsbEndpoint iEndpoint = tInterface.getEndpoint(0);
			UsbEndpoint oEndpoint = tInterface.getEndpoint(1);
			UsbDeviceConnection tconnection;
			
			tconnection = tUsbManager.openDevice(card_reader);
			
			if (tconnection == null) {
				if (handler3.hasMessages(1)) {
					handler3.removeMessages(1);
				}
				return null;
			}	
			tconnection.claimInterface(tInterface, true);
			int output = tconnection.bulkTransfer(oEndpoint, tcmd, tcmd.length, 3000);	
			
			try{
				Thread.sleep(500);
			} catch (InterruptedException a) {
				a.printStackTrace();
			}		
			byte[]rec = new byte[100];
			
			int input = tconnection.bulkTransfer(iEndpoint, rec, rec.length, 3000);				
			for(int i = 0; i <rec.length; i++) {
				Log.e("return", ""+rec[i]);
			}
			tconnection.close();
			return Arrays.copyOfRange(rec, 10, 10+rec[1]);
		}		
		return null;
	}
	
	private byte[] getIccid(){
		
		byte[] cardcode = new byte[18];
		byte[] cardcodeold = new byte[]{0x6F, 0x07, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,(byte)0xA0, (byte)0xA4, 0x00, 0x00, 0x02, (byte)0x2F, (byte)0xE2};
		System.arraycopy(cardcodeold, 0, cardcode, 0, 17);
		cardcode[17] = crc(cardcodeold);
		Log.e("crc", ""+cardcode[17]);
		byte[] firststep = requesticcid( cardcode, new byte[] { (byte)0x9F});
		return firststep;
	}
	
	public byte[] requesticcid(byte[] tcmd, byte[] sw) {  
		
		if (card_reader != null && sw.length == 1) {
			
			UsbInterface tInterface = card_reader.getInterface(0);
			UsbEndpoint iEndpoint = tInterface.getEndpoint(0);
			UsbEndpoint oEndpoint = tInterface.getEndpoint(1);
			UsbDeviceConnection tconnection;
			
			tconnection = tUsbManager.openDevice(card_reader);
			
			if (tconnection == null) {
				if (handler3.hasMessages(2)) {
					handler3.removeMessages(2);
				}
				return null;
			}	
			tconnection.claimInterface(tInterface, true);
			int output = tconnection.bulkTransfer(oEndpoint, tcmd, tcmd.length, 3000);	
			
			try{
				Thread.sleep(500);
			} catch (InterruptedException a) {
				a.printStackTrace();
			}		
			byte[]rec = new byte[100];			
			int input = tconnection.bulkTransfer(iEndpoint, rec, rec.length, 3000);				
			for(int i = 0; i <rec.length; i++) {
				Log.e("return", ""+rec[i]);
			}
			tconnection.close();
			if (rec[10] == (byte) 0x9F) {
				Log.e("iccid", "9f00");
				return Arrays.copyOfRange(rec, 10, 10+rec[1]);
			}else {
				return null;
			}			
		}		
		return null;
	}
	
	
	public void getrealiccid() {
		byte[] cardcodereal = new byte[16];
		byte[] cardcoderealold = new byte[]{0x6F, 0x05, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,(byte)0xA0, (byte)0xB0, 0x00, 0x00, 0x0A};
		System.arraycopy(cardcoderealold, 0, cardcodereal, 0, 15);
		cardcodereal[15] = crc(cardcoderealold);
		byte[] cardcode = requestrealiccid(cardcodereal);
		if(cardcode != null) {
			String cardcodenew = decodetcarduid(cardcode);
			StringBuffer newcode = new StringBuffer();
			for (int i = 0; i < cardcodenew.length(); i++) {
				if( (i+1)%2 == 1) {					
					newcode.append(cardcodenew.substring(i+1,i+2));
				}else if ((i+1)%2 == 0) {
					newcode.append(cardcodenew.substring(i-1,i));
				}
			}
			Log.e("string",cardcodenew);
			cardcodenew = newcode.toString();
			Log.e("string",cardcodenew);
			icinfo.seticcid(cardcodenew);
		}else {
			icinfo.seticcid(null);
		}		
	}
	
	
	public byte[] requestrealiccid(byte[] cardcode) {  
		
		if (card_reader != null) {
			
			UsbInterface tInterface = card_reader.getInterface(0);
			UsbEndpoint iEndpoint = tInterface.getEndpoint(0);
			UsbEndpoint oEndpoint = tInterface.getEndpoint(1);
			UsbDeviceConnection tconnection;
			
			tconnection = tUsbManager.openDevice(card_reader);
			
			if (tconnection == null) {
				if (handler3.hasMessages(2)) {
					handler3.removeMessages(2);
				}
				return null;
			}	
			tconnection.claimInterface(tInterface, true);
			int output = tconnection.bulkTransfer(oEndpoint, cardcode, cardcode.length, 3000);	
			
			try{
				Thread.sleep(500);
			} catch (InterruptedException a) {
				a.printStackTrace();
			}		
			byte[]rec = new byte[100];
			
			int input = tconnection.bulkTransfer(iEndpoint, rec, rec.length, 3000);				
			tconnection.close();
			for(int i = 0; i <rec.length; i++) {
				Log.e("return", ""+rec[i]);
			}			
			if(rec[1] != 0) {
				byte[] newrec = Arrays.copyOfRange(rec, 10, 10+rec[1]-2);
				return newrec;
			}else {
				return null;
			}			
		}		
		return null;
	}
	
	private Boolean poweroff() {
		
		byte[] cmd_read_uid = new byte[]{0x63, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x63};
		Boolean uid = requestslot( cmd_read_uid, new byte[]{ (byte) 0x81});				
		return uid;
	}
	
	public Boolean requestslot(byte[] tcmd, byte[] sw) {  
		
		if (card_reader != null && sw.length == 1) {
			
			UsbInterface tInterface = card_reader.getInterface(0);
			UsbEndpoint iEndpoint = tInterface.getEndpoint(0);
			UsbEndpoint oEndpoint = tInterface.getEndpoint(1);
			UsbDeviceConnection tconnection;
			
			tconnection = tUsbManager.openDevice(card_reader);
			
			if (tconnection == null) {
				if (handler3.hasMessages(1)) {
					handler3.removeMessages(1);
				}
				return null;
			}	
			tconnection.claimInterface(tInterface, true);
			int output = tconnection.bulkTransfer(oEndpoint, tcmd, tcmd.length, 3000);	
			
			try{
				Thread.sleep(500);
			} catch (InterruptedException a) {
				a.printStackTrace();
			}		
			byte[]rec = new byte[100];
			
			int input = tconnection.bulkTransfer(iEndpoint, rec, rec.length, 3000);				
			for(int i = 0; i <rec.length; i++) {
				Log.e("return", ""+rec[i]);
			}
			tconnection.close();
			if(rec[0] == (byte) 0x81) {
				return true;
			}else {
				return false;
			}
		}		
		return false;
	}
	
	private byte crc(byte[] data){
	    
		int i; 
	    byte crc=0x00;
	    
	    for(byte b:data) {
	    	crc ^= b;  
	    }
	    return crc;
	}
	
	private static String decodetcarduid(byte[] uid) {
		StringBuilder ret = new StringBuilder("");
		for (int i = 0; i < uid.length; i++) {
			int v = uid[i] & 0xff;
			String hex = Integer.toHexString(v);
		    if (hex.length() == 1) {
		    	hex = '0' + hex;
		    	}
		    ret.append(hex);
		    }
		return ret.toString();
	}
}
