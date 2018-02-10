package com.hunterdev.rideoutlook2.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.hunterdev.rideoutlook2.R;
import com.hunterdev.rideoutlook2.logic.Deserializer;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.content.Context.LOCATION_SERVICE;

public class ConditionsFragment extends Fragment implements LocationListener, TextToSpeech.OnInitListener
{

    public static ConditionsFragment newInstance()
	{
		return new ConditionsFragment();
	}

	@BindView(R.id.conditions_progress_bar)
	ProgressBar m_conditionsProgressBar;


	private LocationManager m_locManager;
	private TextToSpeech m_textToSpeech;
	private FloatingActionButton m_speakConditionsFAB;

    private final String API_REFERENCE_CODE = "e2b6eaf41ee74858";
	private final String UNIT_OF_MEASUREMENT = "unit_of_measurement";
	private final String APP_RATED = "app_rated";
	private final String USE_COUNT_FOR_RATING = "use_count";
	private final int NUM_USES_BETWEEN_RATING_REQUEST = 3;
	private final String PREF_NAME = "com.hunterdev.rideoutlook2";

	private final String weatherConditionsUrl = "http://api.wunderground.com/api/" + API_REFERENCE_CODE + "/conditions/q/";
	private final String weatherIconUrl = "http://icons.wxug.com/i/c/c/";
    private final String wundergroundReferralUrl = "http://www.wunderground.com/?apiref=4e313604ca87365f";

	private String unitOfMeasurement = "";
	private Boolean didUserRateApp = false;
	private int ratingUseCount = 0;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View rootView = inflater.inflate(R.layout.fragment_conditions, container, false);
		ButterKnife.bind(this, rootView);

		final View coordinatorLayoutView = rootView.findViewById(R.id.snackbarPosition);
		final View.OnClickListener clickListener = new View.OnClickListener()
		{
			public void onClick(View v)
			{
				Toast.makeText(getActivity(), "Add audio muting code here...", Toast.LENGTH_SHORT).show();
				m_conditionsProgressBar.setVisibility(View.INVISIBLE);
			}
		};

		m_speakConditionsFAB = (FloatingActionButton) rootView.findViewById(R.id.myFAB);
		m_speakConditionsFAB.setVisibility(View.INVISIBLE);
		m_speakConditionsFAB.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				m_conditionsProgressBar.setVisibility(View.VISIBLE);

				Snackbar
					.make(coordinatorLayoutView, R.string.snackbar_text, Snackbar.LENGTH_LONG)
					.setAction(R.string.snackbar_action_undo, clickListener)
					.show();
			}
		});

		TextView weatherUndergroundUrlLink = (TextView) rootView.findViewById(R.id.weatherunderground_url_link);
		weatherUndergroundUrlLink.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				goToWeatherUndergroundWebsite();
			}

			private void goToWeatherUndergroundWebsite()
			{
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(wundergroundReferralUrl));
                startActivity(browserIntent);
			}
		});

		getPreferences();
		initialize();
		getLocation();
		promptUserToRateApp();

		return rootView;
	}

	private void getPreferences()
	{
		SharedPreferences prefs = getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

		// Setup the default unit of measurement
		unitOfMeasurement = prefs.getString(UNIT_OF_MEASUREMENT, "F");

		// Check to see if the user has rated app or chose not to
		didUserRateApp = prefs.getBoolean(APP_RATED, false);

		// Count number of times user launched the app ??? For rating app.
		if (!didUserRateApp) { ratingUseCount = prefs.getInt(USE_COUNT_FOR_RATING, 0); }
	}

	private void promptUserToRateApp()
	{
		if (!didUserRateApp)
		{
			if (NUM_USES_BETWEEN_RATING_REQUEST <= ratingUseCount)
			{
				showDialogForAppRating();
			}
			ratingUseCount++;
			saveRatingInfo();
		}
	}

	private void saveRatingInfo()
	{
		SharedPreferences prefs = getActivity().getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor prefEditor = prefs.edit();
		prefEditor.putInt(USE_COUNT_FOR_RATING, ratingUseCount);
		prefEditor.putBoolean(APP_RATED, didUserRateApp);
		prefEditor.apply();
	}

	private void getLocation()
	{
		m_conditionsProgressBar.setVisibility(View.VISIBLE);
        if (isNetworkAvailable())
        { readCurrentWeather(); }
	}

    private boolean isNetworkAvailable()
    {
        ConnectivityManager connectivityManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null)
        { return false; }

        NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    private void initialize()
	{
		m_locManager = (LocationManager) getActivity().getSystemService(LOCATION_SERVICE);
		m_textToSpeech = new TextToSpeech(getActivity(), this);
	}

	private void readCurrentWeather()
	{
		String jsonString = "";
		Deserializer jsonDeserializer = new Deserializer();
		//JsonObject jsonObject = jsonDeserializer.deserializeFromJson(jsonString);
		m_speakConditionsFAB.setVisibility(View.VISIBLE);

	}

	@Override
	public void onLocationChanged(Location location)
	{

	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras)
	{

	}

	@Override
	public void onProviderEnabled(String provider)
	{

	}

	@Override
	public void onProviderDisabled(String provider)
	{

	}

	@Override
	public void onInit(int status)
	{
		if (status != TextToSpeech.SUCCESS)
		{
			String errMsg = "Initialization Failed!";
			Toast.makeText(getActivity().getApplicationContext(), errMsg, Toast.LENGTH_LONG).show();
			Log.e("TTS", errMsg);
		}
	}

	@Override
	public void onStop()
	{
		m_textToSpeech.stop();
		super.onStop();
	}

	private void showDialogForAppRating()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle("Please Rate Ride Outlook")
			.setMessage(getString(R.string.rate_app))
			.setIcon(R.drawable.ic_ride_outlook);

		builder.setNegativeButton("NO", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				// Set the APP_RATED preference to TRUE
				didUserRateApp = true;
				saveRatingInfo();
			}
		});

		builder.setPositiveButton("LATER", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				// Reset the counter and prompt again in 10 uses
				ratingUseCount = 0;
				saveRatingInfo();
			}
		});

		builder.setNeutralButton("YES", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				// Set the APP_RATED preference to TRUE and take user to app store
				didUserRateApp = true;
				saveRatingInfo();
				goToPlayStoreToRateApp();
			}
		});
		builder.setCancelable(false);
		builder.show();
	}

	private void goToPlayStoreToRateApp()
	{
        final String appStoreUrl = "https://play.google.com/store/apps/details?id=com.hunterdev.rideoutlook2";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(appStoreUrl));
		startActivity(intent);
	}
}
