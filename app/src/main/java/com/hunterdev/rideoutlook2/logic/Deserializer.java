package com.hunterdev.rideoutlook2.logic;

import com.google.gson.Gson;

public class Deserializer
{
	// Deserialize to single object.
	public Deserializer deserializeFromJson(String jsonString)
	{
		Gson gson = new Gson();
		Deserializer deserializer = gson.fromJson(jsonString, Deserializer.class);
		return deserializer;
	}
}

