package com.artech.services;

import android.os.AsyncTask;

import com.artech.common.DataRequest;
import com.artech.providers.EntityDataProvider;
import com.artech.providers.ProviderDataResult;

class LoadDataTask extends AsyncTask<Void, EntityServiceResponse, Void>
{
	private final EntityService mService;
	private final int mSessionId;
	private final EntityDataProvider mProvider;
	private final String mIntentFilter;

	private final int mRequestType;
	private final int mRequestCount;

	LoadDataTask(EntityService service, int sessionId, EntityDataProvider provider, String intentFilter, int requestType, int requestCount)
	{
		mService = service;
		mSessionId = sessionId;
		mProvider = provider;
		mIntentFilter = intentFilter;

		mRequestType = requestType;
		mRequestCount = requestCount;
	}

	@Override
	public String toString()
	{
		return String.format("<Type=%s URI='%s'>", mRequestType, getDataUri()); //$NON-NLS-1$
	}

	public String getDataUri()
	{
		return mProvider.getDataUri().toString();
	}

	@Override
	protected Void doInBackground(Void... arg0)
	{
		// Call data provider (local or remote) and publish result.
		ProviderDataResult providerData = mProvider.getData(mSessionId, mRequestType, mRequestCount);

		if (providerData.getSource() != DataRequest.RESULT_SOURCE_NONE)
			publishProgress(new EntityServiceResponse(providerData.getVersion(), providerData));

		return null;
	}

	@Override
	protected void onProgressUpdate(EntityServiceResponse... values)
	{
		super.onProgressUpdate(values);

		if (values == null || values.length != 1)
			throw new IllegalArgumentException("LoadDataTask.onProgressUpdate -- No data!"); //$NON-NLS-1$

		EntityServiceResponse response = values[0];
		mService.announceEntityData(this, mIntentFilter, response);
	}

	@Override
	protected void onCancelled()
	{
		// Doc: If you write your own implementation, do not call super.onCancelled(result)
		mService.afterFinish(this);
	}

	@Override
	protected void onPostExecute(Void result)
	{
		super.onPostExecute(result);
		mService.afterFinish(this);
	}
}
