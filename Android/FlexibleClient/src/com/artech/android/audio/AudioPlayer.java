package com.artech.android.audio;

import java.io.IOException;

import android.content.ComponentName;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.PowerManager;
import android.util.Log;

import com.artech.application.MyApplication;
import com.artech.base.utils.MathUtils;

/**
 * Specialization of MediaPlayer used for Audio in background (either in a service or bound to an Activity).
 * @author matiash
 */
class AudioPlayer implements AudioFocusable, OnPreparedListener, OnBufferingUpdateListener, OnCompletionListener, OnErrorListener
{
	private static String LOG_TAG = "AudioPlayer";

	private final Context mContext;
	private final IAudioPlayerListener mListener;

	// Media Player component.
	private MediaPlayer mPlayer;

	// indicates the state of the media player.
	public enum State
	{
		Stopped,    // media player is stopped and not prepared to play
		Preparing,  // media player is preparing...
		Playing,    // playback active (media player ready!). (but the media player may actually be
		// paused in this state if we don't have audio focus. But we stay in this state
		// so that we know we have to resume playback once we get focus back)
		Paused      // playback paused (media player ready!)
	}

	private enum AudioFocus
	{
		NoFocusNoDuck,    // we don't have audio focus, and can't duck
		NoFocusCanDuck,   // we don't have focus, but can play at a low volume ("ducking")
		Focused           // we have full audio focus
	}

	private State mState = State.Stopped;
	private AudioItem mCurrentAudio = null;

	// Wi-Fi lock, so that the wifi isn't shut down while streaming.
	private WifiLock mWifiLock;

	// Whether the audio we are playing is streaming from the network
	private boolean mIsStreaming = false;

	// For use with media button and remote control APIs.
	private ComponentName mMediaButtonReceiverComponent;
	private AudioManager mAudioManager;

	// AudioFocusHelper, if available (SDK level >= 8).
	// DUCK_VOLUME is the volume we set the media player to when we lose audio focus, but are allowed to continue playback.
	private final boolean mUseAudioFocus;
	private AudioFocusHelper mAudioFocusHelper = null;
	private AudioFocus mAudioFocus = AudioFocus.NoFocusNoDuck;
	private static final float DUCK_VOLUME = 0.1f;

	public AudioPlayer(Context context, IAudioPlayerListener listener, boolean useAudioFocus)
	{
		mContext = context;
		mListener = listener;
		mUseAudioFocus = useAudioFocus;

		// Create the Wifi lock (this does not acquire the lock, this just creates it)
		mWifiLock = ((WifiManager)mContext.getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, "AudioPlayer_Lock");

		// Get the audio manager service (used to registed media button listener).
		mAudioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);

		if (mUseAudioFocus)
			mAudioFocusHelper = new AudioFocusHelper(mContext, this);
		else
			mAudioFocus = AudioFocus.Focused; // Not interested in audio focus, act as if we had it.

