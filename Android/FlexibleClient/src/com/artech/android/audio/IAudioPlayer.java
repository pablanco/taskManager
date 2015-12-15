package com.artech.android.audio;

public interface IAudioPlayer
{
	/** Starts audio playing of the specified URI */
	void play(AudioItem audio);

	/** Returns whether the player is currently playing. */
	boolean isPlaying();

	/** Continues audio playing, if paused. */
	void play();

	/** Pauses audio playing. */
	void pause();

	/** Stops audio playing. */
	void stop();
}
