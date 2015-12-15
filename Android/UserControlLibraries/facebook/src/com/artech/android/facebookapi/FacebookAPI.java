package com.artech.android.facebookapi;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.text.TextUtils;

import com.artech.actions.ActionExecution;
import com.artech.actions.ActionResult;
import com.artech.activities.ActivityHelper;
import com.artech.base.metadata.enums.RequestCodes;
import com.artech.base.services.Services;
import com.artech.externalapi.ExternalApi;
import com.artech.externalapi.ExternalApiResult;
import com.facebook.FacebookException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.Session;
import com.facebook.UiLifecycleHelper;
import com.facebook.widget.FacebookDialog;
import com.facebook.widget.FacebookDialog.Callback;
import com.facebook.widget.FacebookDialog.PendingCall;
import com.facebook.widget.WebDialog;
import com.facebook.widget.WebDialog.OnCompleteListener;

public class FacebookAPI extends ExternalApi {
	// API Method Names
	private static final String METHOD_LOGIN = "Login";
	private static final String METHOD_POST = "PostToWall";

	// Internal stuff
	private Pair<String, List<Object>> mPendingOperation = null;
	private UiLifecycleHelper mUiHelper = null;

	public FacebookAPI() {
		addMethodHandler(METHOD_LOGIN, 0, mLoginMethod);
		addMethodHandler(METHOD_POST, 5, mPostToWallMethod);
	}

	@Override
	public @NonNull ExternalApiResult execute(String method, List<Object> parameters) {
		mPendingOperation = null;

		if (METHOD_LOGIN.equalsIgnoreCase(method))
		{
            // Login is a no-op if we are already authorized.
			if (!isAuthorized())
                return invokeMethod(method, parameters);
            else
                return ExternalApiResult.SUCCESS_CONTINUE;
		}
        else
        {
            // All other methods need authorization first, if we don't already have it.
			if (isAuthorized())
            {
				return invokeMethod(method, parameters);
			}
            else
            {
				mPendingOperation = new Pair<String, List<Object>>(method, parameters);
				return invokeMethod(METHOD_LOGIN);
			}
		}
	}

	@SuppressWarnings("FieldCanBeLocal")
	private final IMethodInvoker mLoginMethod = new IMethodInvoker() {

		@Override
		public @NonNull ExternalApiResult invoke(List<Object> parameters) {
			Intent intent = new Intent(getActivity(), FacebookLoginActivity.class);
			getActivity().startActivityForResult(intent, RequestCodes.ACTION_ALWAYS_SUCCESSFUL);
			return ExternalApiResult.SUCCESS_WAIT;
		}
	};

	@SuppressWarnings("FieldCanBeLocal")
	private final IMethodInvoker mPostToWallMethod = new IMethodInvoker() {

		@Override
		public @NonNull ExternalApiResult invoke(List<Object> parameters) {
			final String name = (String) parameters.get(0);
			final String caption = (String) parameters.get(1);
			final String description = (String) parameters.get(2);
			final String link = (String) parameters.get(3);
			final String picture = (String) parameters.get(4);

			if (TextUtils.isEmpty(link)) {
				Services.Log.debug("Link parameter was empty and is required.");
				return ExternalApiResult.SUCCESS_CONTINUE;
			}

			if (FacebookDialog.canPresentShareDialog(getContext().getApplicationContext())) {
				presentNativeShareDialog(name, caption, description, link, picture);
			} else {
				presentWebShareDialog(name, caption, description, link, picture);
			}

			return ExternalApiResult.SUCCESS_WAIT;
		}
	};

	@Override
	public ExternalApiResult afterActivityResult(int requestCode, int resultCode, Intent result, String method) {
		if (mUiHelper != null) {
			mUiHelper.onActivityResult(requestCode, resultCode, result, mFacebookDialogCallback);

		}
		if (METHOD_LOGIN.equalsIgnoreCase(method)) {
			String accessToken = (resultCode == Activity.RESULT_OK) ? result.getStringExtra(FacebookLoginActivity.EXTRA_FACEBOOK_ACCESS_TOKEN) : null;
			if (accessToken == null) {
				ActionExecution.cancelCurrent();
				return null;
			} else {
				return new ExternalApiResult(ActionResult.SUCCESS_CONTINUE_NO_REFRESH, accessToken);
			}
		} else {
			if (isAuthorized()) {
				if (mPendingOperation != null) {
					new Thread(new ExecutePendingOperation()).start();
					return new ExternalApiResult(ActionResult.SUCCESS_WAIT, null);
				} else {
					return new ExternalApiResult(ActionResult.SUCCESS_CONTINUE_NO_REFRESH, null);
				}
			} else {
				ActionExecution.cancelCurrent();
				return null;
			}
		}
	}

