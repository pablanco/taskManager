package com.artech.android.audio;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.artech.R;
import com.artech.android.audio.AudioPlayer.State;
import com.artech.android.notification.NotificationHelper;
import com.artech.application.MyApplication;
import com.artech.base.services.Services;
import com.artech.utils.Cast;

/**
 * Service that handles media playback. This is the Service through which we perform all the media
 * handling in our application. It waits for Intents, which signal the service to perform specific
 * operations: Play, Pause, Rewind, Skip, etc.
 */
@SuppressWarnings("deprecation")
public abstract class AudioService extends Service implements IAudioPlayerListener
{
	// These are the Intent actions that we are prepared to handle.
	public static final String ACTION_TOGGLE_PLAYBACK = "com.artech.android.audio.action.TOGGLE_PLAYBACK";
	public static final String ACTION_PLAY = "com.artech.android.audio.action.PLAY";
	public static final String ACTION_PAUSE = "com.artech.android.audio.action.PAUSE";
	public static final String ACTION_STOP = "com.artech.android.audio.action.STOP";
	public static final String ACTION_REWIND = "com.artech.android.audio.action.REWIND";
	public static final String ACTION_FAST_FORWARD = "com.artech.android.audio.action.FAST_FORWARD";
	public static final String ACTION_PLAY_URI = "com.artech.android.audio.action.PLAY_URI";

	static final String EXTRA_AUDIO_ITEM = "com.artech.android.audio.extra.AUDIO_ITEM";
	static final String EXTRA_NOTIFICATION_INTENT = "com.artech.android.audio.extra.NOTIFICATION_INTENT";

	private AudioPlayer mPlayer;

	// The ID we use for the notification for the playing audio.
	private final int NOTIFICATION_ID = 1;

	// our RemoteControlClient object, which will use remote control APIs available in
	// SDK level >= 14, if they're available.
	private RemoteControlClientCompat mRemoteControlClientCompat;

	// Dummy album art we will pass to the remote control (if the APIs are available).
	private Bitmap mDummyAlbumArt;

	// The component name of AudioIntentReceiver, for use with media button and remote control APIs
	private ComponentName mMediaButtonReceiverComponent;

	private AudioManager mAudioManager;
	private NotificationManager mNotificationManager;
	private Notification mNotification = null;
	private NotificationCompat.Builder mBuilder = null;
	private Intent mNotificationIntent;

	private final static int REWIND_FF_TIME = 10000; // 10 seconds

	/**
	 * Factory method to create an Intent used to invoke the audio service for the current process.
	 */
	public static Intent createIntent(Context context, String action)
	{
		Intent intent = new Intent(context, MyApplication.getInstance().getAudioServiceClass());
		intent.setAction(action);
		return intent;
	}

	@Override
	public void onCreate()
	{
		mPlayer = new AudioPlayer(getApplicationContext(), this, true);
		mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		mMediaButtonReceiverComponent = new ComponentName(this, MyApplication.getInstance().getAudioIntentReceiverClass());

		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mDummyAlbumArt = BitmapFactory.decodeResource(getResources(), R.drawable.appicon);
	}

