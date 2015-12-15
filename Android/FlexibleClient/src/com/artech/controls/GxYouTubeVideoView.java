package com.artech.controls;

import android.content.Context;
import android.content.res.Resources;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.artech.R;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.artech.fragments.LayoutFragmentActivity;
import com.artech.utils.Cast;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayer.Provider;
import com.google.android.youtube.player.YouTubePlayerSupportFragment;
	


public class GxYouTubeVideoView extends FrameLayout implements YouTubePlayer.OnInitializedListener {
	private static final String TAG = "GxYouTubeVideoView"; //$NON-NLS-1$
	private static final int RECOVERY_DIALOG_REQUEST = 1;
	private static String DEVELOPER_KEY = Strings.EMPTY;
	private String mVideoId = Strings.EMPTY;
	private LayoutFragmentActivity mActivity = null;
	private YouTubePlayerSupportFragment mPlayerFragment = null;
	private YouTubePlayer mPlayer = null;
	
	public GxYouTubeVideoView(Context context, String videoId) {
		super(context);

		mVideoId = videoId;
		
		// Get the Developer key specified in the control property.
		try {
			DEVELOPER_KEY = Services.Strings.getResource(R.string.YoutubeAPIKey);
		} catch (Resources.NotFoundException e) {
			// Android YouTube API Key not found in the resources.
			Services.Exceptions.handle(e);
			return;
		}
		
		if (DEVELOPER_KEY.equalsIgnoreCase(Strings.EMPTY)) {
			Services.Log.Error(TAG, "YouTube API Developer Key was empty."); //$NON-NLS-1$
			return;
		}
		
		mActivity = Cast.as(LayoutFragmentActivity.class, context);
		if (mActivity == null) {
			throw new IllegalArgumentException(TAG + ": Invalid context"); //$NON-NLS-1$
		}
		
		inflate(context, R.layout.youtube_view, this);
		if (findViewById(R.id.youtube_fragment_container) == null) {
			Services.Log.Error(TAG, "Failed to find the YouTube fragment container."); //$NON-NLS-1$
			return;
		}
		
		mPlayerFragment = (YouTubePlayerSupportFragment) mActivity.getSupportFragmentManager().findFragmentById(R.id.youtube_fragment_container);

		if (mPlayerFragment != null) {
			mActivity.getSupportFragmentManager().beginTransaction().remove(mPlayerFragment).commit();
		}
		
		mPlayerFragment = YouTubePlayerSupportFragment.newInstance();
		mActivity.getSupportFragmentManager().beginTransaction().add(R.id.youtube_fragment_container, mPlayerFragment).commit();
		
		mPlayerFragment.initialize(DEVELOPER_KEY, this);
	}

	@Override
	public void onInitializationSuccess(Provider provider, YouTubePlayer player, boolean wasRestored) {
		Services.Log.debug(TAG, "Player Initialized."); //$NON-NLS-1$
		mPlayer = player;
		
		if (!wasRestored) {
			Services.Log.debug(TAG, "Video Cued."); //$NON-NLS-1$
			player.cueVideo(mVideoId);
		}
	}
	
	@Override
	public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult errorReason) {
		Services.Log.Error(TAG, "YoutubePlayer failed to initialize."); //$NON-NLS-1$
		if (errorReason.isUserRecoverableError()) {
			errorReason.getErrorDialog(mActivity, RECOVERY_DIALOG_REQUEST).show();
		} else {
			String errorMessage = Services.Strings.getResource(R.string.GXM_YoutubeError, errorReason.toString());
			Toast.makeText(mActivity.getApplicationContext(), errorMessage, Toast.LENGTH_LONG).show();
		}
	}
	
	public void retryInitialization() {
		// Retry initialization if user performed a recovery action
		mPlayerFragment.initialize(DEVELOPER_KEY, this);
	}
	
	public String getVideoId() {
		return mVideoId;
	}

	public void setVideoId(String videoId) {
		if (!mVideoId.equals(videoId)) {
			mVideoId = videoId;
			// Re-load the player
			mPlayer.cueVideo(videoId);
		}
	}
	
	public void onDestroy() {
		mActivity.getSupportFragmentManager().beginTransaction().remove(mPlayerFragment).commit();
	}

}
