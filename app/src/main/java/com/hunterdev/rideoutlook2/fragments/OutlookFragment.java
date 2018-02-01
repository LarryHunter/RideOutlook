package com.hunterdev.rideoutlook2.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.hunterdev.rideoutlook2.R;

public class OutlookFragment extends Fragment
{
	public static OutlookFragment newInstance()
	{
		return new OutlookFragment();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View rootView = inflater.inflate(R.layout.fragment_outlook, container, false);
		return rootView;
	}
}