	/**
	 * Called when we receive an Intent. When we receive an intent sent to us via startService(),
	 * this is the method that gets called. So here we react appropriately depending on the
	 * Intent's action, which specifies what is being requested of us.
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		String action = intent.getAction();
		if (action.equals(ACTION_TOGGLE_PLAYBACK)) processTogglePlaybackRequest();
		else if (action.equals(ACTION_PLAY)) processPlayRequest();
		else if (action.equals(ACTION_PAUSE)) processPauseRequest();
		else if (action.equals(ACTION_STOP)) processStopRequest();
		else if (action.equals(ACTION_REWIND)) processRewindRequest();
		else if (action.equals(ACTION_FAST_FORWARD)) processFastForwardRequest();
		else if (action.equals(ACTION_PLAY_URI)) processAddRequest(intent);

		return START_NOT_STICKY; // Means we started the service, but don't want it to restart in case it's killed.
	}

	private State getState()
	{
		return mPlayer.getState();
	}

	private void processTogglePlaybackRequest()
	{
		if (getState() == State.Paused || getState() == State.Stopped)
			processPlayRequest();
		else
			processPauseRequest();
	}

	@SuppressLint("InlinedApi")
	private void processPlayRequest()
	{
		AudioItem audio = mPlayer.play();
		if (audio != null)
		{
			setUpAsForeground(Services.Strings.getResource(R.string.GXM_StreamPlaying, audio.getTitle()));

			// Tell any remote controls that our playback state is 'playing'.
			if (mRemoteControlClientCompat != null)
				mRemoteControlClientCompat.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
		}
	}

	@SuppressLint("InlinedApi")
	private void processPauseRequest()
	{
		if (mPlayer.getState() != State.Stopped)
		{
			mPlayer.pause();
			// stopForeground(true);

			// Tell any remote controls that our playback state is 'paused'.
			if (mRemoteControlClientCompat != null)
				mRemoteControlClientCompat.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
		}
		else
		{
			// Received a 'Pause' request, probably from ACTION_AUDIO_BECOMING_NOISY, but we are
			// not playing (and have no audio prepared). Kill the service immediately.
			stopForeground(true);
			stopSelf();
		}
	}

	private void processRewindRequest()
	{
		mPlayer.move(-REWIND_FF_TIME);
	}

	private void processFastForwardRequest()
	{
		mPlayer.move(+REWIND_FF_TIME);
	}

	private void processStopRequest()
	{
		processStopRequest(false);
	}

	private void processStopRequest(boolean force)
	{
		mPlayer.stop(force);

		// Tell any remote controls that our playback state is 'stopped'.
		if (mRemoteControlClientCompat != null)
			mRemoteControlClientCompat.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);

		// Service is no longer necessary. Will be started again if needed.
		stopForeground(true);
		stopSelf();
	}

	private void processAddRequest(Intent intent)
	{
		// Get the intent to be associated to the notification.
		mNotificationIntent = Cast.as(Intent.class, intent.getExtras().getParcelable(EXTRA_NOTIFICATION_INTENT));

		// Audio URI comes in the data part of the Intent. But take full item if present too.
		AudioItem audio = Cast.as(AudioItem.class, intent.getExtras().getSerializable(EXTRA_AUDIO_ITEM));
		if (audio == null)
			audio = new AudioItem(intent.getData().toString());

		playAudio(audio);
	}

	@SuppressLint("InlinedApi")
	private void playAudio(AudioItem audio)
	{
		if (mPlayer.isPlaying(audio))
			return; // Do nothing if asked to play the same audio that is currently playing.

		stopForeground(true);
		setUpAsForeground(Services.Strings.getResource(R.string.GXM_StreamLoading, audio.getTitle()));

		// Use the remote control APIs (if available) to set the playback state
		if (mRemoteControlClientCompat == null)
		{
			Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
			intent.setComponent(mMediaButtonReceiverComponent);
			mRemoteControlClientCompat = new RemoteControlClientCompat(PendingIntent.getBroadcast(this,	0, intent, 0));
			RemoteControlHelper.registerRemoteControlClient(mAudioManager, mRemoteControlClientCompat);
		}

		mRemoteControlClientCompat.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
		mRemoteControlClientCompat.setTransportControlFlags(RemoteControlClient.FLAG_KEY_MEDIA_PLAY | RemoteControlClient.FLAG_KEY_MEDIA_PAUSE | RemoteControlClient.FLAG_KEY_MEDIA_REWIND | RemoteControlClient.FLAG_KEY_MEDIA_FAST_FORWARD | RemoteControlClient.FLAG_KEY_MEDIA_STOP);

		// Update the remote controls
		mRemoteControlClientCompat.editMetadata(true)
			.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, audio.getArtist())
			.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, audio.getAlbum())
			.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, audio.getTitle())
			.putBitmap(RemoteControlClientCompat.MetadataEditorCompat.METADATA_KEY_ARTWORK,	mDummyAlbumArt)
			.apply();

		// Start actually preparing the audio player.
		mPlayer.play(audio);
	}

	@Override
	public void onPrepared(AudioPlayer player, AudioItem audio)
	{
		updateNotification(Services.Strings.getResource(R.string.GXM_StreamPlaying, audio.getTitle()));
	}

	@Override
	public void onCompletion(AudioPlayer player, AudioItem audio)
	{
		// Remove the notification (and stop the service, since nothing else will be done for a time).
		mNotificationManager.cancel(NOTIFICATION_ID);
		stopForeground(true);
		stopSelf();
	}

	/** Updates the notification. */
	private void updateNotification(String text)
	{
		PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
				mNotificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		mNotification = mBuilder
			.setContentTitle(getNotificationHeader())
			.setContentText(text)
			.setContentIntent(pi)
			.build();

		mNotificationManager.notify(NOTIFICATION_ID, mNotification);
	}

	/**
	 * Configures service as a foreground service. A foreground service is a service that's doing
	 * something the user is actively aware of (such as playing music), and must appear to the
	 * user as a notification. That's why we create the notification here.
	 */
	private void setUpAsForeground(String text)
	{
		PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
				mNotificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		mBuilder = NotificationHelper.newBuilder(this);
		mNotification = mBuilder
				.setWhen(System.currentTimeMillis())
				.setContentTitle(getNotificationHeader())
				.setContentText(text)
				.setContentIntent(pi)
				.setOngoing(true)
				.build();

		startForeground(NOTIFICATION_ID, mNotification);
	}

	private CharSequence getNotificationHeader()
	{
		return getResources().getText(R.string.app_name);
	}

	/**
	 * Called when there's an error playing media. When this happens, the media player goes to
	 * the Error state. We warn the user about the error and reset the media player.
	 */
	@Override
	public boolean onError(AudioPlayer ap, int what, int extra)
	{
		stopForeground(true);
		return true;
	}

	@Override
	public void onDestroy()
	{
		// Service is being killed, so make sure we release our resources
		stopForeground(true);
		mPlayer.destroy();
	}

	public class LocalBinder extends Binder
	{
		public boolean isPlaying()
		{
			return (mPlayer != null && mPlayer.isPlaying());
		}
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		return new LocalBinder();
	}
}