		mMediaButtonReceiverComponent = new ComponentName(mContext, MyApplication.getInstance().getAudioIntentReceiverClass());
	}

	/**
	 * Returns the current State of the AudioPlayer.
	 */
	public State getState()
	{
		return mState;
	}

	public boolean isPlaying()
	{
		return (mState == State.Playing || mState == State.Preparing);
	}

	public boolean isPlaying(AudioItem audio)
	{
		return (isPlaying() && AudioItem.areEqual(mCurrentAudio, audio));
	}

	/**
	 * Starts playing the supplied audio item.
	 */
	public void play(AudioItem audio)
	{
		if (isPlaying(audio))
			return; // Do nothing if asked to play the same audio that is currently playing.

		mCurrentAudio = null;
		mState = State.Stopped;
		relaxResources(false); // release everything except MediaPlayer

		try
		{
			mCurrentAudio = audio;
			mIsStreaming = audio.isRemote();
			tryToGetAudioFocus();

			createMediaPlayerIfNeeded();
			mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mPlayer.setDataSource(audio.getAudioPlayerUri());
			mState = State.Preparing;

			// Use the media button APIs (if available) to register ourselves for media button events
			MediaButtonHelper.registerMediaButtonEventReceiverCompat(mAudioManager, mMediaButtonReceiverComponent);

			// Starts preparing the media player in the background. When it's done, it will call onPrepared().
			mPlayer.prepareAsync();

			// If we are streaming from the internet, we want to hold a Wifi lock, which prevents
			// the Wifi radio from going to sleep while the song is playing. If, on the other hand,
			// we are *not* streaming, we want to release the lock if we were holding it before.
			if (mIsStreaming)
				mWifiLock.acquire();
			else if (mWifiLock.isHeld())
				mWifiLock.release();
		}
		catch (IOException ex)
		{
			Log.e(LOG_TAG, "IOException playing next song: " + ex.getMessage());
			ex.printStackTrace();
		}
	}

	/**
	 * Resumes the currently paused item or replays the last one.
	 * @return The AudioItem that started/continued playing, or null.
	 */
	public AudioItem play()
	{
		if (mState == State.Stopped)
		{
			// If we're stopped, play the last audio again.
			if (mCurrentAudio != null)
				play(mCurrentAudio);
		}
		else if (mState == State.Paused)
		{
			// If we're paused, just continue playback.
			mState = State.Playing;
			tryToGetAudioFocus();
			configAndStartMediaPlayer();
		}

		return mCurrentAudio;
	}

	public void pause()
	{
		if (mState == State.Playing)
		{
			// Pause media player and cancel the 'foreground service' state.
			mState = State.Paused;
			mPlayer.pause();
			relaxResources(false); // while paused, we always retain the MediaPlayer
			// do not give up audio focus
		}
	}

	public void stop(boolean force)
	{
		if (mState == State.Playing || mState == State.Paused || force)
		{
			mCurrentAudio = null;
			mState = State.Stopped;

			// let go of all resources...
			relaxResources(true);
			giveUpAudioFocus();
		}
	}

	public void move(int delta)
	{
		if (mState == State.Playing || mState == State.Paused)
		{
			if (mPlayer.getCurrentPosition() != 0 && mPlayer.getDuration() != 0)
				mPlayer.seekTo(MathUtils.constrain(mPlayer.getCurrentPosition() + delta, 0, mPlayer.getDuration()));
		}
	}

	/**
	 * Makes sure the media player exists and has been reset. This will create the media player
	 * if needed, or reset the existing media player if one already exists.
	 */
	private void createMediaPlayerIfNeeded()
	{
		if (mPlayer == null)
		{
			mPlayer = new MediaPlayer();

			// Make sure the media player will acquire a wake-lock while playing. If we don't do
			// that, the CPU might go to sleep while the song is playing, causing playback to stop.
			// Remember that to use this, we have to declare the android.permission.WAKE_LOCK
			// permission in AndroidManifest.xml.
			mPlayer.setWakeMode(mContext, PowerManager.PARTIAL_WAKE_LOCK);

			// we want the media player to notify us when it's ready preparing, and when it's done
			// playing:
			mPlayer.setOnPreparedListener(this);
			mPlayer.setOnCompletionListener(this);
			mPlayer.setOnBufferingUpdateListener(this);
			mPlayer.setOnErrorListener(this);
		}
		else
			mPlayer.reset();
	}

	@Override
	public void onPrepared(MediaPlayer mp)
	{
		// The media player is done preparing. That means we can start playing!
		mState = State.Playing;

		if (mListener != null)
			mListener.onPrepared(this, mCurrentAudio);

		configAndStartMediaPlayer();
	}

	@Override
	public void onCompletion(MediaPlayer mp)
	{
		mCurrentAudio = null;
		mState = State.Stopped;

		relaxResources(false);
		giveUpAudioFocus();

		if (mListener != null)
			mListener.onCompletion(this, mCurrentAudio);
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra)
	{
		// Toast.makeText(getApplicationContext(), "Media player error! Resetting.", Toast.LENGTH_SHORT).show();
		Log.e(LOG_TAG, "Error: what=" + String.valueOf(what) + ", extra=" + String.valueOf(extra));

		mCurrentAudio = null;
		mState = State.Stopped;
		relaxResources(true);
		giveUpAudioFocus();

		if (mListener != null)
			mListener.onError(this, what, extra);

		return true;
	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent)
	{
		// Nothing, set just to prevent error message in Log. But we could notify about percent or something.
	}

	/**
	 * Reconfigures MediaPlayer according to audio focus settings and starts/restarts it. This
	 * method starts/restarts the MediaPlayer respecting the current audio focus state. So if
	 * we have focus, it will play normally; if we don't have focus, it will either leave the
	 * MediaPlayer paused or set it to a low volume, depending on what is allowed by the
	 * current focus settings. This method assumes mPlayer != null, so if you are calling it,
	 * you have to do so from a context where you are sure this is the case.
	 */
	private void configAndStartMediaPlayer()
	{
		if (mAudioFocus == AudioFocus.NoFocusNoDuck)
		{
			// If we don't have audio focus and can't duck, we have to pause, even if mState
			// is State.Playing. But we stay in the Playing state so that we know we have to resume
			// playback once we get the focus back.
			if (mPlayer.isPlaying())
				mPlayer.pause();

			return;
		}
		else if (mAudioFocus == AudioFocus.NoFocusCanDuck)
			mPlayer.setVolume(DUCK_VOLUME, DUCK_VOLUME);  // we'll be relatively quiet
		else
			mPlayer.setVolume(1.0f, 1.0f); // we can be loud

		if (!mPlayer.isPlaying())
			mPlayer.start();
	}

	private void tryToGetAudioFocus()
	{
		if (mAudioFocus != AudioFocus.Focused && mAudioFocusHelper != null && mAudioFocusHelper.requestFocus())
			mAudioFocus = AudioFocus.Focused;
	}

	/**
	 * Releases resources used for playback. This includes the wake locks and possibly the MediaPlayer.
	 * @param releaseMediaPlayer Indicates whether the Media Player should also be released or not
	 */
	private void relaxResources(boolean releaseMediaPlayer)
	{
		// stop and release the Media Player, if it's available
		if (releaseMediaPlayer && mPlayer != null)
		{
			mPlayer.reset();
			mPlayer.release();
			mPlayer = null;
			mState = State.Stopped;
		}

		// we can also release the Wifi lock, if we're holding it
		if (mWifiLock.isHeld())
			mWifiLock.release();
	}

	private void giveUpAudioFocus()
	{
		if (mAudioFocus == AudioFocus.Focused && mAudioFocusHelper != null && mAudioFocusHelper.abandonFocus())
			mAudioFocus = AudioFocus.NoFocusNoDuck;
	}

	@Override
	public void onGainedAudioFocus()
	{
		// Toast.makeText(getApplicationContext(), "gained audio focus.", Toast.LENGTH_SHORT).show();
		mAudioFocus = AudioFocus.Focused;

		// restart media player with new focus settings
		if (mState == State.Playing)
			configAndStartMediaPlayer();
	}

	@Override
	public void onLostAudioFocus(boolean canDuck)
	{
		// Toast.makeText(getApplicationContext(), "lost audio focus." + (canDuck ? "can duck" : "no duck"), Toast.LENGTH_SHORT).show();
		mAudioFocus = canDuck ? AudioFocus.NoFocusCanDuck : AudioFocus.NoFocusNoDuck;

		// start/restart/pause media player with new focus settings
		if (mPlayer != null && mPlayer.isPlaying())
			configAndStartMediaPlayer();
	}

	public void destroy()
	{
		// Release everything.
		stop(true);
	}
}
