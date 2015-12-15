package com.artech.android.audio;

import android.content.Context;

/**
 * AudioPlayer wrapper (using audio focus).
 * @author matiash
 */
public class AudioPlayerSolo implements IAudioPlayer
{
	private AudioPlayer mPlayer;

	public AudioPlayerSolo(Context context)
	{
		mPlayer = new AudioPlayer(context, null, true); // Solo uses and respects audio focus.
	}

	@Override
	public void play(AudioItem audio)
	{
		mPlayer.play(audio);
	}

	@Override
	public boolean isPlaying()
	{
		return mPlayer.isPlaying();
	}

	@Override
	public void play()
	{
		mPlayer.play();
	}

	@Override
	public void pause()
	{
		mPlayer.pause();
	}

	@Override
	public void stop()
	{
		mPlayer.stop(true);
	}
}
