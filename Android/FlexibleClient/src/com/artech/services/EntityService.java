package com.artech.services;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import com.artech.activities.IntentParameters;
import com.artech.base.services.Services;
import com.artech.common.DataRequest;
import com.artech.common.IntentHelper;
import com.artech.providers.EntityDataProvider;

public abstract class EntityService extends Service
{
	private LoadDataTask mRunningTask = null;
	private Queue<LoadDataTask> mPriorityQueuedTasks = new ConcurrentLinkedQueue<LoadDataTask>();
	private Queue<LoadDataTask> mQueuedTasks = new ConcurrentLinkedQueue<LoadDataTask>();

	private static final Object sLock = new Object();
	private static boolean sIsWorking;

	private static final boolean LOG_ENABLED = false;
	private static final String LOG_TAG = "EntityService"; //$NON-NLS-1$

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		if (intent == null)
			return Service.START_NOT_STICKY;

		int sessionId = intent.getIntExtra(IntentParameters.Service.DataViewSession, 0);
		String intentFilter = intent.getStringExtra(IntentParameters.Service.IntentFilter);
		EntityDataProvider provider = IntentHelper.getObject(intent, IntentParameters.Service.DataProvider, EntityDataProvider.class);
		int requestType = intent.getIntExtra(IntentParameters.Service.RequestType, 0); // This parameter is mandatory; 0 is an error.
		int requestCount = intent.getIntExtra(IntentParameters.Service.RequestCount, DataRequest.COUNT_DEFAULT);

		if (intentFilter == null || requestType == 0)
			return Service.START_NOT_STICKY;

		if (provider==null)
		{
			Services.Log.Error("onStartCommand provider null");
		}
		else 
		{
			//Services.Log.debug("onStartCommand provider: " + provider.getDataUri().toString() );
			startTask(sessionId, provider, intentFilter, requestType, requestCount);
		}
		return Service.START_NOT_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		// TODO Auto-generated method stub.
		return null;
	}

	private void startTask(int sessionId, EntityDataProvider provider, String intentFilter, int requestType, int requestCount)
	{
		// Hack: do NOT queue more than one task for the same DP.
		for (LoadDataTask task : mQueuedTasks)
		{
			if (task.getDataUri().equals(provider.getDataUri().toString()))
			{
				Services.Log.Error("duplicate task in queue?!?");
				return;
			}
		}

		// Cases of requests:
		// 1) FIRST: first return data from cache and then query the server.
		// 2) REFRESH: only query the server (checking for changes).
		// 3) MORE: only query the server (previous data is assumed to have been loaded).
		// 4) CACHE: return data from cache and stop.
		if (requestType == DataRequest.REQUEST_FIRST ||	requestType == DataRequest.REQUEST_CACHED)
		{
			// Ask for local data. Put in priority queue because it should be fast.
			LoadDataTask task = new LoadDataTask(this, sessionId, provider, intentFilter, DataRequest.REQUEST_CACHED, DataRequest.COUNT_ALL);
			enqueueTask(task, true);
		}

		if (requestType != DataRequest.REQUEST_CACHED)
		{
			// Ask for server data. May return "up-to-date" if:
			// a) Asking for data which was already returned by the previous request from cache.
			// b) Asking for REQUEST_MORE and the data is already fully cached.
			LoadDataTask task = new LoadDataTask(this, sessionId, provider, intentFilter, requestType, requestCount);
			enqueueTask(task, false);
		}

		runNextTask();
	}

	/**
	 * Adds a task to the queue, possibly reordering it to minimize user latency.
	 */
	private void enqueueTask(LoadDataTask task, boolean isPriority)
	{
		synchronized(sLock)
		{
			sIsWorking = true;
			debug("Task ENQUEUE: " + task.toString()); //$NON-NLS-1$

			if (isPriority)
				mPriorityQueuedTasks.add(task);
			else
				mQueuedTasks.add(task);
		}
	}

	/**
	 * Gets the next task to execute from the queue, if any.
	 */
	private LoadDataTask dequeueTask()
	{
		synchronized(sLock)
		{
			LoadDataTask task;

			// Check for "priority" tasks first (these are presumed to execute fast).
			task = mPriorityQueuedTasks.poll();
			if (task == null)
				task = mQueuedTasks.poll();

			if (task != null)
				debug("Task DEQUEUE: " + task.toString()); //$NON-NLS-1$
			else
				debug("Task DEQUEUE: <nothing to do>"); //$NON-NLS-1$

			sIsWorking = (task != null);
			return task;
		}
	}

	/**
	 * Queries if there are any tasks currently executing.
	 */
	public static boolean isWorking()
	{
		synchronized(sLock)
		{
			return sIsWorking;
		}
	}

	private void runNextTask()
	{
		if (mRunningTask != null)
			return; // Some previous task is still running.

		// Get the next (pending) task.
		LoadDataTask task = dequeueTask();
		if (task != null)
		{
			if (!task.getStatus().equals(AsyncTask.Status.PENDING))
				throw new IllegalStateException("Task in queue is not pending!"); //$NON-NLS-1$

			mRunningTask = task;
			task.execute();
		}
	}

	/**
	 * Called after a LoadDataTask has finished, either successfully or not.
	 * @param task
	 */
	void afterFinish(LoadDataTask task)
	{
		if (task != mRunningTask)
			throw new IllegalArgumentException("Finished task is not the currently running one."); //$NON-NLS-1$

		debug("Task FINISHED: " + task.toString()); //$NON-NLS-1$
		mRunningTask = null;
		runNextTask();
	}

	/**
	 * Called by LoadDataTask to announce a data result (either cached or from server).
	 * @param intentKey Intent action filter (supplied by the call to service).
	 * @param result Data returned by EntityDataProvider.
	 */
	void announceEntityData(LoadDataTask task, String intentFilter, EntityServiceResponse response)
	{
		Intent intent = new Intent(intentFilter);
		debug("Task PROGRESS: " + task.toString()); //$NON-NLS-1$
		debug(String.format("Task PROGRESS DATA: UpToDate=%s, MoreData=%s, Source=%s", response.isUpToDate(), response.hasMoreData(), response.getSource())); //$NON-NLS-1$

		EntityServiceResponse.put(intent, response);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}

	private static void debug(String message)
	{
		if (LOG_ENABLED)
			Services.Log.debug(LOG_TAG, message);
	}
}
