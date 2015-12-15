package com.artech.android.facebookapi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;

public class FacebookLoginActivity extends FragmentActivity {
	public final static String EXTRA_FACEBOOK_ACCESS_TOKEN = "FacebookAccessToken";
	private UiLifecycleHelper uiHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		uiHelper = new UiLifecycleHelper(this, statusCallback);
		uiHelper.onCreate(savedInstanceState);

		setContentView(R.layout.com_artech_facebook_login_activity_layout);
	}

	@Override
	protected void onResume() {
		super.onResume();
		uiHelper.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
		uiHelper.onPause();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		uiHelper.onDestroy();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		uiHelper.onActivityResult(requestCode, resultCode, data);
	}

	private final Session.StatusCallback statusCallback = new Session.StatusCallback() {

		@Override
		public void call(Session session, SessionState state, Exception exception) {
			onSessionStateChanged(session, state, exception);
		}

	};

	private void onSessionStateChanged(Session session, SessionState state, Exception exception) {
		if (SessionState.OPENED.equals(state)) {
			Intent data = new Intent();
			data.putExtra(EXTRA_FACEBOOK_ACCESS_TOKEN, session.getAccessToken());
			setResult(Activity.RESULT_OK, data);
			finish();
		}
	}
}
