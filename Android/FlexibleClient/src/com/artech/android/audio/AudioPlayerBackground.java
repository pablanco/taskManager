package com.artech.android.audio;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.IBinder;

import com.artech.android.audio.AudioService.LocalBinder;
import com.artech.application.MyApplication;

/**
 * Wrapper to control the background audio service.
 * @author matiash
 */
public class AudioPlayerBackground implements IAudioPlayer
{
	private final Context mContext;
	private final Intent mNotificationIntent;

	public AudioPlayerBackground(Context context, Intent notificationIntent)
	{
		mContext = context;
		mNotificationIntent = notificationIntent;
	}

	@Override
	public void play(AudioItem audio)
	{
		// Use a service to play background audio.
		Intent intent = AudioService.createIntent(mContext, AudioService.ACTION_PLAY_URI);

		intent.setData(Uri.parse(audio.getUri()));
		intent.putExtra(AudioService.EXTRA_AUDIO_ITEM, audio);
		intent.putExtra(AudioService.EXTRA_NOTIFICATION_INTENT, mNotificationIntent);
		mContext.startService(intent);
	}

	@Override
	public boolean isPlaying()
	{
		Intent intent = new Intent(mContext, MyApplication.getInstance().getAudioServiceClass());
		final Object bindingSignal = new Object();

		// Bind to the service just in order to query its status.
		AudioServiceConnection connection = new AudioServiceConnection(bindingSignal);
		if (mContext.bindService(intent, connection, Context.BIND_AUTO_CREATE))
		{
			// Because binding is asynchronous, wait a little for it to finish.
			try
			{
				synchronized (bindingSignal)
				{
					bindingSignal.wait(200);
				}
			}
			catch (InterruptedException e) { }

			mContext.unbindService(connection);
			return (connection != null && connection.isPlaying());
		}
		else
			return false;
	}

	@Override
	public void play()
	{
		mContext.startService(AudioService.createIntent(mContext, AudioService.ACTION_PLAY));
	}

	@Override
	public void pause()
	{
		mContext.startService(AudioService.createIntent(mContext, AudioService.ACTION_PAUSE));
	}

	@Override
	public void stop()
	{
		mContext.startService(AudioService.createIntent(mContext, AudioService.ACTION_STOP));
	}

	private static class AudioServiceConnection implements ServiceConnection
	{
		private final Object mBindingSignal;
		private boolean mIsPlaying;

		private AudioServiceConnection(Object bindingSignal)
		{
			mBindingSignal = bindingSignal;
		}

		@Override
		public void onServiceConnected(ComponentName className, IBinder service)
		{
			LocalBinder binder = (LocalBinder) service;
			mIsPlaying = binder.isPlaying();

			// Signal a successful binding, and that we got the answer.
			synchronized (mBindingSignal)
			{
				mBindingSignal.notify();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName className) { }

		boolean isPlaying() { return mIsPlaying; }
	}
}
