package com.artech.activities;

import java.util.List;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnSystemUiVisibilityChangeListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.VideoView;

import com.artech.R;
import com.artech.android.media.utils.VideoUtils;
import com.artech.base.metadata.enums.Orientation;
import com.artech.base.services.Services;
import com.artech.controls.LoadingIndicatorView;

/**
 * Relevant samples that illustrate the interaction with SystemUI flags.
 * 1. https://android.googlesource.com/platform/development/+/master/samples/ApiDemos/src/com/example/android/apis/view/VideoPlayerActivity.java
 * 2. https://android.googlesource.com/platform/developers/samples/android/+/master/ui/window/AdvancedImmersiveMode/AdvancedImmersiveModeSample/src/main/java/com/example/android/advancedimmersivemode/AdvancedImmersiveModeFragment.java
 */
public class VideoViewActivity extends AppCompatActivity implements	MediaPlayer.OnPreparedListener,
																	MediaPlayer.OnErrorListener,
																	MediaPlayer.OnCompletionListener {
	public static final String INTENT_EXTRA_LINK = "Link";
	public static final String INTENT_EXTRA_IS_AUDIO = "IsAudio";
	public static final String INTENT_EXTRA_SHOW_BUTTONS = "ShowButtons";
	public static final String INTENT_EXTRA_ORIENTATION = "Orientation";
	public static final String INTENT_EXTRA_CURRENT_POSITION = "CurrentPosition";
	private static final int AUTO_HIDE_TIMEOUT = 3000;	// time in milliseconds
	
	private LoadingIndicatorView mBufferingIndicator = null;
	private VideoView mVideoView = null;
	private View mDecorView = null;
	private MediaController mMediaController = null;
	private Uri mVideoUri = null;
	private boolean mIsAudio = false;
	private boolean mShowButtons = false;
	private String mOrientation = null;
	private int mCurrentPosition = 0;
	private int mLastSystemUiVis = 0;
	private Handler mHandler = null;
	
	private Runnable mHideNavigation = new Runnable() {
		
		@Override
		public void run() {
			setNavigationVisibility(false);
		}
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);

		final Object data = getLastCustomNonConfigurationInstance();
		if (data != null) {
			mCurrentPosition = Integer.parseInt(data.toString());
		}

		Intent intent = getIntent();
		if (intent.hasExtra(INTENT_EXTRA_LINK))
			mVideoUri = Uri.parse(intent.getStringExtra(INTENT_EXTRA_LINK));
		if (intent.hasExtra(INTENT_EXTRA_IS_AUDIO))
			mIsAudio = intent.getBooleanExtra(INTENT_EXTRA_IS_AUDIO, false);
		if (intent.hasExtra(INTENT_EXTRA_SHOW_BUTTONS))
			mShowButtons = intent.getBooleanExtra(INTENT_EXTRA_SHOW_BUTTONS, false);
		if (intent.hasExtra(INTENT_EXTRA_ORIENTATION))
			mOrientation = intent.getStringExtra(INTENT_EXTRA_ORIENTATION);
		if (intent.hasExtra(INTENT_EXTRA_CURRENT_POSITION))
			mCurrentPosition = intent.getIntExtra(INTENT_EXTRA_CURRENT_POSITION, 0);
		
		if (VideoUtils.isYouTubeUrl(mVideoUri.toString())) {
			viewInYouTubeApp(mVideoUri);
			finish();
			return;
		}

		if (mIsAudio) {
			// Prevent rotation, because that would restart the audio currently playing.
			// This is a stopgap measure until we have a bound service for playing audio.
			ActivityHelper.setOrientation(this, Services.Device.getScreenOrientation());
		}

		if (mOrientation != null) {
			ActivityHelper.setOrientation(this, mOrientation.equals(Orientation.LANDSCAPE.toString()) ? Orientation.LANDSCAPE : Orientation.PORTRAIT);
		}
		
		mHandler = new Handler(Looper.getMainLooper());

		setupLayout();
		setupCallbacks();
	}
	
	private void setupLayout() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
		
		setContentView(R.layout.videoviewlayout);
		mVideoView = (VideoView) findViewById(R.id.VideoView);
		mBufferingIndicator = (LoadingIndicatorView) findViewById(R.id.bufferingIndicator);
		mDecorView = getWindow().getDecorView();

		// set support toolbar, dont add action bar to video view activity, not needed.
		//Toolbar toolbar = (Toolbar)this.findViewById(R.id.toolbar);
		//this.setSupportActionBar(toolbar);
	}
	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void setupCallbacks() {
		mVideoView.setOnPreparedListener(this);
		mVideoView.setOnErrorListener(this);
		mVideoView.setOnCompletionListener(this);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			mDecorView.setOnTouchListener(new OnTouchListener() {
				
				@SuppressLint("ClickableViewAccessibility")
                @Override
				public boolean onTouch(View v, MotionEvent event) {
					mHandler.removeCallbacks(mHideNavigation);
					mHandler.post(mHideNavigation);
					return true;
				}
			});
			mDecorView.setOnSystemUiVisibilityChangeListener(new OnSystemUiVisibilityChangeListener() {
				
				@Override
				public void onSystemUiVisibilityChange(int visibility) {
					int diff = mLastSystemUiVis ^ visibility;
					mLastSystemUiVis = visibility;
					if ((diff & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) != 0 && (visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
						setNavigationVisibility(true);
					}
				}
			});
		}
	}
	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void setNavigationVisibility(boolean visible) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
			return;
		}
		
		setMediaControllerVisibility(visible);
		
		int newVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
				| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
		
		if (!visible) {
			newVisibility |= View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
		}
		
		if (visible) {
			mHandler.removeCallbacks(mHideNavigation);
			mHandler.postDelayed(mHideNavigation, AUTO_HIDE_TIMEOUT);
		}
		
		mDecorView.setSystemUiVisibility(newVisibility);
	}
	
	private void setupMediaController() {
		mMediaController = new MediaController(mVideoView.getContext());
		mMediaController.setAnchorView(mVideoView);
		mVideoView.setMediaController(mMediaController);
		mMediaController.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mHandler.removeCallbacks(mHideNavigation);
				mHandler.postDelayed(mHideNavigation, AUTO_HIDE_TIMEOUT);
			}
		});
	}
	
	private void setMediaControllerVisibility(boolean visible) {
		if (mMediaController != null) {
			if (visible) {
				mMediaController.show(0);
			} else {
				mMediaController.hide();
			}
		}
	}
	
	private void startVideo() {
		if (mShowButtons) {
			setupMediaController();
		}
		mBufferingIndicator.setVisibility(View.GONE);
		mVideoView.start();
	}

	private void stopVideo() {
		mVideoView.stopPlayback();
		mBufferingIndicator.setVisibility(View.GONE);
	}

	@Override
	protected void onResume() {
		super.onResume();
		mHandler.removeCallbacks(mHideNavigation);
		mHandler.post(mHideNavigation);
		mBufferingIndicator.setVisibility(View.VISIBLE);
		mVideoView.resume();
	}

	@Override
	protected void onPause() {
		super.onPause();
		mCurrentPosition = mVideoView.getCurrentPosition();
		mBufferingIndicator.setVisibility(View.GONE);
		mVideoView.suspend();
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		mVideoView.setVideoURI(mVideoUri);
		if (mCurrentPosition > 0) {
			mVideoView.seekTo(mCurrentPosition);
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		stopVideo();
	}

	@Override
	public Object onRetainCustomNonConfigurationInstance() {
		return (mCurrentPosition > 0) ? mCurrentPosition : null;
	}
	
	private void viewInYouTubeApp(Uri url) {
		String videoId = (url.getQueryParameter("v") != null) ? url.getQueryParameter("v") : url.getLastPathSegment();
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + videoId));
		List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		if (list.size() > 0) {
			startActivity(intent);
		}
	}

	// MediaPlayer callbacks
	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		stopVideo();
		return VideoUtils.openVideoIntent(this, mVideoUri);
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		startVideo();
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		if (mIsAudio) {
			finish();
		}
	}
}
