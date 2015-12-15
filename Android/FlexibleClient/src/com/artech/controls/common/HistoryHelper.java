package com.artech.controls.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.SharedPreferences;

import com.artech.application.MyApplication;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.services.Services;
import com.artech.base.utils.ListUtils;

public class HistoryHelper
{
	private static final String PREFERENCES_KEY = "InputHistory";
	private static final char LIST_SEPARATOR = ',';

	private final String mKey;
	private List<String> mValues;

	public HistoryHelper(LayoutItemDefinition definition)
	{
		mKey = definition.getLayout().getParent().getName() + "::" + definition.getName();
	}

	private void loadValues()
	{
		if (mValues == null)
		{
			SharedPreferences historyStore = MyApplication.getAppSharedPreferences(PREFERENCES_KEY);
			String historyValues = historyStore.getString(mKey, null);
			if (Services.Strings.hasValue(historyValues))
				mValues = Services.Strings.decodeStringList(historyValues, LIST_SEPARATOR);
			else
				mValues = new ArrayList<String>();
		}
	}

	public List<String> getValues()
	{
		loadValues();

		// For the user, return sorted values. The internal list is kept in insertion order.
		ArrayList<String> sortedValues = new ArrayList<String>(mValues);
		Collections.sort(sortedValues, String.CASE_INSENSITIVE_ORDER);
		return sortedValues;
	}

	public void store(String value)
	{
		if (!Services.Strings.hasValue(value))
			return;

		loadValues();

		// If present, remove it (so that it's "moved to end" when re-added).
		int prevIndex = ListUtils.indexOf(mValues, value, String.CASE_INSENSITIVE_ORDER);
		if (prevIndex != -1)
			mValues.remove(prevIndex);

		mValues.add(value);
		String historyValues = Services.Strings.encodeStringList(mValues, LIST_SEPARATOR);

		SharedPreferences historyStore = MyApplication.getAppSharedPreferences(PREFERENCES_KEY);
		historyStore.edit().putString(mKey, historyValues).commit();
	}

	public static void clearAll()
	{
		SharedPreferences historyStore = MyApplication.getAppSharedPreferences(PREFERENCES_KEY);
		historyStore.edit().clear().commit();
	}
}
