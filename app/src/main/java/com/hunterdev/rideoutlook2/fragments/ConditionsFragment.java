package com.hunterdev.rideoutlook2.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.hunterdev.rideoutlook2.R;
import com.hunterdev.rideoutlook2.activities.MainActivity;
import com.hunterdev.rideoutlook2.activities.PermissionsActivity;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.content.Context.LOCATION_SERVICE;

public class ConditionsFragment extends Fragment implements LocationListener, TextToSpeech.OnInitListener
{
	private final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 99;
	private final String TAG = "ConditionsFragment";

	private String m_location;
	private boolean m_weatherQueried;
	private boolean m_iconRetrieved;
	private ImageView m_imageView;
	private TextView m_textView;

	public static ConditionsFragment newInstance()
	{
		return new ConditionsFragment();
	}

	@BindView(R.id.conditions_progress_bar)
	ProgressBar m_conditionsProgressBar;

	private LocationManager m_locManager;
	private TextToSpeech m_textToSpeech;
	private FloatingActionButton m_speakConditionsFAB;

	private final String JSON_EXTENSION = ".json";
	private final String API_REFERENCE_CODE = "e2b6eaf41ee74858";
	private final String UNIT_OF_MEASUREMENT = "unit_of_measurement";
	private final String APP_RATED = "app_rated";
	private final String USE_COUNT_FOR_RATING = "use_count";
	private final String PREF_NAME = "com.hunterdev.rideoutlook2";
	private final int NUM_USES_BETWEEN_RATING_REQUEST = 3;

	private final String weatherConditionsUrl = "http://api.wunderground.com/api/" + API_REFERENCE_CODE + "/conditions/q/";
	private final String weatherIconUrl = "http://icons.wxug.com/i/c/c/";
	private final String wundergroundReferralUrl = "http://www.wunderground.com/?apiref=4e313604ca87365f";

	private String m_unitOfMeasurement;
	private Boolean m_didUserRateApp;
	private int m_ratingUseCount;

