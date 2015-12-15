package com.artech.services;

import java.io.Serializable;

import android.content.Intent;

import com.artech.common.DataRequest;
import com.artech.providers.ProviderDataResult;

public class EntityServiceResponse implements Serializable
{
	private static final long serialVersionUID = 1L;

	private static final String INTENT_KEY = "EntityServiceResponse";

	private final String mResponseId;
	private final int mSource;
	private final int mStatusCode;
	private final String mStatusMessage;
	private final boolean mIsUpToDate;
	private final boolean mHasMoreData;

	EntityServiceResponse(String responseId, ProviderDataResult data)
	{
		mResponseId = responseId;
		mSource = data.getSource();
		mStatusCode = data.getStatusCode();
		mStatusMessage = data.getStatusMessage();
		mIsUpToDate = data.isUpToDate();
		mHasMoreData = data.isMoreDataAvailable();
	}

	public static void put(Intent intent, EntityServiceResponse response)
	{
		intent.putExtra(INTENT_KEY, response);
	}

	public static EntityServiceResponse get(Intent intent)
	{
		return (EntityServiceResponse)intent.getSerializableExtra(INTENT_KEY);
	}

	/**
	 * Response id. Used to distinguish when the request results in new data or not.
	 * If the data is the same as was cached, refreshing the UI is not necessary.
	 */
	public String getResponseId() { return mResponseId; }

	/**
	 * Result source. Can be DataRequest.RESULT_SOURCE_LOCAL, SERVER, or NONE.
	 */
	public int getSource() { return mSource; }

	public static String getSourceName(int source)
	{
		switch (source)
		{
			case DataRequest.RESULT_SOURCE_LOCAL :
				return "Local"; //$NON-NLS-1$
			case DataRequest.RESULT_SOURCE_SERVER :
				return "Server"; //$NON-NLS-1$
			case DataRequest.RESULT_SOURCE_NONE :
				return "None"; //$NON-NLS-1$
			default :
				return "Unknown"; //$NON-NLS-1$
		}
	}

	/**
	 * Response status code (normal or error).
	 */
	public int getStatusCode() { return mStatusCode; }

	/**
	 * Response status message (only if status code = error).
	 */
	public String getStatusMessage() { return mStatusMessage; }

	/**
	 * When true, signals that the server responded "up to date".
	 */
	public boolean isUpToDate() { return mIsUpToDate; }

	/**
	 * When true, signals that the server has (or at least may have) more data
	 * than what was returned by the current query.
	 */
	public boolean hasMoreData() { return mHasMoreData; }
}
