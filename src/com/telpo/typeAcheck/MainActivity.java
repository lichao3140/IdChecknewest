package com.telpo.typeAcheck;

import com.telpo.typeAcheck.SerialTACardActivity;
import com.telpo.typeAcheck.UsbTACardActivity;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import com.telpo.idcheck.R;
import com.telpo.tps550.api.idcard.IdCard;
import com.telpo.tps550.api.TelpoException;


public class MainActivity extends Activity {
	
	private Button idserial_mode, idusb_mode, tusb_mode, tserial_mode;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        new Thread(new Runnable() {
            
            @Override
            public void run() {
                try {
                    IdCard.open(MainActivity.this);
                } catch (TelpoException e) {
                    e.printStackTrace();
                }
            }
        }).start(); 
		
		setContentView(R.layout.activity_main);
		tusb_mode = (Button) findViewById(R.id.tusb_mode);
		tserial_mode = (Button) findViewById(R.id.tserial_mode);		
		
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
				
				try {
					IdCard.open(MainActivity.this);
				} catch (TelpoException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				Intent intent = new Intent(MainActivity.this, SerialTACardActivity.class);
				startActivity(intent);
			}
			
		});
	}    

	@Override
	protected void onDestroy() {
		super.onDestroy();
		IdCard.close();
	}
}

