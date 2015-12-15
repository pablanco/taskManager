package com.artech.android.controls;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Base64;
import android.util.Log;
import android.util.Xml;
import android.view.View;

import com.artech.android.ActivityResourceBase;
import com.artech.android.ActivityResources;
import com.artech.base.metadata.layout.ControlInfo;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.utils.Strings;
import com.artech.controls.IGxEdit;
import com.artech.ui.Coordinator;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.artech.android.facebookapi.R;
import com.facebook.model.GraphUser;
import com.facebook.widget.LoginButton;

public class GXFacebookButton extends LoginButton implements IGxEdit {

	private UiLifecycleHelper mUIHelper;
	private JSONObject mUserInformation = null;
	private Coordinator mCoordinator;

	private static final String USER_LOGGEDIN_EVENT = "OnUserLoggedIn";
	private static final String USER_LOGGEDOUT_EVENT = "OnUserLoggedOut";
	private static final String USER_INFOUPDATED_EVENT = "OnUserInfoUpdated";
	private static final String USER_ERROR_EVENT = "OnError";

	public GXFacebookButton(Context context, Coordinator coordinator, LayoutItemDefinition def) {
		super(context, getAttributeSet(coordinator));
		onFinishInflate();
		mCoordinator = coordinator;
		initialize(context, def);
	}

	private static AttributeSet getAttributeSet(Coordinator coordinator) {
		XmlResourceParser parser = coordinator.getUIContext().getActivity().getResources().getXml(R.layout.com_artech_sdfacebookbutton);
		return Xml.asAttributeSet(parser);
	}

	public static void showHashKey(Context context, String packageName) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(
                    packageName, PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.i("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
                }
        } catch (NameNotFoundException e) {
        } catch (NoSuchAlgorithmException e) {
        }
    }

	public GXFacebookButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialize(context, null);
	}

	public class LifeCycleCallBack extends ActivityResourceBase
	{
		@Override
		public void onResume(Activity activity) {
			mUIHelper.onResume();

			// For scenarios where the main activity is launched and user
		    // session is not null, the session state change notification
		    // may not be triggered. Trigger it if it's open/closed.
		    Session session = Session.getActiveSession();
		    if (session != null &&
		           (session.isOpened() || session.isClosed()) ) {
		        onSessionStateChange(session, session.getState(), null);
		    }
		}

		@Override
		public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
			mUIHelper.onActivityResult(requestCode, resultCode, data);
		}

		@Override
		public void onSaveInstanceState(Activity activity, Bundle outState) {
			mUIHelper.onSaveInstanceState(outState);
		}

		@Override
		public void onPause(Activity activity) {
			mUIHelper.onPause();
		}

		@Override
		public void onDestroy(Activity activity) {
			mUIHelper.onDestroy();
		}

		@Override
		public void onCreate(Activity activity, Bundle savedInstanceState) {
		    mUIHelper = new UiLifecycleHelper(activity, callback);
		    mUIHelper.onCreate(savedInstanceState);
		}

	}

	private void initialize(Context context, LayoutItemDefinition def)
	{
		if (def != null) {
			ControlInfo info = def.getControlInfo();
			if (info != null) {
				String permissions = info.optStringProperty("@SDFacebookButtonReadPermissions");
				if (permissions.length() == 0)
					permissions = "public_profile,email,user_friends";
				String[] arrPermissions = permissions.split(",");
				setReadPermissions(Arrays.asList(arrPermissions));

				boolean publishAllowed = info.optBooleanProperty("@SDFacebookButtonPublishAllowed");
				if (publishAllowed)
					setPublishPermissions(Collections.singletonList("publish_actions"));
			}
		}
		ActivityResources.setResource(mCoordinator.getUIContext().getActivity(), LifeCycleCallBack.class, new LifeCycleCallBack());
		final View view = this;
		setUserInfoChangedCallback(new LoginButton.UserInfoChangedCallback() {
            @Override
            public void onUserInfoFetched(GraphUser user) {
            	try {
            		if (user != null) {
	                	mUserInformation = new JSONObject();
	            		mUserInformation.put("id", user.getId());
	            		mUserInformation.put("name", user.getName());
	            		mUserInformation.put("first_name", user.getFirstName());
	            		mUserInformation.put("middle_name", user.getMiddleName());
	            		mUserInformation.put("last_name", user.getLastName());
	            		mUserInformation.put("birthday", user.getBirthday());
	            		if (user.getLocation() != null && user.getLocation().getLocation() != null) {
	            			mUserInformation.put("location_city", user.getLocation().getLocation().getCity());
	               			mUserInformation.put("location_country", user.getLocation().getLocation().getCountry());
	               			mUserInformation.put("location_city", user.getLocation().getLocation().getCity());
	               			mUserInformation.put("location_latitude", user.getLocation().getLocation().getLatitude());
	               			mUserInformation.put("location_longitude", user.getLocation().getLocation().getLongitude());
	            		}
	            		if (user.getProperty("email") != null)
	            			mUserInformation.put("email", user.getProperty("email").toString());
	            		mUserInformation.put("link", user.getLink());
	                  	mCoordinator.runControlEvent(view, USER_INFOUPDATED_EVENT);
            		}

				} catch (JSONException e) {
					mUserInformation = null;
				}
            }
        });


	}

	private final Session.StatusCallback callback = new Session.StatusCallback() {
	    @Override
	    public void call(Session session, SessionState state, Exception exception) {
	        onSessionStateChange(session, state, exception);
	    }
	};
	private String mTag;

	private void onSessionStateChange(Session session, SessionState state, Exception exception) {
	    if (exception != null) {
	    	mCoordinator.runControlEvent(this, USER_ERROR_EVENT);
	    } else {
			if (state.isOpened()) {
		    	mCoordinator.runControlEvent(this, USER_LOGGEDIN_EVENT);
		    } else if (state.isClosed()) {
		    	mCoordinator.runControlEvent(this, USER_LOGGEDOUT_EVENT);
		    }
	    }
	}

	@Override
	public String getGx_Value() {
		if (mUserInformation == null)
			return Strings.EMPTY;
		return mUserInformation.toString();
	}

	@Override
	public void setGx_Value(String value) {
	}

	@Override
	public String getGx_Tag() {
		return mTag;
	}

	@Override
	public void setGx_Tag(String tag) {
		mTag = tag;
	}

	@Override
	public void setValueFromIntent(Intent data) {
	}

	@Override
	public boolean isEditable() {
		return true; // Because it changes its associated data.
	}

	@Override
	public IGxEdit getViewControl() {
		return this;
	}

	@Override
	public IGxEdit getEditControl() {
		return this;
	}
}
