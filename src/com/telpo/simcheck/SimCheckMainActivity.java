package com.telpo.simcheck;

import com.telpo.idcheck.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class SimCheckMainActivity extends Activity {
	 
	
	private Button sim_mode;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.simcheck_activity_main);
		sim_mode = (Button) findViewById(R.id.sim_mode);
	
		sim_mode.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(SimCheckMainActivity.this, UsbSimCardActivity.class);
				startActivity(intent);
			}
			
		});		
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}	
}
