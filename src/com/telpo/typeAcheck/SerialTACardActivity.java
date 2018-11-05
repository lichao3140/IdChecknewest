package com.telpo.typeAcheck;

import com.telpo.idcheck.R;
import com.telpo.tps550.api.TelpoException;
import com.telpo.tps550.api.typea.SerialTypeACard;
import com.telpo.tps550.api.typea.TAInfo;
import com.telpo.tps550.api.typea.TASectorInfo;

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

public class SerialTACardActivity extends SerialPortActivity{

	private byte[] Password, tbuffer = new byte[512];
	private byte[] Newwritedata;
	private byte[] compensate = new byte[] {};
	private int tsize, tn, begin = 0;
	private Button tgetpermission, pwConfirm, writeData,writeDataConfirm,read_again;
	private TextView tahint, tcardInfo, thint_view, tdef_view, ttest_info_view;
	private EditText pwCheck, dataWrite;
	private SerialTypeACard reader;
	private TAInfo tinfo;
	private TASectorInfo tsectorinfo;
	private SoundPool tsoundPool;
	private View tinfo_view;
	private GetTAInfoTask tAsyncTask;
	private PwCheckTask checkAsyncTask;
	private WriteDataTask dataAsyncTask;
	private ReadDataTask readAsynctTask;
	private boolean isCardPressing = true;
	private boolean isUsingtUsb = false;
	private boolean hastReader = true;
	private boolean istFinish = false;
	private Boolean writeright = false;
	
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
		    	readAsynctTask = new ReadDataTask();
		    	readAsynctTask.execute();
		    	break;
		    case 4:
		    	dataAsyncTask = new WriteDataTask();
		    	dataAsyncTask.execute();
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
		
