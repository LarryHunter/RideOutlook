package com.hunterdev.rideoutlook2.activities;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import com.hunterdev.rideoutlook2.R;
import com.hunterdev.rideoutlook2.R2;
import com.hunterdev.rideoutlook2.views.MainActivityViewPagerAdapter;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity
{
	@BindView(R2.id.activity_main_viewPager) ViewPager mainViewPager;
	@BindView(R2.id.activity_main_tabLayout) TabLayout tabLayout;

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

}
