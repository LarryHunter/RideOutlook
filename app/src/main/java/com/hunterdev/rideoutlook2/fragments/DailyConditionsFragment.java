package com.hunterdev.rideoutlook2.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.hunterdev.rideoutlook2.R;

public class DailyConditionsFragment extends Fragment
{
	public static DailyConditionsFragment newInstance()
	{
		return new DailyConditionsFragment();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View rootView = inflater.inflate(R.layout.fragment_daily_conditions, container, false);
		return rootView;
	}
}
