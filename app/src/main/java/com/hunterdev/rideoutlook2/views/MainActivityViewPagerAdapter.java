package com.hunterdev.rideoutlook2.views;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.hunterdev.rideoutlook2.R;
import com.hunterdev.rideoutlook2.fragments.ConditionsFragment;
import com.hunterdev.rideoutlook2.fragments.DailyConditionsFragment;
import com.hunterdev.rideoutlook2.fragments.OutlookFragment;

public class MainActivityViewPagerAdapter extends FragmentStatePagerAdapter
{
	private Context m_context;
	public MainActivityViewPagerAdapter(FragmentManager fm, final Context context)
	{
		super(fm);
		m_context = context;

	}

	@Override
	public Fragment getItem(int position)
	{
		Fragment returnFragment;

		switch (position)
		{
			case 0:
				returnFragment = OutlookFragment.newInstance();
				break;
			case 1:
				returnFragment = ConditionsFragment.newInstance();
				break;
			case 2:
				returnFragment = DailyConditionsFragment.newInstance();
				break;
			default:
				return null;
		}
		return returnFragment;
	}

	@Override
	public int getCount()
	{
		// Total number of fragments
		return 3;
	}

	@Override
	public CharSequence getPageTitle(int position)
	{
		switch (position)
		{
			case 0:
				return m_context.getString(R.string.section_two_title);
			case 1:
				return m_context.getString(R.string.section_one_title);
			case 2:
				return m_context.getString(R.string.section_three_title);
			default:
				return "";
		}
	}
}
