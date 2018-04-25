package com.hunterdev.rideoutlook2.activities;

import android.Manifest;
import android.Manifest.permission;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;

import com.hunterdev.rideoutlook2.R;

public class PermissionsActivity extends AppCompatActivity
{
	private Button m_okButton;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_permissions);
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		m_okButton = findViewById(R.id.ok_button);
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		m_okButton.setOnClickListener(new View.OnClickListener()
		{
			public static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 99;

			@Override
			public void onClick(View view)
			{
				// Request location lookup permission
				ActivityCompat.requestPermissions(PermissionsActivity.this,
					new String[]{permission.ACCESS_FINE_LOCATION},
					MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
			}
		});
	}
}
