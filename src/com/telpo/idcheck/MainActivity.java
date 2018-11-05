package com.telpo.idcheck;

import com.common.pos.api.util.posutil.PosUtil;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.telpo.simcheck.SimCheckMainActivity;
import com.telpo.tps550.api.TelpoException;
import com.telpo.tps550.api.fingerprint.FingerPrint;
import com.telpo.tps550.api.idcard.IdCard;
import com.telpo.tps550.api.idcard.Utils;
import com.telpo.tps550.api.util.StringUtil;
import com.telpo.tps550.api.util.SystemUtil;
import com.telpo.typeAcheck.SerialTACardActivity;
import com.telpo.typeAcheck.UsbTACardActivity;


public class MainActivity extends Activity {
	
	private Button idserial_mode, idusb_mode, tusb_mode, tserial_mode, 
	serialmain_mode, usbmain_mode,btmain_mode,only_idcard,only_finger,simcard_read;
	private TextView hint_view;
	private Boolean serialmode = false; 
	private Boolean usbmode = false;
	private Boolean inWindow = true;
	private boolean secondPage = false;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		 	
		new Thread(new Runnable() {
            
            @Override
            public void run() {
                try {
                	if(SystemUtil.getDeviceType() == StringUtil.DeviceModelEnum.TPS350_4G.ordinal()) {
            			PosUtil.setFingerPrintPower(PosUtil.FINGERPRINT_POWER_ON);
            		}else if(SystemUtil.getDeviceType() == StringUtil.DeviceModelEnum.TPS450.ordinal()
            				|| SystemUtil.getDeviceType() == StringUtil.DeviceModelEnum.TPS450C.ordinal()) {
            			FingerPrint.fingerPrintPower(1);
            		}
                    IdCard.open(MainActivity.this);
                } catch (TelpoException e) {
                    e.printStackTrace();
                }
            }
        }).start(); 
		
		setContentView(R.layout.activity_main);
		
		hint_view = (TextView) findViewById(R.id.select_mode);
		serialmain_mode = (Button) findViewById(R.id.serial_mode);
		usbmain_mode = (Button) findViewById(R.id.usb_mode);
		btmain_mode = (Button) findViewById(R.id.bt_mode);
		idserial_mode = (Button) findViewById(R.id.idserial_mode);
		idusb_mode = (Button) findViewById(R.id.idusb_mode);
		tusb_mode = (Button) findViewById(R.id.tusb_mode);
		tserial_mode = (Button) findViewById(R.id.tserial_mode);
		only_idcard = (Button) findViewById(R.id.only_idcard);
		only_finger = (Button) findViewById(R.id.only_finger);
		simcard_read = (Button) findViewById(R.id.simcard_read);
		simcard_read.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				startActivity(new Intent(MainActivity.this,SimCheckMainActivity.class));
			}
		});
		serialmain_mode.setVisibility(View.VISIBLE);
		usbmain_mode.setVisibility(View.VISIBLE);
		btmain_mode.setVisibility(View.VISIBLE);
		
		serialmode = false;
		usbmode = false;
		
		serialmain_mode.setOnClickListener(new OnClickListener() {
			@Override
			
			public void onClick(View v) {
				
				serialmode = true;
				secondPage = true;
				inWindow = false;
				openserial();
			}
		});
		
		usbmain_mode.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				usbmode = true;
				secondPage = true;
				inWindow = false;
				openusb();
			}
		});
		
		btmain_mode.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent intent = new Intent(MainActivity.this, BluetoothIdCardActivity.class);
				startActivity(intent);
			}
		});
		
		idserial_mode.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent intent = new Intent(MainActivity.this, SerialIdCardActivity.class);
				startActivity(intent);
			}
		});
		idusb_mode.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent intent = new Intent(MainActivity.this, UsbIdCardActivity.class);
				startActivity(intent);
			}
		});
		tusb_mode.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				Intent intent = new Intent(MainActivity.this, UsbTACardActivity.class);
				startActivity(intent);
			}
			
		});
		
		tserial_mode.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, SerialTACardActivity.class);
				startActivity(intent);
			}
			
		});
		
		only_idcard.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent intent = new Intent(MainActivity.this, OnlyIdcard.class);
				startActivity(intent);
			}
		});
		
		only_finger.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent intent = new Intent(MainActivity.this, OnlyFinger.class);
				startActivity(intent);
			}
		});
		//root for chmod video 0~15
	    new Thread(new Runnable() {
			
			@Override
			public void run() {
				String path = "/dev/video";
				for (int i = 0; i < 15; i++) {
					Utils.upgradeRootPermission(path + i);
				}
			}
		});
	} 
	
	public void onStart(Bundle savedInstanceState) {
		super.onStart();
		if(serialmode) {
			openserial();
		}else if (usbmode) {
			openusb();
		}
	}
		
	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		
		if (secondPage) {			
			idserial_mode.setVisibility(View.GONE);
			idusb_mode.setVisibility(View.GONE);
			tserial_mode.setVisibility(View.GONE);
			tusb_mode.setVisibility(View.GONE);
			serialmain_mode.setVisibility(View.VISIBLE);
			usbmain_mode.setVisibility(View.VISIBLE);
			btmain_mode.setVisibility(View.VISIBLE);
			//only_idcard.setVisibility(View.VISIBLE);
			only_finger.setVisibility(View.VISIBLE);
			secondPage = false;
		}else {
			super.onBackPressed();
		}		
	}
	
	public void openserial() {
		
		hint_view.setText(getString(R.string.card_select));
		serialmain_mode.setVisibility(View.GONE);
		usbmain_mode.setVisibility(View.GONE);
		btmain_mode.setVisibility(View.GONE);
		idserial_mode.setVisibility(View.VISIBLE);
		tserial_mode.setVisibility(View.VISIBLE);
		only_finger.setVisibility(View.GONE);
		only_idcard.setVisibility(View.GONE);
	}
	
	public void openusb() {
		
		hint_view.setText(getString(R.string.card_select));
		serialmain_mode.setVisibility(View.GONE);
		usbmain_mode.setVisibility(View.GONE);
		btmain_mode.setVisibility(View.GONE);
		idusb_mode.setVisibility(View.VISIBLE);
		tusb_mode.setVisibility(View.VISIBLE);
		only_finger.setVisibility(View.GONE);
		only_idcard.setVisibility(View.GONE);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(SystemUtil.getDeviceType() == StringUtil.DeviceModelEnum.TPS450.ordinal()
			|| SystemUtil.getDeviceType() == StringUtil.DeviceModelEnum.TPS450C.ordinal()) {
			FingerPrint.fingerPrintPower(0);
		}
		IdCard.close();
	}
}

