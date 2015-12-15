package com.artech.android.api;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.artech.android.ActivityResourceBase;
import com.artech.android.ActivityResources;
import com.artech.android.audio.AudioItem;
import com.artech.android.audio.AudioPlayerBackground;
import com.artech.android.audio.AudioPlayerMix;
import com.artech.android.audio.AudioPlayerSolo;
import com.artech.android.audio.IAudioPlayer;
import com.artech.base.services.Services;
import com.artech.base.utils.Function;
import com.artech.base.utils.SafeBoundsList;
import com.artech.externalapi.ExternalApi;
import com.artech.externalapi.ExternalApiResult;

public class AudioApi extends ExternalApi
{
	private static final String METHOD_PLAY = "Play"; //$NON-NLS-1$
	private static final String METHOD_PLAY_BACKGROUND = "PlayBackground"; //$NON-NLS-1$
	private static final String METHOD_STOP = "Stop"; //$NON-NLS-1$
	private static final String METHOD_IS_PLAYING = "IsPlaying"; //$NON-NLS-1$

	private static final String TYPE_BACKGROUND = "0"; //$NON-NLS-1$
	private static final String TYPE_FOREGROUND = "(foreground)"; //$NON-NLS-1$
	private static final String TYPE_MIX = "1"; //$NON-NLS-1$
	private static final String TYPE_SOLO = "2"; //$NON-NLS-1$

	@Override
	public @NonNull ExternalApiResult execute(String method, List<Object> parameters)
	{
		SafeBoundsList<String> parameterValues = toString(parameters);

		if (METHOD_PLAY.equalsIgnoreCase(method) && parameterValues.size() == 2)
		{
			play(parameterValues.get(0), parameterValues.get(1), null);
			return ExternalApiResult.SUCCESS_CONTINUE;
		}
		else if (METHOD_PLAY_BACKGROUND.equalsIgnoreCase(method) && parameterValues.size() >= 1)
		{
			play(parameterValues.get(0), TYPE_BACKGROUND, parameterValues.get(1));
			return ExternalApiResult.SUCCESS_CONTINUE;
		}
		else if (METHOD_STOP.equalsIgnoreCase(method))
		{
			String type = parameterValues.get(0);
			getActivityAudio().stop(type);
			return ExternalApiResult.SUCCESS_CONTINUE;
		}
		else if (METHOD_IS_PLAYING.equalsIgnoreCase(method))
		{
			String type = parameterValues.get(0);
			Boolean result = getActivityAudio().isPlaying(type);
			return ExternalApiResult.success(result.toString());
		}
		else
			return ExternalApiResult.failureUnknownMethod(this, method);
	}

	private void play(String uri, String type, String description)
	{
		if (Services.Strings.hasValue(uri))
		{
			AudioItem audio = new AudioItem(uri, description);
			getActivityAudio().play(audio, type);
		}
	}

	private ActivityAudio getActivityAudio()
	{
		Activity activity = getActivity();

		return ActivityResources.getResource(activity, ActivityAudio.class,
			new Function<Activity, ActivityAudio>()
			{
				@Override
				public ActivityAudio run(Activity activity) { return new ActivityAudio(activity); }
			});
	}

	private static class ActivityAudio extends ActivityResourceBase
	{
		private final IAudioPlayer mPlayerBackground;
		private final IAudioPlayer mPlayerSolo;
		private final IAudioPlayer mPlayerMix;

		private ActivityAudio(Activity activity)
		{
			// TODO: Create an intent that will open this activity only if it was closed.
			// As a temporary hack, set FLAG_ACTIVITY_SINGLE_TOP. This will return to the application
			// but most likely not create a new activity, since our activities are almost all the same class.
			Intent notificationIntent = new Intent(activity.getIntent());
			notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

			// Create all audio players with the Application context to prevent memory leaks
			// (e.g. a background audio player will most surely live longer than the activity that starts it).
			Context context = activity.getApplicationContext();

			mPlayerBackground = new AudioPlayerBackground(context, notificationIntent);
			mPlayerSolo = new AudioPlayerSolo(context);
			mPlayerMix = new AudioPlayerMix(context);
		}

		public synchronized void play(AudioItem audio, String type)
		{
			if (type == null)
				type = TYPE_BACKGROUND;

			// Note: Solo + Mix mixes, but Mix + Solo stops mix. Account for this second case.
			if (TYPE_SOLO.equalsIgnoreCase(type) && mPlayerMix.isPlaying())
				mPlayerMix.stop();

			for (IAudioPlayer player : getPlayers(type))
			{
				// Should be only one, but break after starting just in case.
				player.play(audio);
				break;
			}
		}

		@SuppressWarnings("unused")
		public synchronized void pause(String type)
		{
			for (IAudioPlayer player : getPlayers(type))
				player.pause();
		}

		public synchronized void stop(String type)
		{
			for (IAudioPlayer player : getPlayers(type))
				player.stop();
		}

		public synchronized boolean isPlaying(String type)
		{
			for (IAudioPlayer player : getPlayers(type))
				if (player.isPlaying())
					return true;

			return false;
		}

		private ArrayList<IAudioPlayer> getPlayers(String type)
		{
			// If type is undefined, return all.
			// Foreground comprises both mix and solo.
			ArrayList<IAudioPlayer> players = new ArrayList<IAudioPlayer>();

			if (type == null || TYPE_MIX.equalsIgnoreCase(type) || TYPE_FOREGROUND.equalsIgnoreCase(type))
				players.add(mPlayerMix);
			if (type == null || TYPE_SOLO.equalsIgnoreCase(type) || TYPE_FOREGROUND.equalsIgnoreCase(type))
				players.add(mPlayerSolo);
			if (type == null || TYPE_BACKGROUND.equalsIgnoreCase(type))
				players.add(mPlayerBackground);

			return players;
		}

		@Override
		public synchronized void onResume(Activity activity)
		{
			// Resume foreground audio when activity is resumed.
			for (IAudioPlayer player : getPlayers(TYPE_FOREGROUND))
				player.play();
		}

		@Override
		public synchronized void onPause(Activity activity)
		{
			// Pause foreground audio when activity is paused.
			for (IAudioPlayer player : getPlayers(TYPE_FOREGROUND))
				player.pause();
		}

		@Override
		public synchronized void onDestroy(Activity activity)
		{
			for (IAudioPlayer player : getPlayers(TYPE_FOREGROUND))
				player.stop();
		}
	}
}
