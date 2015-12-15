package com.genexus.live_editing.util;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.os.Build;
import android.os.Bundle;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class ActivityTracker {
    private static final ActivityTracker sInstance = new ActivityTracker();
    private int mActivitiesRunningCount = 0;
    private List<Listener> mListeners = new CopyOnWriteArrayList<>();

    public static ActivityTracker getInstance() {
        return sInstance;
    }

    public void registerListener(Listener listener) {
        mListeners.add(listener);
    }

    public void unregisterListener(Listener listener) {
        mListeners.remove(listener);
    }

    public void beginTracking(Application application) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            application.registerActivityLifecycleCallbacks(mActivityLifecycleCallbacks);
        }
    }

    public void endTracking(Application application) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            application.registerActivityLifecycleCallbacks(mActivityLifecycleCallbacks);
        }
    }

    private ActivityLifecycleCallbacks mActivityLifecycleCallbacks = new ActivityLifecycleCallbacks() {
        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            for (Listener listener : mListeners) {
                listener.onActivityCreated(activity);
            }
        }

        @Override
        public void onActivityStarted(Activity activity) {
            for (Listener listener : mListeners) {
                listener.onActivityStarted(activity);
            }
            mActivitiesRunningCount++;
        }

        @Override
        public void onActivityResumed(Activity activity) {

        }

        @Override
        public void onActivityPaused(Activity activity) {

        }

        @Override
        public void onActivityStopped(Activity activity) {
            mActivitiesRunningCount--;
            for (Listener listener : mListeners) {
                listener.onActivityStopped(activity);
            }
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            for (Listener listener : mListeners) {
                listener.onActivityDestroyed(activity);
            }
        }
    };

    public int getActivitiesRunningCount() {
        return mActivitiesRunningCount;
    }

    public interface Listener {
        void onActivityCreated(Activity activity);

        void onActivityStarted(Activity activity);

        void onActivityStopped(Activity activity);

        void onActivityDestroyed(Activity activity);
    }
}
