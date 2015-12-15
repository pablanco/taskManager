package com.artech.android.api;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.artech.application.MyApplication;
import com.artech.base.utils.Strings;

@SuppressLint("CommitPrefEdits")
public class ClientStorage
{
	private static final String PREFERENCES_KEY = "ClientStorageApi";

	public static void set(String key, String value)
	{
		SharedPreferences session = MyApplication.getAppSharedPreferences(PREFERENCES_KEY);
		Editor sessionEditor = session.edit();
		sessionEditor.putString(key, value);
		sessionEditor.commit();
	}
	
	public static String get(String key)
	{
		SharedPreferences session = MyApplication.getAppSharedPreferences(PREFERENCES_KEY);
		return session.getString(key, Strings.EMPTY);
	}
	
	public static void remove(String key)
	{
		SharedPreferences session = MyApplication.getAppSharedPreferences(PREFERENCES_KEY);
		Editor sessionEditor = session.edit();
		sessionEditor.remove(key);
		sessionEditor.commit();
	}
	
	public static void clear()
	{
		SharedPreferences session = MyApplication.getAppSharedPreferences(PREFERENCES_KEY);
		Editor sessionEditor = session.edit();
		sessionEditor.clear();
		sessionEditor.commit();
	}
	
	
	
}
