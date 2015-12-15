package com.artech.android.audio;

interface IAudioPlayerListener
{
    /**
     * Called when the media file is ready for playback.
     * @param ap the MediaPlayer that is ready for playback.
     * @param audio The item that is ready for playback.
     */
    void onPrepared(AudioPlayer ap, AudioItem audio);

    /**
     * Called when the end of a media source is reached during playback.
     * @param ap the MediaPlayer that reached the end of the file.
     * @param audio The item that was completed.
     */
    void onCompletion(AudioPlayer ap, AudioItem audio);

    /**
     * Called to indicate an error.
     *
     * @param ap      the MediaPlayer the error pertains to
     * @param what    the type of error that has occurred:
     * <ul>
     * <li>{@link #MEDIA_ERROR_UNKNOWN}
     * <li>{@link #MEDIA_ERROR_SERVER_DIED}
     * </ul>
     * @param extra an extra code, specific to the error. Typically
     * implementation dependant.
     * @return True if the method handled the error, false if it didn't.
     * Returning false, or not having an OnErrorListener at all, will
     * cause the OnCompletionListener to be called.
     */
	boolean onError(AudioPlayer ap, int what, int extra);
}
