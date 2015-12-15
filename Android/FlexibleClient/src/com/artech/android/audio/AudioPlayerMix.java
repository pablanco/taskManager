package com.artech.android.audio;

import java.util.ArrayList;

import android.content.Context;

/**
 * Support for concurrent (mixing) audio players.
 * @author matiash
 */
public class AudioPlayerMix implements IAudioPlayer, IAudioPlayerListener
{
	private final Context mContext;
	private final ArrayList<AudioPlayer> mPlayers;
	private final Object mLock = new Object();

	public AudioPlayerMix(Context context)
	{
		mContext = context;
		mPlayers = new ArrayList<AudioPlayer>();
	}

	@Override
	public void play(AudioItem audio)
	{
		synchronized (mLock)
		{
			// If the same audio is already playing, skip. Otherwise start a new player.
			for (AudioPlayer player : new ArrayList<AudioPlayer>(mPlayers))
				if (player.isPlaying(audio))
					return;

			AudioPlayer player = new AudioPlayer(mContext, this, false); // Mixing doesn't use/respect audio focus.
			mPlayers.add(player);
			player.play(audio);
		}
	}

	@Override
	public boolean isPlaying()
	{
		synchronized (mLock)
		{
			// Are any channels playing?
			for (AudioPlayer player : new ArrayList<AudioPlayer>(mPlayers))
				if (player.isPlaying())
					return true;

			// None.
			return false;
		}
	}

	@Override
	public void play()
	{
		synchronized (mLock)
		{
			for (AudioPlayer player : new ArrayList<AudioPlayer>(mPlayers))
				player.play();
		}
	}

	@Override
	public void pause()
	{
		synchronized (mLock)
		{
			// Stop all.
			for (AudioPlayer player : new ArrayList<AudioPlayer>(mPlayers))
				player.pause();
		}
	}

	@Override
	public void stop()
	{
		synchronized (mLock)
		{
			// Stop all.
			for (AudioPlayer player : new ArrayList<AudioPlayer>(mPlayers))
				player.stop(true);

			mPlayers.clear();
		}
	}

	@Override
	public void onPrepared(AudioPlayer ap, AudioItem audio)
	{
		// Nothing to do, playing will auto-start.
	}

	@Override
	public void onCompletion(AudioPlayer ap, AudioItem audio)
	{
		synchronized (mLock)
		{
			// Finished playing, remove player.
			ap.stop(true);
			mPlayers.remove(ap);
		}
	}

	@Override
	public boolean onError(AudioPlayer ap, int what, int extra)
	{
		synchronized (mLock)
		{
			// Finished playing, remove player.
			ap.stop(true);
			mPlayers.remove(ap);
			return true;
		}
	}
}
