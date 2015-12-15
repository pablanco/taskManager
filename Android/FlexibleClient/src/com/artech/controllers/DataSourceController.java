package com.artech.controllers;

import java.util.UUID;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import com.artech.activities.IntentParameters;
import com.artech.application.MyApplication;
import com.artech.base.metadata.DynamicCallDefinition;
import com.artech.base.metadata.IDataSourceDefinition;
import com.artech.base.services.Services;
import com.artech.common.DataRequest;
import com.artech.common.IntentHelper;
import com.artech.compatibility.SherlockHelper;
import com.artech.providers.EntityDataProvider;
import com.artech.services.EntityServiceResponse;

public class DataSourceController implements IDataSourceControllerInternal
{
	private final DataViewController mParent;
	private final DataSourceModel mModel;
	private final int mId;

	private IDataSourceBoundView mView;
	private EntityReceiver mReceiver;

	// Status
	private ViewData mLastResponse;
	private ViewData mDelayedCacheResponse;
	private boolean mRequestPending;
	private boolean mJustAttached;

	public DataSourceController(DataViewController parent, DataSourceModel model)
	{
		mParent = parent;
		mModel = model;
		mId = createDataSourceId();
	}

	private static int NEXT_DATA_SOURCE_ID = 1;
	static synchronized int createDataSourceId()
	{
		return NEXT_DATA_SOURCE_ID++;
	}

	private Context getContext()
	{
		return mParent.getContext();
	}

	@Override
	public IDataSourceDefinition getDefinition()
	{
		return mModel.getDefinition();
	}

	@Override
	public int getId()
	{
		return mId;
	}

	@Override
	public String getName()
	{
		return mModel.getDefinition().getName();
	}

	@Override
	public IDataViewController getParent()
	{
		return mParent;
	}

	@Override
	public DataSourceModel getModel()
	{
		return mModel;
	}

	@Override
	public void attach(IDataSourceBoundView view)
	{
		mView = view;
		mJustAttached = true;
	}

	@Override
	public void detach()
	{
		mView = null;
	}

	// -----------------------------------------------------
	// Fragment data management methods.

	@Override
	public void onResume()
	{
		if (mReceiver == null)
		{
			// Prepare receiver for data communication.
			mReceiver = new EntityReceiver();
			IntentFilter filter = new IntentFilter(mReceiver.ID);
			LocalBroadcastManager.getInstance(getContext()).registerReceiver(mReceiver, filter);

			if (mView != null && mView.needsMoreData())
			{
				// If this controller is being "resurrected", then the old data may suffice.
				// Otherwise start a new task to load the data.
				if (mJustAttached && mLastResponse != null)
				{
					postResponse(mLastResponse);
					followUpAfterResponse(mLastResponse);
				}
				else
					startLoading(DataRequest.REQUEST_FIRST, DataRequest.COUNT_DEFAULT);
			}
		}

		mJustAttached = false;
	}

	/**
	 * Calls (via Intent) the EntityService to get data.
	 * @param requestType Used to indicate if the request comes at activity start, when scrolling, or via refresh.
	 */
	private synchronized void startLoading(int requestType, int requestCount)
	{
		// Start service to load data in background.
		// Copy receive to local variable because we could be interrupted by another thread.
		EntityReceiver receiver = mReceiver;
		if (receiver != null)
		{
			Intent msgService = new Intent(getContext(), MyApplication.getInstance().getEntityServiceClass());
			msgService.putExtra(IntentParameters.Service.DataViewSession, mParent.getSessionId());
			IntentHelper.putObject(msgService, IntentParameters.Service.DataProvider, EntityDataProvider.class, mModel.getProvider());
			msgService.putExtra(IntentParameters.Service.IntentFilter, receiver.ID);
			msgService.putExtra(IntentParameters.Service.RequestType, requestType);
			msgService.putExtra(IntentParameters.Service.RequestCount, requestCount);

			mRequestPending = true;
			getContext().startService(msgService);
		}
	}

	@Override
	public void onRequestMoreData()
	{
		// Controls may send more requests than necessary (for example, scrolling seems to be fired
		// when loading data). Ignore these so that the UC doesn't have so much work to do.
		if (mRequestPending)
			return;

		if (mView != null && mView.needsMoreData())
			startLoading(DataRequest.REQUEST_MORE, DataRequest.COUNT_DEFAULT);
	}

