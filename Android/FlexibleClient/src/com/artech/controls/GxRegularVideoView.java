package com.artech.controls;

import android.content.Context;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.VideoView;

import com.artech.R;
import com.artech.activities.ActivityLauncher;
import com.artech.android.media.utils.VideoUtils;
import com.artech.base.controls.IGxControlNotifyEvents;
import com.artech.base.metadata.enums.Orientation;
import com.artech.base.metadata.layout.LayoutItemDefinition;



public class GxRegularVideoView extends FrameLayout implements IGxControlNotifyEvents {
	private LayoutItemDefinition mDefinition = null;
	private Uri mVideoUri = null;
	private VideoView mVideoView = null;
	private ProgressBar mBufferingIndicator = null;
	private ImageButton mPlayButton = null;
	private ImageButton mFullscreenButton = null;
	private boolean mIsBuffering = false;
	private int mCurrentPosition = 1;

	private enum State {INIT, PAUSED, RESUMED}
	private State state = State.INIT;

	public GxRegularVideoView(Context context, LayoutItemDefinition definition, Uri videoUri, int currentPosition) {
		super(context);
		mDefinition = definition;
		mVideoUri = videoUri;
		mCurrentPosition = currentPosition;
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		setupLayout();
        setupCallbacks();
		prepareVideo();
	}

	private void setupLayout() {
		mVideoView = new VideoView(getContext());
		mBufferingIndicator = new ProgressBar(getContext());
		mPlayButton = new ImageButton(getContext());
		mFullscreenButton = new ImageButton(getContext());
		
		mVideoView.setBackgroundColor(Color.TRANSPARENT);
		mBufferingIndicator.setBackgroundColor(Color.TRANSPARENT);
		mPlayButton.setBackgroundColor(Color.TRANSPARENT);
		mFullscreenButton.setBackgroundColor(Color.TRANSPARENT);
		
		mBufferingIndicator.setIndeterminate(true);
		mPlayButton.setImageResource(R.drawable.gx_domain_action_play_dark);
		mFullscreenButton.setImageResource(R.drawable.gx_ic_fullscreen_grey_36dp);
		mFullscreenButton.setPadding(0, 0, 0, 0);
		
		addView(mVideoView, 0, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, mDefinition.CellGravity));
		addView(mBufferingIndicator, 1, new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
		addView(mPlayButton, 2, new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
		addView(mFullscreenButton, 3, new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.RIGHT));
	}

	private void setupCallbacks() {
		mVideoView.setOnPreparedListener(mOnPreparedListener);
        mVideoView.setOnErrorListener(mOnErrorListener);
        mPlayButton.setOnClickListener(mOnPlayClickListener);
        mPlayButton.setOnTouchListener(mOnPlayTouchListener);
        mFullscreenButton.setOnClickListener(mOnFullscreenClickListener);
        mFullscreenButton.setOnTouchListener(mOnFullscreenTouchListener);
	}

	private void setupMediaController() {
		MediaController mediaController = new MediaController(mVideoView.getContext());
		mediaController.setAnchorView(mVideoView);
		mediaController.setForegroundGravity(mDefinition.CellGravity);
		mVideoView.setMediaController(mediaController);
	}

	private void prepareVideo() {
		mPlayButton.setVisibility(View.GONE);
		setBufferingIndicator(true);
		mVideoView.setMediaController(null);
		mVideoView.setVideoURI(mVideoUri);
	}

	private void startVideo() {
		setupMediaController();
		mPlayButton.setVisibility(View.GONE);
		mVideoView.start();
	}

	private void stopVideo() {
		mVideoView.suspend();
		mVideoView.stopPlayback();
		setBufferingIndicator(false);
	}
	
	private void setBufferingIndicator(boolean isBuffering) {
		if (mIsBuffering != isBuffering) {
			mIsBuffering = isBuffering;
			mBufferingIndicator.setVisibility(isBuffering ? View.VISIBLE : View.GONE);
		}
	}

	public Uri getVideoUri() {
		return mVideoUri;
	}

	public void setVideoUri(Uri videoUri) {
		if (!mVideoUri.equals(videoUri)) {
			mVideoUri = videoUri;
			stopVideo();
			prepareVideo();
		}
	}
	
	public int getCurrentPosition() {
		return mCurrentPosition;
	}
	
	public void setCurrentPosition(int currentPosition) {
		mCurrentPosition = currentPosition;
	}

	@Override
	public void notifyEvent(EventType type) {
		if (mVideoView != null) {
			if (type == EventType.ACTIVITY_PAUSED && state != State.PAUSED) {
				if (mVideoView.getCurrentPosition() > 0) {
					mCurrentPosition = mVideoView.getCurrentPosition();
				}
				stopVideo();
				state = State.PAUSED;
			} else if (type == EventType.ACTIVITY_RESUMED && state != State.RESUMED) {
				stopVideo();
				prepareVideo();
				state = State.RESUMED;
			}
		}
	}
	
	private MediaPlayer.OnPreparedListener mOnPreparedListener = new MediaPlayer.OnPreparedListener() {
		
		@Override
		public void onPrepared(MediaPlayer mp) {
			mp.setOnInfoListener(mOnInfoListener);
			
			if ((mVideoView.getCurrentPosition() < mCurrentPosition && mVideoView.canSeekForward()) ||
					(mVideoView.getCurrentPosition() > mCurrentPosition && mVideoView.canSeekBackward())) {
				mVideoView.seekTo(mCurrentPosition);
			}
			
			mVideoView.pause();
			setBufferingIndicator(false);
	        mPlayButton.setVisibility(View.VISIBLE);
		}
	};
	
	private MediaPlayer.OnInfoListener mOnInfoListener = new MediaPlayer.OnInfoListener() {
		
		@Override
		public boolean onInfo(MediaPlayer mp, int what, int extra) {
			if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
				setBufferingIndicator(true);
			} else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
				setBufferingIndicator(false);
			}
			
			return false;
		}
	};
	
	private MediaPlayer.OnErrorListener mOnErrorListener = new MediaPlayer.OnErrorListener() {
		
		@Override
		public boolean onError(MediaPlayer mp, int what, int extra) {
			stopVideo();
			return VideoUtils.openVideoIntent(getContext(), mVideoUri);
		}
	};
	
	private View.OnClickListener mOnPlayClickListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			startVideo();
		}
	};
	
	private View.OnTouchListener mOnPlayTouchListener = new View.OnTouchListener() {
		
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				mPlayButton.setColorFilter(Color.argb(191, 0, 0, 0));
			} else if (event.getAction() == MotionEvent.ACTION_UP) {
				mPlayButton.setColorFilter(Color.TRANSPARENT);
			}
			return false;
		}
	};
	
	private View.OnClickListener mOnFullscreenClickListener = new View.OnClickListener() {
    	
    	@Override
    	public void onClick(View v) {
    		ActivityLauncher.CallViewVideoFullscreen(getContext(), mVideoUri.toString(), Orientation.LANDSCAPE, mVideoView.getCurrentPosition());
    	}
    };
    
    private View.OnTouchListener mOnFullscreenTouchListener = new View.OnTouchListener() {
		
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				mFullscreenButton.setColorFilter(Color.argb(191, 255, 255, 255));
			} else if (event.getAction() == MotionEvent.ACTION_UP) {
				mFullscreenButton.setColorFilter(Color.TRANSPARENT);
			}
			return false;
		}
	};
}
