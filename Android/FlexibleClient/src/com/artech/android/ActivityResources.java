package com.artech.android;

import java.util.ArrayList;
import java.util.WeakHashMap;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.artech.base.utils.Function;

/**
 * Manager of resources per activity.
 * Resources are API components bound to an Activity that must receive notifications from it (e.g. ProgressIndicator,
 * ActivityAudio, &c). Methods are called on the corresponding lifecycle events of their owner activities.
 *
 * @author matiash
 */
public class ActivityResources
{
	private static final Object sLock = new Object();
	private static WeakHashMap<Activity, ArrayList<IActivityResource>> sResources = new WeakHashMap<Activity, ArrayList<IActivityResource>>();
	private static WeakHashMap<Activity, Bundle> sBundles = new WeakHashMap<Activity, Bundle>();
	
	/**
	 * Save an Activity state. Activity receives the state onCreate and this state is used to create controls that need to know this state
	 * @param activity
	 * @param savedInstanceState
	 */
	public static void setActivitySavedInstanceState(Activity activity, Bundle savedInstanceState) {
		synchronized (sLock) {
			sBundles.put(activity, savedInstanceState);
		}
	}
	
	
	/**
	 * Tries to get the resource of the specified class associated to the activity.
	 * @param activity Activity that manages the resource.
	 * @param t Resource type.
	 * @return The previously existing resource, or null.
	 */
	public static <TResource extends IActivityResource> TResource getResource(Activity activity, Class<TResource> t)
	{
		synchronized (sLock)
		{
			ArrayList<IActivityResource> resources = sResources.get(activity);
			if (resources != null)
			{
				for (IActivityResource resource : resources)
					if (t.isInstance(resource))
						return t.cast(resource);
			}

			return null;
		}
	}

	/**
	 * Tries to get the resource of the specified class associated to the activity.
	 * If the resource is not present and a factory function is provided, it will be created and added.
	 * @param activity Activity that manages the resource.
	 * @param t Resource type.
	 * @param resourceFactory Function to create the resource if it doesn't exist yet. Can be null.
	 * @return The previously existing resource, or a new one if a factory is provided, or null.
	 */
	public static <TResource extends IActivityResource> TResource getResource(Activity activity, Class<TResource> t, Function<Activity, TResource> resourceFactory)
	{
		synchronized (sLock)
		{
			ArrayList<IActivityResource> resources = sResources.get(activity);
			if (resources != null)
			{
				for (IActivityResource resource : resources)
					if (t.isInstance(resource))
						return t.cast(resource);
			}

			// Resource is not present, create it if a factory was provider.
			if (resourceFactory != null)
			{
				TResource newResource = resourceFactory.run(activity);
				setResource(activity, t, newResource);
				return newResource;
			}

			return null;
		}
	}

	/**
	 * Associates a previously created resource to the activity.
	 * @param activity Activity that manages the resource.
	 * @param t Resource type.
	 * @param resource Resource instance.
	 */
	public static <TResource extends IActivityResource> void setResource(Activity activity, Class<TResource> t, TResource resource)
	{
		synchronized (sLock)
		{
			ArrayList<IActivityResource> resources = sResources.get(activity);
			if (resources == null)
			{
				resources = new ArrayList<IActivityResource>();
				sResources.put(activity, resources);
			}
			Bundle savedInstanceState = getSavedInstanceState(activity);
			resource.onCreate(activity, savedInstanceState);
			resources.add(resource);
		}
	}

	/**
	 * Notifies all the activity's resources about its onNewIntent() life-cycle event.
	 */
	public static void onNewIntent(final Activity activity, final Intent intent)
	{
		onActivityEvent(activity, new Function<IActivityResource, Void>()
		{
			@Override
			public Void run(IActivityResource input) { input.onNewIntent(activity, intent); return null; }
		});
	}

	/**
	 * Notifies all the activity's resources about its onResume() life-cycle event.
	 */
	public static void onResume(final Activity activity)
	{
		onActivityEvent(activity, new Function<IActivityResource, Void>()
		{
			@Override
			public Void run(IActivityResource input) { input.onResume(activity); return null; }
		});
	}
	
	/**
	 * Notifies all the activity's resources about its onSaveInstanceState() life-cycle event.
	 */
	public static void onSaveInstanceState(final Activity activity, final Bundle outState)
	{
		onActivityEvent(activity, new Function<IActivityResource, Void>()
		{
			@Override
			public Void run(IActivityResource input) { input.onSaveInstanceState(activity, outState); return null; }
		});
	}
	
	/**
	 * Notifies all the activity's resources about its onActivityResult() life-cycle event.
	 */
	public static void onActivityResult(final Activity activity,final int requestCode, final int resultCode, final Intent data) {
		onActivityEvent(activity, new Function<IActivityResource, Void>()
		{
			@Override
			public Void run(IActivityResource input) { input.onActivityResult(activity, requestCode, resultCode, data); return null; }
		});
	}


	/**
	 * Notifies all the activity's resources about its onPause() life-cycle event.
	 */
	public static void onPause(final Activity activity)
	{
		onActivityEvent(activity, new Function<IActivityResource, Void>()
		{
			@Override
			public Void run(IActivityResource input) { input.onPause(activity); return null; }
		});
	}

	/**
	 * Notifies all the activity's resources about its onDestroy() life-cycle event.
	 */
	public static void onDestroy(final Activity activity)
	{
		onActivityEvent(activity, new Function<IActivityResource, Void>()
		{
			@Override
			public Void run(IActivityResource input) { input.onDestroy(activity); return null; }
		});

		// After running all resource's onDestroy(), release them (they won't be called again).
		sResources.remove(activity);
	}

	private static <T> void onActivityEvent(Activity activity, Function<IActivityResource, T> event)
	{
		synchronized (sLock)
		{
			ArrayList<IActivityResource> resources = sResources.get(activity);
			if (resources != null)
			{
				for (IActivityResource resource : resources)
					event.run(resource);
			}
		}
	}


	private static Bundle getSavedInstanceState(Activity activity) {
		synchronized (sLock) {
			return sBundles.get(activity);
		}
	}
}