	@Override
	public void onRefresh(boolean keepPosition)
	{
		mParent.updateDataSourceParameters(this);
		
		int count = DataRequest.COUNT_DEFAULT;
		if (keepPosition && mLastResponse != null)
		{
			if (mLastResponse.isMoreAvailable())
			{
				// If we had more than one page of data, ask for as many pages in this request.
				// That way the list won't "scroll upwards".
				if (mLastResponse.getCount() > mModel.getProvider().getRowsPerPage())
					count = mLastResponse.getCount();
			}
			else
				count = DataRequest.COUNT_ALL;
		}

		startLoading(DataRequest.REQUEST_REFRESH, count);
	}

	// Inner Class For Receiving Intents
	public class EntityReceiver extends BroadcastReceiver
	{
		public final String ID = UUID.randomUUID().toString();

		@Override
		public void onReceive(Context context, Intent intent)
		{
			final EntityServiceResponse response = EntityServiceResponse.get(intent);
			if (response == null)
				return;

			// Server response means request is completely done (could be an error or "up to date", doesn't matter).
			if (response.getSource() == DataRequest.RESULT_SOURCE_SERVER)
				mRequestPending = false;

			ViewData viewData = null;

			if (response.getSource() == DataRequest.RESULT_SOURCE_SERVER && mDelayedCacheResponse != null)
			{
				// We delayed the cache response to wait for the server (probably because
				// there was a cached dynamic call) so use it if the server didn't respond.
				if (response.getStatusCode() == DataRequest.ERROR_NETWORK)
					viewData = mDelayedCacheResponse;

				mDelayedCacheResponse = null;
			}

			// If this response is the same as the one last passed to the UI, don't update the view.
			if (mLastResponse == null || !mLastResponse.getResponseId().equals(response.getResponseId()))
			{
				if (viewData == null)
				{
					viewData = new ViewData(
						mModel.getUri(),
						response.getResponseId(),
						response.getSource(),
						mModel.getProvider().getEntities(), // copy to new collection (TODO: in background thread).
						response.hasMoreData(),
						response.getStatusCode(),
						response.getStatusMessage(),
						false);
				}

				// Don't post a response with dynamic calls from cache, wait for the real response from server.
				if (response.getSource() == DataRequest.RESULT_SOURCE_LOCAL &&
					viewData.getSingleEntity() != null &&
					DynamicCallDefinition.from(viewData.getSingleEntity()).size() != 0)
				{
					mDelayedCacheResponse = viewData;
					return;
				}

				postResponse(viewData);
			}
			else
			{
				// raise "on refresh not change" notification
				if (viewData == null)
				{
					viewData = new ViewData(
							mModel.getUri(),
							response.getResponseId(),
							response.getSource(),
							null, // data not needed.
							response.hasMoreData(),
							response.getStatusCode(),
							response.getStatusMessage(),
							true);
				}

				postResponse(viewData);
			}

			// In case the view *still* needs more data after updating...
			followUpAfterResponse(mLastResponse);
		}
	}

	protected void postResponse(ViewData viewData)
	{
		if (!viewData.getDataUnchanged())
		{
			// Notify the parent controller before updating UI.
			mParent.onReceive(this, viewData);
			mLastResponse = viewData;
		}

		Services.Log.debug(String.format("Updating UI: %s (%s).", getName(), EntityServiceResponse.getSourceName(viewData.getResponseSource()))); //$NON-NLS-1$

		// Update the corresponding UI control.
		if (mView != null)
			mView.update(viewData);

		SherlockHelper.invalidateOptionsMenu(mParent.getParent().getActivity());
	}

	private void followUpAfterResponse(final ViewData response)
	{
		if (mView != null &&
			mView.needsMoreData() &&
			response.isMoreAvailable() &&
			response.getResponseSource() == DataRequest.RESULT_SOURCE_SERVER &&
			response.getStatusCode() == DataRequest.ERROR_NONE)
		{
			// ... launch a follow-up request.
			startLoading(DataRequest.REQUEST_MORE, DataRequest.COUNT_DEFAULT);
		}
	}

	@Override
	public void onPause()
	{
		if (mReceiver != null)
		{
			LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mReceiver);
			mReceiver = null;
		}
	}

	@Override
	public IDataSourceBoundView getBoundView()
	{
		return mView;
	}
}