	private class ExecutePendingOperation implements Runnable {

		@Override
		public void run() {
			String method = mPendingOperation.first;
			List<Object> parms = mPendingOperation.second;
			Object result = execute(method, parms);
			getAction().setOutputValue(result);
		}
	}

	private void presentWebShareDialog(final String name, final String caption, final String description,
			final String link, final String picture) {
		getActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Bundle params = new Bundle();
				params.putString("link", link);
				if (!TextUtils.isEmpty(name)) {
					params.putString("name", name);
				}
				if (!TextUtils.isEmpty(caption)) {
					params.putString("caption", caption);
				}
				if (!TextUtils.isEmpty(description)) {
					params.putString("description", description);
				}
				if (!TextUtils.isEmpty(picture)) {
					params.putString("picture", picture);
				}
				new WebDialog.FeedDialogBuilder(getActivity(), Session.getActiveSession(), params)
                        .setOnCompleteListener(mOnWebShareDialogComplete)
					.build()
					.show();
			}
		});
	}

	private final OnCompleteListener mOnWebShareDialogComplete = new OnCompleteListener() {

		@Override
		public void onComplete(Bundle values, FacebookException error) {
			if (error == null) {
				final String postId = values.getString("post_id");
				if (postId != null) {
					// Posted story successfully.
					ActionExecution.continueCurrent(getActivity(), true);
				} else {
					// User clicked the cancel button.
					ActionExecution.cancelCurrent();
				}
			} else if (error instanceof FacebookOperationCanceledException) {
				// User clicked the "x" button.
				ActionExecution.cancelCurrent();
			} else {
				// Generic error (e.g. network error).
				ActionExecution.cancelCurrent();
			}
		}
	};

	private void presentNativeShareDialog(final String name, final String caption, final String description, final String link,
			final String picture) {
		FacebookDialog.ShareDialogBuilder shareDialogBuilder = new FacebookDialog.ShareDialogBuilder(getActivity());
		shareDialogBuilder.setLink(link);
		if (!TextUtils.isEmpty(name)) {
			shareDialogBuilder.setName(name);
		}
		if (!TextUtils.isEmpty(caption)) {
			shareDialogBuilder.setCaption(caption);
		}
		if (!TextUtils.isEmpty(description)) {
			shareDialogBuilder.setDescription(description);
		}
		if (!TextUtils.isEmpty(picture)) {
			shareDialogBuilder.setPicture(picture);
		}
		FacebookDialog shareDialog = shareDialogBuilder.build();
		ActivityHelper.registerActionRequestCode(com.facebook.internal.NativeProtocol.DIALOG_REQUEST_CODE);
		mUiHelper = new UiLifecycleHelper(getActivity(), null);
		mUiHelper.trackPendingDialogCall(shareDialog.present());
	}

	private final Callback mFacebookDialogCallback = new Callback() {

		@Override
		public void onError(PendingCall pendingCall, Exception error, Bundle data) {
			ActionExecution.cancelCurrent();
			mUiHelper = null;
		}

		@Override
		public void onComplete(PendingCall pendingCall, Bundle data) {
			if (FacebookDialog.getNativeDialogDidComplete(data)
					&& !FacebookDialog.COMPLETION_GESTURE_CANCEL.equals(
							FacebookDialog.getNativeDialogCompletionGesture(data))) {
				ActionExecution.continueCurrent(getActivity(), true);
			} else {
				ActionExecution.cancelCurrent();
			}
			mUiHelper = null;
		}
	};

	private boolean isAuthorized() {
		Session session = Session.getActiveSession();
		return (session != null && session.isOpened());
	}
}
