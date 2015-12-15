package com.genexus.live_editing;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.artech.common.ServiceHelper;
import com.genexus.live_editing.model.Command;
import com.google.gson.Gson;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;


public class LocalHttpServer extends Thread {
    private static final int CONNECT_TIMEOUT = 2;
    private static final int READ_TIMEOUT = 0;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final Gson mGson;
    private final OkHttpClient mHttpClient;
    private final HttpUrl mUrl;
    private EventsListener mListener;
    private Call mCall;
    private boolean mStop = false;

    public LocalHttpServer(Gson gson, OkHttpClient httpClient, HttpUrl url, EventsListener listener) {
        mGson = gson;
        mHttpClient = httpClient;
        mUrl = url;
        mListener = listener;
        mHttpClient.setConnectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS);
        mHttpClient.setReadTimeout(READ_TIMEOUT, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mListener != null && !mStop) {
                    mListener.onConnectionEstablished();
                }
            }
        }, (CONNECT_TIMEOUT + 1) * 1000);
        Request request = new Request.Builder()
                .url(mUrl)
                .headers(Headers.of(ServiceHelper.Headers.getMobileHeaders()))
                .build();
        try {
            while (!mStop) {
                mCall = mHttpClient.newCall(request); // TODO: Check concurrency when shutdown() is called, or use client.cancel(tag).
                Response response = mCall.execute();
                if (mListener != null && response.isSuccessful()) {
                    mListener.onMessageArrival(response);
                }
            }
        } catch (IOException e) {
            Log.e("LiveEditing", e.getMessage());
        }
        mStop = true;
        if (mListener != null) {
            mListener.onConnectionDropped();
        }
    }

    public void shutdown() {
        mStop = true;
        mListener = null;
        mCall.cancel();
    }

    public interface EventsListener {
        void onConnectionEstablished();

        void onConnectionDropped();

        void onMessageArrival(Response response);
    }

    public void send(Command command) {
        String commandJson = mGson.toJson(command);
        RequestBody body = RequestBody.create(JSON, commandJson);
        Request request = new Request.Builder()
                .url(mUrl)
                .headers(Headers.of(ServiceHelper.Headers.getMobileHeaders()))
                .post(body)
                .build();
        mHttpClient.newCall(request).enqueue(mOkHttpCallback);
    }

    private Callback mOkHttpCallback = new Callback() {
        @Override
        public void onFailure(Request request, IOException e) {
            Log.e("LiveEditing", e.getMessage());
        }

        @Override
        public void onResponse(Response response) throws IOException {
            // Do nothing.
        }
    };
}