	// Create JSON variables for return items
	private String ride_outlook;
	private String city;
	private String state;
	private String weather;
	private String temperature_F;
	private String temperature_C;
	private String temperature;
	private String humidity;
	private String wind_dir;
	private Double wind_mph = 0.0;
	private Double wind_gust_mph = 0.0;
	private Double wind_kph = 0.0;
	private Double wind_gust_kph = 0.0;
	private String wind;
	private String weather_to_speak;
	private String icon;
	private String icon_with_prefix;
	private Bitmap weatherBitmap;


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
				m_textToSpeech.stop();
			}
		};

		m_speakConditionsFAB = rootView.findViewById(R.id.myFAB);
		m_speakConditionsFAB.setVisibility(View.VISIBLE);
		m_speakConditionsFAB.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				m_textToSpeech.speak("Testing... 1, 2, 3, 4, 5, 6, 7, 8, 9, 10", TextToSpeech.QUEUE_FLUSH, null);
				Snackbar
					.make(coordinatorLayoutView, R.string.snackbar_text, Snackbar.LENGTH_LONG)
					.setAction(R.string.snackbar_action_undo, clickListener)
					.show();
			}
		});

		TextView weatherUndergroundUrlLink = rootView.findViewById(R.id.weatherunderground_url_link);
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

		m_imageView = rootView.findViewById(R.id.icon_image);
		m_textView = rootView.findViewById(R.id.current_conditions);

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
		m_unitOfMeasurement = prefs.getString(UNIT_OF_MEASUREMENT, "F");

		// Check to see if the user has rated app or chose not to
		m_didUserRateApp = prefs.getBoolean(APP_RATED, false);

		// Count number of times user launched the app ??? For rating app.
		if (!m_didUserRateApp)
		{
			m_ratingUseCount = prefs.getInt(USE_COUNT_FOR_RATING, 0);
		}
	}

	private void promptUserToRateApp()
	{
		if (!m_didUserRateApp)
		{
			if (NUM_USES_BETWEEN_RATING_REQUEST <= m_ratingUseCount)
			{
				showDialogForAppRating();
			}

			m_ratingUseCount++;
			saveRatingInfo();
		}
	}

	private void saveRatingInfo()
	{
		SharedPreferences prefs = getActivity().getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor prefEditor = prefs.edit();
		prefEditor.putInt(USE_COUNT_FOR_RATING, m_ratingUseCount);
		prefEditor.putBoolean(APP_RATED, m_didUserRateApp);
		prefEditor.apply();
	}

	private void getLocation()
	{
		m_conditionsProgressBar.setVisibility(View.VISIBLE);
		if (isNetworkAvailable())
		{
			List<String> locationProviders = m_locManager.getAllProviders();
			for (String provider : locationProviders)
			{
				if (provider.compareToIgnoreCase("network") == 0)
				{
					if (ActivityCompat.checkSelfPermission(this.getContext(),
						Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
					{
						// Should we show an explanation?
						if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
							Manifest.permission.ACCESS_FINE_LOCATION))
						{
							// Show an explanation to the user *asynchronously* -- don't block
							// this thread waiting for the user's response! After the user
							// sees the explanation, try again to request the permission.
							Intent permissionsExplanation = new Intent(getContext(), PermissionsActivity.class);
							startActivity(permissionsExplanation);
						}
						else
						{
							// No explanation needed, we can request the permission.
							ActivityCompat.requestPermissions(getActivity(),
								new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
								MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
						}
					}
					else
					{
						m_locManager.requestLocationUpdates(provider, 0, 0, this);
					}
				}
			}
			//readCurrentWeather();
		}
		else
		{
			getActivity().runOnUiThread(showNetworkError);
		}
	}

	Runnable showNetworkError = new Runnable()
	{
		@Override
		public void run()
		{
			MainActivity.showAlertDialog(getString(R.string.network_error_title), getString(R.string.network_error_text), getContext());
		}
	};

	private boolean isNetworkAvailable()
	{
		ConnectivityManager connectivityManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivityManager == null)
		{
			return false;
		}

		NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
		return netInfo != null && netInfo.isConnectedOrConnecting();
	}

	private void initialize()
	{
		m_locManager = (LocationManager) getActivity().getSystemService(LOCATION_SERVICE);
		m_textToSpeech = new TextToSpeech(getActivity(), this);
	}

	@Override
	public void onDetach()
	{
		super.onDetach();
		m_textToSpeech.shutdown();
	}

	private void readCurrentWeather()
	{
		//String jsonString;
		//Deserializer jsonDeserializer = new Deserializer();
		//JsonObject jsonObject = jsonDeserializer.deserializeFromJson(jsonString);

	}

	@Override
	public void onLocationChanged(Location location)
	{
//	    TODO: DISABLE THE WEATHER WHILE DEBUGGING...
//		Toast.makeText(getApplicationContext(), "Need to re-enable location...", Toast.LENGTH_SHORT).show();
//		m_weatherQueried = true;

//		m_location = "32807"; // --> Orlando, FL
//		m_location = "92121"; // --> San Diego, CA
//		m_location = "59.95,30.30"; // --> St. Petersburg, Russia
//		m_location = "27.77,-82.64"; // --> St. Petersburg, FL
		m_location = location.getLatitude() + "," + location.getLongitude();
		if (!m_weatherQueried)
		{
			accessInternetForWeatherConditions();

			int sleepTime = 500;
			while (!m_iconRetrieved && sleepTime < 5000)
			{
				try
				{
					Thread.sleep(sleepTime);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
				sleepTime += 500;
			}
			if (m_iconRetrieved)
			{
				accessInternetForWeatherIcon();
			} else
			{
				icon = "clear";
			}
		}
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
				m_didUserRateApp = true;
				saveRatingInfo();
			}
		});

		builder.setPositiveButton("LATER", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				// Reset the counter and prompt again in 10 uses
				m_ratingUseCount = 0;
				saveRatingInfo();
			}
		});

		builder.setNeutralButton("YES", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				// Set the APP_RATED preference to TRUE and take user to app store
				m_didUserRateApp = true;
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

	private void accessInternetForWeatherConditions()
	{
		m_weatherQueried = true;

		// Access the Internet for the current weather conditions
		Thread weatherThread = new Thread(internetThreadForWeatherConditions);
		weatherThread.start();

		try
		{
			Thread.sleep(1500);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
			Log.e("Sleep", "Error sleeping for 1500 milliseconds!");
		}
	}

	private void accessInternetForWeatherIcon()
	{
		// Access the Internet for current weather icon
		Thread weatherIconThread = new Thread(internetThreadForWeatherIcon);
		weatherIconThread.start();
	}

	Runnable internetThreadForWeatherConditions = new Runnable()
	{
		@Override
		public void run()
		{
			String httpReturn = "";

			// Try connecting to a weather underground web site
			try
			{
				// Create a new client object
				HttpClient httpClient = new DefaultHttpClient();

				String conditionsUrl = weatherConditionsUrl + m_location + JSON_EXTENSION;

				// Post the Url for weather conditions
				HttpPost conditionsHttpPost = new HttpPost(conditionsUrl);

				// Execute the post and get the response object
				HttpResponse httpResponse = httpClient.execute(conditionsHttpPost);

				// Get the message from the response
				HttpEntity httpEntity = httpResponse.getEntity();

				// Get the content of the message
				InputStream inputStream = httpEntity.getContent();

				// Convert response to string
				try
				{
					BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "iso-8859-1"), 8);
					StringBuilder strBuilder = new StringBuilder();
					String currentLine = null;
					while ((currentLine = reader.readLine()) != null)
					{
						strBuilder.append(currentLine + "\n");
					}

					// Close the input stream
					inputStream.close();

					// Convert our string builder object to a string
					httpReturn = strBuilder.toString();
				}
				catch (Exception e)
				{
					Log.e("InputStreamReder", "Error reading input stream:" + e.getLocalizedMessage());
				}
			}
			catch (Exception e)
			{
				Log.e("HTTP ERROR", "Error in http connection:" + e.toString());
			}

			// Now, lets parse the JSON data
			try
			{
				// Pars the JSON object
				JSONObject jObj = new JSONObject(httpReturn);
				String current_observation = jObj.getString("current_observation");

				jObj = new JSONObject(current_observation);
				weather = jObj.getString("weather");
				temperature_F = jObj.getInt("temp_f") + "";
				temperature_C = jObj.getInt("temp_c") + "";

				if (m_unitOfMeasurement.equals("F"))
				{
					temperature = temperature_F;
				} else
				{
					temperature = temperature_C;
				}

				// Use this line if compiling on a PC Alt+ 248
				//temperature += "";

				// Use this line if compiling on a MAC option shift 8
				temperature += "°";

				humidity = jObj.getString("relative_humidity");
				wind_dir = jObj.getString("wind_dir");

				// Wind speed in US measurements
				wind_mph = jObj.getDouble("wind_mph");
				wind_gust_mph = jObj.getDouble("wind_gust_mph");

				// Wind speed in metric measurements
				wind_kph = jObj.getDouble("wind_kph");
				wind_gust_kph = jObj.getDouble("wind_gust_kph");

				wind = "From the " + wind_dir;
				if (m_unitOfMeasurement.equals("F"))
				{
					if (wind_mph.intValue() == 0)
					{
						wind = "Calm";
					} else
					{
						wind += " at " + wind_mph + " MPH";
						if (wind_gust_mph.intValue() != 0)
						{
							wind += " Gusting to " + wind_gust_mph + " MPH";
						}
					}
				} else
				{
					if (wind_kph.intValue() == 0)
					{
						wind = "Calm";
					} else
					{
						wind += " at " + wind_kph + " KPH";
						if (wind_gust_kph.intValue() != 0)
						{
							wind += " Gusting to " + wind_gust_kph + " KPH";
						}
					}
				}

				//wind = jObj.getString("wind_string");
				icon = jObj.getString("icon");
				icon_with_prefix = getIconPrefix() + icon;

				if (icon != null || icon != "")
				{
					m_iconRetrieved = true;
				}

				String display_location = jObj.getString("display_location");
				jObj = new JSONObject(display_location);
				city = jObj.getString("city");
				state = jObj.getString("state");

				String state_string = getStateString();

				ride_outlook = city + state_string + "\n\n";

				ride_outlook += temperature + " and " + weather + "\n" +
					humidity + " Humidity\n\n" +
					"Wind: " + wind;

				weather_to_speak = "In " + city + " " + getStateName(state) +
					", it's " + temperature + " and " + weather +
					" with " + humidity + " humidity " +
					", and the wind is " + extractWindDirectionFromString();

				Log.d("JSON Object", "Ride Outlook = " + ride_outlook);
				Log.d("JSON Object", "Conditions icon = " + icon_with_prefix);
			}
			catch (Exception e)
			{
				Log.e("JSON Parsing", "Error parsing the JSON data: " + e.toString());
			}
		}

		private String extractWindDirectionFromString()
		{
			String newWind = "";
			String returnStr = "";
			int index = 0;

			// Parse the wind direction from the string and spell it out
			// indexOf return -1 if String does not contain specified word

			// Each specific direction
			index = wind.indexOf("East");
			if (index != -1)
			{
				return extractWindSpeedFromString();
			}

			index = wind.indexOf("West");
			if (index != -1)
			{
				return extractWindSpeedFromString();
			}

			index = wind.indexOf("North");
			if (index != -1)
			{
				return extractWindSpeedFromString();
			}

			index = wind.indexOf("South");
			if (index != -1)
			{
				return extractWindSpeedFromString();
			}

			// Variations of each direction...

			// WNW, NNW, NW
			index = wind.indexOf("WNW");
			if (index != -1)
			{
				for (String retval : wind.split("WNW", 3))
				{
					newWind = new String(wind.substring(index + 3));
					returnStr = retval + "west north west" + newWind;
					Log.d("Parsed WNW", returnStr);
					wind = returnStr;
					return extractWindSpeedFromString();
				}
			}

			index = wind.indexOf("NW");
			if (index != -1)
			{
				for (String retval : wind.split("NW", 2))
				{
					newWind = new String(wind.substring(index + 2));
					returnStr = retval + "north west" + newWind;
					Log.d("Parsed NW", returnStr);
					wind = returnStr;
					return extractWindSpeedFromString();
				}
			}

			index = wind.indexOf("NNW");
			if (index != -1)
			{
				for (String retval : wind.split("NNW", 3))
				{
					newWind = new String(wind.substring(index + 3));
					returnStr = retval + "north north west" + newWind;
					Log.d("Parsed NNW", returnStr);
					wind = returnStr;
					return extractWindSpeedFromString();
				}
			}

			// WSW, SSW, SW
			index = wind.indexOf("WSW");
			if (index != -1)
			{
				for (String retval : wind.split("WSW", 3))
				{
					newWind = new String(wind.substring(index + 3));
					returnStr = retval + "west south west" + newWind;
					Log.d("Parsed WSW", returnStr);
					wind = returnStr;
					return extractWindSpeedFromString();
				}
			}

			index = wind.indexOf("SSW");
			if (index != -1)
			{
				for (String retval : wind.split("SSW", 3))
				{
					newWind = new String(wind.substring(index + 3));
					returnStr = retval + "south south west" + newWind;
					Log.d("Parsed SSW", returnStr);
					wind = returnStr;
					return extractWindSpeedFromString();
				}
			}

			index = wind.indexOf("SW");
			if (index != -1)
			{
				for (String retval : wind.split("SW", 2))
				{
					newWind = new String(wind.substring(index + 2));
					returnStr = retval + "south west" + newWind;
					Log.d("Parsed SW", returnStr);
					wind = returnStr;
					return extractWindSpeedFromString();
				}
			}

			// ESE, SSE, SE
			index = wind.indexOf("ESE");
			if (index != -1)
			{
				for (String retval : wind.split("ESE", 3))
				{
					newWind = new String(wind.substring(index + 3));
					returnStr = retval + "east south east" + newWind;
					Log.d("Parsed ESE", returnStr);
					wind = returnStr;
					return extractWindSpeedFromString();
				}
			}

			index = wind.indexOf("SSE");
			if (index != -1)
			{
				for (String retval : wind.split("SSE", 3))
				{
					newWind = new String(wind.substring(index + 3));
					returnStr = retval + "south south east" + newWind;
					Log.d("Parsed SSE", returnStr);
					wind = returnStr;
					return extractWindSpeedFromString();
				}
			}

			index = wind.indexOf("SE");
			if (index != -1)
			{
				for (String retval : wind.split("SE", 2))
				{
					newWind = new String(wind.substring(index + 2));
					returnStr = retval + "south east" + newWind;
					Log.d("Parsed SE", returnStr);
					wind = returnStr;
					return extractWindSpeedFromString();
				}
			}

			// ENE, NNE, NE
			index = wind.indexOf("ENE");
			if (index != -1)
			{
				for (String retval : wind.split("ENE", 3))
				{
					newWind = new String(wind.substring(index + 3));
					returnStr = retval + "east north east" + newWind;
					Log.d("Parsed ENE", returnStr);
					wind = returnStr;
					return extractWindSpeedFromString();
				}
			}

			index = wind.indexOf("NNE");
			if (index != -1)
			{
				for (String retval : wind.split("NNE", 3))
				{
					newWind = new String(wind.substring(index + 3));
					returnStr = retval + "north north east" + newWind;
					Log.d("Parsed NNE", returnStr);
					wind = returnStr;
					return extractWindSpeedFromString();
				}
			}

			index = wind.indexOf("NE");
			if (index != -1)
			{
				for (String retval : wind.split("NE", 2))
				{
					newWind = new String(wind.substring(index + 2));
					returnStr = retval + "north east" + newWind;
					Log.d("Parsed NE", returnStr);
					wind = returnStr;
					return extractWindSpeedFromString();
				}
			}

			returnStr = extractWindSpeedFromString();
			return returnStr;
		}

		private String extractWindSpeedFromString()
		{
			String newWindSpeed;
			String returnStr;
			String returnStr2;
			int index;

			// Parse the wind speed (MPH/KPH) from the string and spell it out
			//indexOf return -1 if String does not contain specified word
			String windSpeedMeasurement = "";
			String distanceMeasurement = "";
			if (m_unitOfMeasurement.equals("F"))
			{
				windSpeedMeasurement = "MPH";
				distanceMeasurement = "miles";
			} else
			{
				windSpeedMeasurement = "KPH";
				distanceMeasurement = "kilometers";
			}

			index = wind.indexOf(windSpeedMeasurement);
			if (index != -1)
			{
				for (String retval : wind.split(windSpeedMeasurement, 3))
				{
					newWindSpeed = new String(wind.substring(index + 3));
					returnStr = retval + distanceMeasurement + " per hour" + newWindSpeed;
					Log.d("Parsed WindSpeed", returnStr);

					index = returnStr.indexOf(windSpeedMeasurement);
					if (index != -1)
					{
						for (String retval2 : returnStr.split(windSpeedMeasurement, 3))
						{
							newWindSpeed = new String(returnStr.substring(index + 3));
							returnStr2 = retval2 + distanceMeasurement + " per hour" + newWindSpeed;
							Log.d("Parsed WindSpeed2", returnStr2);
							return returnStr2;
						}
					} else
					{
						return returnStr;
					}
				}
			} else
			{
				Log.d("Parsing " + windSpeedMeasurement, windSpeedMeasurement + " not found in: " + wind);
			}
			return wind;
		}

		private String getIconPrefix()
		{
			Calendar rightNow = Calendar.getInstance();
			int currentHour = rightNow.get(Calendar.HOUR_OF_DAY);

			if (currentHour > 5 && currentHour < 19)
			{
				return "";
			} else
			{
				return "nt_";
			}
		}

		private String getStateString()
		{
			if (state.isEmpty())
				return "";
			else
				return ", " + state;
		}

		private String getStateName(String state)
		{
			String fullStateName;

			switch (state)
			{
				case "CA":
					fullStateName = "California";
					break;
				case "NV":
					fullStateName = "Nevada";
					break;
				case "FL":
					fullStateName = "Florida";
					break;
				case "AZ":
					fullStateName = "Arizona";
					break;
				case "NM":
					fullStateName = "New Mexico";
					break;
				case "NY":
					fullStateName = "New York";
					break;
				case "TN":
					fullStateName = "Tennessee";
					break;
				case "GA":
					fullStateName = "Georgia";
					break;
				case "TX":
					fullStateName = "Texas";
					break;
				default:
					fullStateName = state;
					break;
			}
			return fullStateName;
		}
	};

	Runnable internetThreadForWeatherIcon = new Runnable()
	{
		@Override
		public void run()
		{
			String completeIconUrl;
			try
			{
				completeIconUrl = weatherIconUrl + icon_with_prefix + ".gif";
				Log.d(TAG, completeIconUrl);

				URL url = new URL(completeIconUrl);
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.setDoInput(true);
				connection.connect();
				InputStream input = connection.getInputStream();
				Bitmap myBitmap = BitmapFactory.decodeStream(input);
				Log.d("Bitmap", "Got the bitmap image");
				weatherBitmap = myBitmap;
			}
			catch (IOException e)
			{
				e.printStackTrace();
				Log.e("Exception", e.getMessage());
				weatherBitmap = null;
			}
			m_imageView.post(updateGUI);
		}
	};

	Runnable updateGUI = new Runnable()
	{
		@Override
		public void run()
		{
			m_textView.setText(ride_outlook);
			m_imageView.setImageBitmap(weatherBitmap);
			m_conditionsProgressBar.setVisibility(View.GONE);
		}
	};
}