		pwCheck = (EditText) findViewById(R.id.edittext1);
		pwConfirm = (Button) findViewById(R.id.passwordconfirm);
		writeData = (Button) findViewById(R.id.writedata);
		dataWrite = (EditText) findViewById(R.id.edittext2);		
		tahint = (TextView) findViewById(R.id.tAhint);
		tcardInfo = (TextView) findViewById(R.id.showData);
		tinfo_view = findViewById(R.id.relativeLayout);
		thint_view = (TextView) findViewById(R.id.hint_view);
		tdef_view = (TextView) findViewById(R.id.default_view);
		ttest_info_view = (TextView) findViewById(R.id.test_info_view);
		writeDataConfirm = (Button) findViewById(R.id.writedataconfirm);
		read_again = (Button) findViewById(R.id.read_again);
		read_again.setVisibility(View.VISIBLE);
		thint_view.setText(getString(R.string.idcard_qfk));
		tgetpermission = (Button)findViewById(R.id.requestPermission);
		tgetpermission.setVisibility(View.GONE);
		tahint.setVisibility(View.VISIBLE);
		Log.e("OnCreate","aaa");
		try {
			reader = new SerialTypeACard(mOutputStream);
		} catch (TelpoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		pwConfirm.setOnClickListener(new View.OnClickListener(){
			
			@Override
			public void onClick(View v) {
				openLockCheck();
				pwConfirm.setEnabled(false);
				read_again.setEnabled(false);
			}
		});
		
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
				
				thint_view.setText(getString(R.string.idcard_qfk));
				tdef_view.setVisibility(View.GONE);
				pwConfirm.setVisibility(View.GONE);
				pwCheck.setVisibility(View.GONE);
				dataWrite.setVisibility(View.GONE);
				tcardInfo.setVisibility(View.GONE);
				writeData.setVisibility(View.GONE);		
				writeDataConfirm.setVisibility(View.GONE);
				
				reader = null;
				try {
					reader = new SerialTypeACard(mOutputStream);
				} catch (TelpoException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				handler3.sendMessageDelayed(handler3.obtainMessage(1, ""), 100);
			}
		});
	}
	
	protected void onDestroy(){
		super.onDestroy();
	//***	can delete	origin: unregisterReceiver(tReceiver);
		while(isUsingtUsb);
	}

	protected void onStart(){
		super.onStart();
		
		thint_view.setText(getString(R.string.idcard_qfk));
		tdef_view.setVisibility(View.GONE);
		pwConfirm.setVisibility(View.GONE);
		pwCheck.setVisibility(View.GONE);
		dataWrite.setVisibility(View.GONE);
		tcardInfo.setVisibility(View.GONE);
		writeData.setVisibility(View.GONE);		
		writeDataConfirm.setVisibility(View.GONE);
		istFinish = false;
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
		if(handler3.hasMessages(4)) {
			handler3.removeMessages(4);
		}
		pwCheck.setText("");
		dataWrite.setText("");
		istFinish = true;
	}

	protected void onResume() {
		super.onResume();
		thint_view.setText(getString(R.string.idcard_qfk));
		//may be wait for add;
		if(isCardPressing == true) {
			handler3.sendMessageDelayed(handler3.obtainMessage(1, ""), 0);
		}
	}
	
	public void onBackPressed(){
		super.onBackPressed();
		if(handler3.hasMessages(1)) {
			handler3.removeMessages(1);
		}	
		if(handler3.hasMessages(2)) {
			handler3.removeMessages(2);
		}
		if(handler3.hasMessages(3)) {
			handler3.removeMessages(3);
		}
		if(handler3.hasMessages(4)) {
			handler3.removeMessages(4);
		}
		istFinish = true;
	}

	public void openLockCheck() {
		
		String password = new String(pwCheck.getText().toString());
		Password = hexStringToBytes(password);
		try {
			reader.transmitpassword(Password);
		} catch (TelpoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (pwCheck.getText() != null ) {
			handler3.sendMessageDelayed(handler3.obtainMessage(2,""), 0);
		}else if ( pwCheck.getText() == null) {
			thint_view.setText(getString(R.string.tacard_mybnwk));
		}
	}	
	
	public void openWriteData() {
		
		if ( hastReader ) {
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
		
		String newwritedata = new String(dataWrite.getText().toString());
		Newwritedata = hexStringToBytes(newwritedata);
		try {
			reader.transmitNewwritedata(Newwritedata);
		} catch (TelpoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//tbuffer = new byte[20];
		tbuffer = new byte[512];
		begin = 0;
		if ( hastReader ) {
			handler3.sendMessageDelayed(handler3.obtainMessage(4,""), 0);
		}else if (!hastReader){
			thint_view.setText(getString(R.string.idcard_wfljdkq));
		}
	}
	
    
	private class GetTAInfoTask extends AsyncTask<Void, Integer, Boolean>{
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			
			tinfo = null;
			//start_time = System.nanoTime();
			//
			if(reader == null) {
				try {
					reader = new SerialTypeACard(mOutputStream);
				} catch (TelpoException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			read_again.setEnabled(false);
		}
		
		@Override
		protected Boolean doInBackground(Void... arg0){
			
			try {
				reader.checkTACard();
			} catch (TelpoException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
			/*try{
				Thread.sleep(1000);
			} catch (InterruptedException a) {
				a.printStackTrace();
			}*/
			try {
				tinfo = reader.requestTACard();
			} catch (TelpoException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (tinfo != null){
				return true;				
			}
			return false;
		}
			
		@Override
		protected void onPostExecute(Boolean result){
			super.onPostExecute(result);
			if (result) {
				tahint.setVisibility(View.INVISIBLE);
				tcardInfo.setText(getString(R.string.idcard_kh) + tinfo.getNum());
				tcardInfo.setVisibility(View.VISIBLE);
				showtInfoView();
				thint_view.setText(getString(R.string.tacard_myrz));
				pwCheck.setVisibility(View.VISIBLE);
				pwConfirm.setVisibility(View.VISIBLE);
				tbuffer = new byte[512];
				try {
					reader.transmittbuffer(tbuffer);
				} catch (TelpoException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				begin = 0;
				read_again.setEnabled(true);
				
			}else {
				begin = 0;
				handler3.sendMessageDelayed(handler3.obtainMessage(1, ""),  100);
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
				reader.checkPW();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
			try{
				Thread.sleep(100);
			} catch (InterruptedException a) {
				a.printStackTrace();
			}
			pwright = reader.requestpw();	
			return pwright;			
		}
		
		@Override 
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			if (result == true) {
				tahint.setVisibility(View.INVISIBLE);
				thint_view.setText(getString(R.string.tacard_zzdqknsj));//密钥验证成功,正在读取卡内数据
				tcardInfo.setVisibility(View.VISIBLE);
				pwConfirm.setVisibility(View.VISIBLE);
				pwCheck.setVisibility(View.VISIBLE);
				tbuffer = new byte[40];
				try {
					reader.transmittbuffer(tbuffer);
				} catch (TelpoException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				begin = 0;
				pwConfirm.setEnabled(true);
				read_again.setEnabled(true);
				handler3.sendMessageDelayed(handler3.obtainMessage(3, ""), 100);
			}else if (result == false){
				
				tbuffer = new byte[512];
				try {
					reader.transmittbuffer(tbuffer);
				} catch (TelpoException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				begin = 0;
				thint_view.setText(getString(R.string.tacard_mycw));	
				pwConfirm.setEnabled(true);
				read_again.setEnabled(true);
			}			
		}
	}
	
	
	private class ReadDataTask extends AsyncTask<Void, Integer, Boolean> {
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();			
		}
		
		@Override
		protected Boolean doInBackground(Void...  arg4) {
			
			tbuffer = new byte[40];
			try {
				if(reader != null) {
					reader.transmittbuffer(tbuffer);
				}
			} catch (TelpoException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			begin = 0;
			try {
				reader.readData();
			} catch (TelpoException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try{
				Thread.sleep(300);
			} catch (InterruptedException a) {
				a.printStackTrace();
			}
			tsectorinfo = reader.requestData();
			if (tsectorinfo != null) {
				return true;
			}else {
				return false;
			}
				
		}
			
		@Override
		protected void onPostExecute(Boolean result) {
			
			if (result) {
				tcardInfo.setText(getString(R.string.idcard_kh) + tinfo.getNum() + "\n\n"
						+ getString(R.string.tacard_knsj) + tsectorinfo.getSectorData());
				tcardInfo.setVisibility(View.VISIBLE);
				writeData.setVisibility(View.VISIBLE);	
				thint_view.setText(getString(R.string.tacard_knsjydq));//卡内数据已读取
				pwConfirm.setVisibility(View.INVISIBLE);
				pwCheck.setVisibility(View.INVISIBLE);
				tbuffer = new byte[512];
				try {
					reader.transmittbuffer(tbuffer);
				} catch (TelpoException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				begin = 0;
			}else {
				Log.e("readdatafail", "readdatafail");
				//handler3.sendMessageDelayed(handler3.obtainMessage(3, ""), 100);
			}
		}
	}	
		
	
	private class WriteDataTask extends AsyncTask<Void, Integer, Boolean> {
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();	
			read_again.setEnabled(false);
		}
		
		@Override
		protected Boolean doInBackground(Void...  arg2) {
			
			Boolean writeinsuccess = false;
			try {
				writeright = reader.writeInData();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try{
				Thread.sleep(2000);
			} catch (InterruptedException a) {
				a.printStackTrace();
			}
			if (writeright = true) {
				writeinsuccess = reader.requestwrite();
			}
			return writeinsuccess;
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			if (result == true) {
				thint_view.setText(getString(R.string.tacard_xrsjcg));
				tbuffer = new byte[512];
				try {
					reader.transmittbuffer(tbuffer);
				} catch (TelpoException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				begin = 0;
			}else if (result == false) {
				if( Newwritedata == null ) {
					openWriteData();
					thint_view.setText(getString(R.string.tacard_xrsjbnwk));
					tcardInfo.setText(getString(R.string.idcard_kh) + tinfo.getNum() + "\n\n"
							+ getString(R.string.tacard_knsj) + tsectorinfo.getSectorData());
					tbuffer = new byte[512];
					try {
						reader.transmittbuffer(tbuffer);
					} catch (TelpoException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					begin = 0;
				}else {
					openWriteData();
					thint_view.setText(getString(R.string.tacard_xrsjsb));
					tbuffer = new byte[512];
					try {
						reader.transmittbuffer(tbuffer);
					} catch (TelpoException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					begin = 0;
				}				
			}		
			read_again.setEnabled(true);
		}
	}

	
	@Override
	protected void onDataReceived(final byte[] buffer, final int size, final int n) {
		// TODO Auto-generated method stub
		
		runOnUiThread(new Runnable() {
			public void run() {				
				
				for(int i = 0; i < size; i++) {
					tbuffer[begin + i] = buffer[i];
				}
				begin = begin + size;

				try {
					reader.transmittbuffer(tbuffer);
				} catch (TelpoException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}				
		});
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

	private void showtInfoView() {
		tinfo_view.setVisibility(View.VISIBLE);
		tdef_view.setVisibility(View.GONE);
		tdef_view.setText("");
	}
	
	private byte crc(byte[] data){
	    
		int i; 
	    byte crc=0x00;
	    
	    for(byte b:data) {
	    	crc ^= b;  
	    }
	    return crc;
	}
	
}
