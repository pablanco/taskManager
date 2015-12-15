package com.genexus.live_editing;

import java.io.IOException;
import java.util.WeakHashMap;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;

import com.artech.activities.ActivityHelper;
import com.artech.application.MyApplication;
import com.artech.base.services.Services;
import com.facebook.stetho.okhttp.StethoInterceptor;
import com.genexus.live_editing.model.Command;
import com.genexus.live_editing.model.CommandType;
import com.genexus.live_editing.util.ActivityTracker;
import com.genexus.live_editing.util.Intents;
import com.genexus.live_editing.util.OngoingNotification;
import com.genexus.live_editing.util.SharedPreferencesHelper;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Response;

public class LiveEditingAgent {
    private static final LiveEditingAgent sInstance = new LiveEditingAgent();
    private final Gson mGson;
    private final OkHttpClient mHttpClient;
    private HttpUrl mUrl;
    private LocalHttpServer mLocalHttpServer;
    private ActivityTracker mActivityTracker;
    private LocalBroadcastManager mLocalBroadcastManager;
    private WeakHashMap<Activity, ActivityCommandReceiver> mCommandsReceivers;
    private CommandExecutor mCommandExecutor;

    private LiveEditingAgent() {
        mCommandsReceivers = new WeakHashMap<>();
        mActivityTracker = ActivityTracker.getInstance();
        mGson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).create();
        mHttpClient = new OkHttpClient();
        mHttpClient.networkInterceptors().add(new StethoInterceptor());
    }

    public static LiveEditingAgent getInstance() {
        return sInstance;
    }

    public void injectInto(MyApplication application) {
        // Start tracking activities.
        mActivityTracker.beginTracking(application);
        mActivityTracker.registerListener(mActivityTrackerListener);
        // Register listener to be notified then metadata loading has completed.
        application.registerOnMetadataLoadFinished(mMetadataLoadingListener);
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(application);
    }

    public void launch() {
        mUrl = getLiveEditingUrl();
        if (mUrl == null) {
            return;
        }
        if (mLocalHttpServer != null) {
            shutdown();
        }
        mLocalHttpServer = new LocalHttpServer(mGson, mHttpClient, mUrl, mConnectionListener);
        mLocalHttpServer.start();
        registerActionsReceiver();
        OngoingNotification.show();
    }

    public void shutdown() {
        mLocalHttpServer.shutdown();
        mLocalHttpServer = null;
        unregisterActionsReceiver();
        OngoingNotification.dismiss();
    }

    private HttpUrl getLiveEditingUrl() {
        HttpUrl defaultUrl = HttpUrl.parse(
                Services.Application.getPatternSettings().getIDEConnectionString());
        if (defaultUrl == null) {
            return null;
        }
        HttpUrl appUrl = HttpUrl.parse(MyApplication.getApp().getAPIUri());
        if (appUrl == null) {
            return null;
        }
        SharedPreferences sharedPreferences = MyApplication.getInstance().getSharedPreferences(
                SharedPreferencesHelper.PREFERENCES_FILENAME,
                Context.MODE_PRIVATE);
        SharedPreferencesHelper spHelper = new SharedPreferencesHelper(sharedPreferences, appUrl, defaultUrl);
        return spHelper.getLiveEditingUrl();
    }

    private void registerActionsReceiver() {
        IntentFilter filter = new IntentFilter(Intents.ACTION_CONNECT);
        mLocalBroadcastManager.registerReceiver(mActionsReceiver, filter);
    }

    private void unregisterActionsReceiver() {
        mLocalBroadcastManager.unregisterReceiver(mActionsReceiver);
    }

    private void registerCommandsReceiver(Activity activity) {
        ActivityCommandReceiver receiver = new ActivityCommandReceiver(activity, mLocalHttpServer);
        IntentFilter filter = new IntentFilter(Intents.EVENT_COMMAND_RECEIVED);
        mLocalBroadcastManager.registerReceiver(receiver, filter);
        mCommandsReceivers.put(activity, receiver);
    }

    private void unregisterCommandsReceiver(Activity activity) {
        BroadcastReceiver receiver = mCommandsReceivers.get(activity);
        if (receiver != null) {
            mLocalBroadcastManager.unregisterReceiver(receiver);
        }
        mCommandsReceivers.remove(activity);
    }

    private BroadcastReceiver mActionsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intents.ACTION_CONNECT.equals(action)) {
                MyApplication.getInstance().showMessage(context.getString(R.string.connecting));
                launch();
            }
        }
    };

    private MyApplication.MetadataLoadingListener mMetadataLoadingListener =
            new MyApplication.MetadataLoadingListener() {
                @Override
                public void onMetadataLoadFinished() {
                    launch();
                }
            };

    private ActivityTracker.Listener mActivityTrackerListener = new ActivityTracker.Listener() {
        @Override
        public void onActivityCreated(Activity activity) {
            if (ActivityHelper.isGenexusActivity(activity)) {
                registerCommandsReceiver(activity);
                if (mCommandExecutor != null) {
                    refreshLiveInspector();
                }
            }
        }

        @Override
        public void onActivityStarted(Activity activity) {
            if (mActivityTracker.getActivitiesRunningCount() == 0 && Services.Application.isLoaded()) {
                launch();
            }
        }

        @Override
        public void onActivityStopped(Activity activity) {
            if (mActivityTracker.getActivitiesRunningCount() == 0) {
                shutdown();
            }
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            if (ActivityHelper.isGenexusActivity(activity)) {
                unregisterCommandsReceiver(activity);
                if (mCommandExecutor != null) {
                    refreshLiveInspector();
                }
            }
        }
    };

    // TODO: Implement it.
    private void refreshLiveInspector() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Command command = new Command();
                command.setType(CommandType.INSPECT_UI);
                mCommandExecutor.enqueue(command);
            }
        }, 1000);
    }

    private LocalHttpServer.EventsListener mConnectionListener = new LocalHttpServer.EventsListener() {
        @Override
        public void onConnectionEstablished() {
            mCommandExecutor = new CommandExecutor(mUrl);
            mCommandExecutor.start();
            // TODO: Don't display the msg on every resume. Only when it connects for the first time.
            String message = MyApplication.getAppContext().getString(R.string.connection_established);
            MyApplication.getInstance().showMessage(message);
        }

        @Override
        public void onConnectionDropped() {
            if (mCommandExecutor != null) {
                mCommandExecutor.shutdown();
            }
            String message = MyApplication.getAppContext().getString(R.string.connection_failed);
            MyApplication.getInstance().showMessage(message);
        }

        @Override
        public void onMessageArrival(Response response) {
            String json;
            try {
                json = response.body().string();
            } catch (IOException e) {
                return;
            }
            Command command = mGson.fromJson(json, Command.class);
            mCommandExecutor.enqueue(command);
        }
    };
}
