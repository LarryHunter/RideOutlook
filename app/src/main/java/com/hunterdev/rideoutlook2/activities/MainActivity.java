package com.hunterdev.rideoutlook2.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.hunterdev.rideoutlook2.R;
import com.hunterdev.rideoutlook2.R2;
import com.hunterdev.rideoutlook2.views.MainActivityViewPagerAdapter;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity
{
	private final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 99;
	private final int CONDITIONS_FRAGMENT_ID = 1;
	private final String TAG = MainActivity.class.getSimpleName();

	@BindView(R2.id.activity_main_viewPager)
	ViewPager mainViewPager;
	@BindView(R2.id.activity_main_tabLayout)
	TabLayout tabLayout;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		ButterKnife.bind(this);
		MainActivityViewPagerAdapter adapter = new MainActivityViewPagerAdapter(getSupportFragmentManager(), getApplicationContext());
		tabLayout.setupWithViewPager(mainViewPager);
		mainViewPager.setAdapter(adapter);
		mainViewPager.setCurrentItem(1);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		String alertHeader;
		StringBuilder strBuilder = new StringBuilder();

		switch (item.getItemId())
		{
			case R.id.action_settings:
				Intent settings = new Intent(this, SettingsActivity.class);
				startActivity(settings);
				break;
			case R.id.action_about:
				alertHeader = "About Ride Outlook";
				strBuilder.append(getResources().getString(R.string.about_dialog_text));
				showAlertDialog(alertHeader, strBuilder.toString(), this);
				break;
			default:
				Log.d(TAG, "Unknown menu item selected: " + item.getItemId());
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
	{
		switch (requestCode)
		{
			case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION:
			{
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
				{
					// permission was granted! Go back to the Conditions Fragment
					mainViewPager.setCurrentItem(CONDITIONS_FRAGMENT_ID);
				} else
				{
					// permission denied! Notify user and exit
					showAlertDialog("Permission Required", permissions.toString() + " required.", this);
					finish();
				}
				return;
			}

			// other 'case' lines to check for other
			// permissions this app might request.
		}
	}

	public static void showAlertDialog(String titleString, String displayString, Context context)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(titleString).setMessage(displayString).setIcon(R.drawable.ic_ride_outlook);
		builder.setPositiveButton("OK", null);
		builder.setCancelable(false);
		builder.show();
	}
}
