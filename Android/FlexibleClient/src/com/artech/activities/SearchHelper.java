package com.artech.activities;

import android.app.SearchManager;
import android.content.Intent;
import android.util.Pair;

import com.artech.base.metadata.IDataSourceDefinition;
import com.artech.base.services.Services;
import com.artech.controllers.IDataSourceController;

/**
 * Internal class to manage SearchStubActivity and its callers.
 * @author matiash
 *
 */
public class SearchHelper
{
	private static IDataSourceDefinition sCurrentSearchDefinition;
	private static int sSearchedDataSource;
	private static String sSearchText;

	static void prepare(IDataSourceController dataSource)
	{
		// Store search definition for suggestion provider
		sCurrentSearchDefinition = dataSource.getDefinition();
		sSearchedDataSource = dataSource.getId();
	}

	static void onSearch(Intent intent)
	{
		// Get search string.
		sSearchText = intent.getStringExtra(SearchManager.QUERY);
	}

	public static IDataSourceDefinition getCurrentSearchDefinition()
	{
		return sCurrentSearchDefinition;
	}

	/**
	 * Returns and <b>clears</b> the last performed search.
	 */
	static Pair<IDataSourceController, String> getCurrentSearch(ActivityController activityController)
	{
		Pair<IDataSourceController, String> value = null;
		if (sSearchedDataSource != 0 && Services.Strings.hasValue(sSearchText))
		{
			IDataSourceController dataSource = activityController.getDataSource(sSearchedDataSource);
			if (dataSource != null)
				value = new Pair<IDataSourceController, String>(dataSource, sSearchText);
		}

		sSearchText = null;
		return value;
	}
}
