package com.artech.android.api;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.support.v7.app.AlertDialog;
import android.text.Html;

import com.artech.R;
import com.artech.actions.ActionExecution;
import com.artech.actions.ApiAction;
import com.artech.activities.ActivityFlowControl;
import com.artech.activities.GenexusActivity;
import com.artech.application.MyApplication;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.artech.base.utils.ThreadUtils;
import com.artech.common.PhoneHelper;

public class SDActions
{
	private static Activity myActivity = null;

	//return action
	public static void returnAction(Activity activity)
	{
		ActivityFlowControl.finishWithReturn(activity);
	}

	//add contact action.
	public static boolean addContactFromParameters(Activity fromActivity, List<String> values)
	{
		String contactName = Strings.EMPTY;
		String secondName = Strings.EMPTY;
		String email = Strings.EMPTY;
		String phone = Strings.EMPTY;
		String companyName = Strings.EMPTY;

		if (values.size() > 0)
			contactName = values.get(0);
		if (values.size() > 1)
			secondName = values.get(1);
		if (values.size() > 2)
			email = values.get(2);
		if (values.size() > 3)
			phone = values.get(3);
		if (values.size() > 4)
			companyName = values.get(4);

		return PhoneHelper.addContact(fromActivity, contactName, secondName, phone, email, null, companyName);
	}

	//send message action.
	public static void sendMessageFromParameters(Activity fromActivity, List<String> values)
	{
		String data = Strings.EMPTY;
		if (values.size() > 0)
			data = values.get(0);

		String toMessage = Strings.EMPTY;
		if (values.size() > 1)
			toMessage = values.get(1);

		PhoneHelper.share(fromActivity, data, toMessage);
	}

	//add appointment action
	public static boolean addAppointmentFromParameters(Activity fromActivity, List<String> values)
	{
		String title = Strings.EMPTY;
		Date startDate = null;
		Date endDate = null;
		Date startDateTime = null;
		Date endDateTime = null;
		String place = Strings.EMPTY;

		if (values.size() > 0)
			title = values.get(0);
		if (values.size() > 1)
			startDate = Services.Strings.getDate(values.get(1));
		if (values.size() > 2)
			endDate = Services.Strings.getDate(values.get(2));
		if (values.size() > 3)
		{
			startDateTime = Services.Strings.getDateTime(values.get(3));
			if (startDateTime==null)
				startDateTime = startDate;
			else
				cloneDate(startDate, startDateTime);
		}
		if (values.size() > 4)
		{
			endDateTime = Services.Strings.getDateTime(values.get(4));
			if (endDateTime==null)
				endDateTime = endDate;
			else
				cloneDate(endDate, endDateTime);
		}
		if (values.size() > 5)
			place = values.get(5);
		if (startDateTime == null)
			startDateTime = startDate;
		if (endDateTime == null)
			endDateTime = endDate;

		return PhoneHelper.addAppointment(fromActivity, title, startDateTime, endDateTime, place);
	}

	private static void cloneDate(Date originalDate, Date toReplaceDate)
	{
		if (originalDate!=null)
		{
			Calendar calOriginal = Calendar.getInstance();
			calOriginal.setTime(originalDate);

			Calendar calReplace = Calendar.getInstance();
			calReplace.setTime(toReplaceDate);

			calReplace.set(Calendar.YEAR, calOriginal.get(Calendar.YEAR));
			calReplace.set(Calendar.MONTH, calOriginal.get(Calendar.MONTH));
			calReplace.set(Calendar.DAY_OF_MONTH, calOriginal.get(Calendar.DAY_OF_MONTH));
			calReplace.set(Calendar.SECOND, 0);
			toReplaceDate.setTime(calReplace.getTime().getTime());
		}
	}

	// Dialogs

	public static void showMessage(Activity activity, String message, boolean isToast)
	{
		int type = (isToast ? ShowMessageRunnable.TYPE_TOAST : ShowMessageRunnable.TYPE_MESSAGE);
		activity.runOnUiThread(new ShowMessageRunnable(activity, type, message));

		// Wait a short delay for toast to appear (not necessary, for better visual effect only).
		if (isToast)
			ThreadUtils.sleep(150);
	}

	public static void showConfirmDialog(ApiAction action, Activity activity, String message)
	{
		activity.runOnUiThread(new ConfirmRunnable(action, activity, message));
	}

	private static class ShowMessageRunnable implements Runnable
	{
		static final int TYPE_MESSAGE = 1;
		static final int TYPE_TOAST = 2;
		static final int TYPE_CONFIRM = 3;

		private final Activity mActivity;
		private final int mType;
		private final CharSequence mText;

		public ShowMessageRunnable(Activity activity, int type, String text)
		{
			mActivity = activity;
			mType = type;
			mText = Html.fromHtml(text);
		}

		@Override
		public void run()
		{
			if (mType == TYPE_CONFIRM || mType == TYPE_MESSAGE)
			{
				if (!mActivity.isFinishing()) {
					// Show dialog
					AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
					builder.setCancelable(false);
					builder.setMessage(mText);
					builder.setPositiveButton(R.string.GXM_button_ok, mDialogContinue);
					if (mType == TYPE_CONFIRM)
						builder.setNegativeButton(R.string.GXM_cancel, mDialogCancel);
					builder.show();
				}
			}
			else
			{
				// Show toast. It's not necessary to call continue, because this particular action
				// does not wait for completion (catchOnActivityResult = false).
				MyApplication.getInstance().showMessage(mText);
			}
		}

		private final DialogInterface.OnClickListener mDialogContinue = new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				onClickOk();
			}
		};

		private final DialogInterface.OnClickListener mDialogCancel = new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				onClickCancel();
			}
		};

		protected void onClickOk()
		{
			// By default, continue execution.
			continueEventExecution();
		}

		protected void onClickCancel()
		{
			// By default, cancel execution.
			cancelEventExecution();
		}

		protected final void continueEventExecution()
		{
			ActionExecution.continueCurrent(mActivity, false);
		}

		protected final void cancelEventExecution()
		{
			ActionExecution.cancelCurrent();
		}
	}

	private static class ConfirmRunnable extends ShowMessageRunnable
	{
		private final ApiAction mConfirmAction;

		public ConfirmRunnable(ApiAction action, Activity activity, String text)
		{
			super(activity, TYPE_CONFIRM, text);
			mConfirmAction = action;
		}

		@Override
		protected void onClickOk()
		{
			// Set result to True.
			if (mConfirmAction != null && mConfirmAction.hasOutput())
				mConfirmAction.setOutputValue(Strings.TRUE);

			// Continue execution.
			continueEventExecution();
		}

		@Override
		protected void onClickCancel()
		{
			if (mConfirmAction != null && mConfirmAction.hasOutput())
			{
				// Set result to False and continue (new behavior).
				mConfirmAction.setOutputValue(Strings.FALSE);
				continueEventExecution();
			}
			else
			{
				// Cancel event (old behavior)
				cancelEventExecution();
			}
		}
	}

	
	//Geolocation API, use Google Play Services API when possible 
	
	public static JSONObject getMyLocation(Activity activity, List<String> values, boolean showMessages)
	{
		myActivity = activity;

		// read parameters
		int minAccuracy = readInteger(values, 0, 0);
		int timeout = readInteger(values, 1, 0);
		mIncludeHeadingAndSpeed = readBoolean(values, 2);
		boolean ignoreErrors = readBoolean(values, 3);

		//create a fusedHelper if not created already
		LocationHelper.createFusedLocationHelper();
			
		if (!ignoreErrors)
		{
			// if location service are disabled return null, and composite block not continue.
			Object locationServiceEnabled = isLocationServiceEnabled();
			boolean locationServiceEnabledboolean = Boolean.valueOf(locationServiceEnabled.toString());
			if (!locationServiceEnabledboolean)
			{
				// show message with error.
				if (myActivity != null && showMessages)
					SDActions.showMessage(myActivity, Services.Strings.getResource(R.string.GXM_LocationServicesAreDisabled), true);

				return null;
			}
		}
		// don't request updates if already in tracking
		if (myActivity!=null && !LocationHelper.isTracking)
		{
			if (showMessages)
				myActivity.runOnUiThread(requestLocationUpdatesinUI);
			else
				myActivity.runOnUiThread(requestLocationUpdatesinUINoDialog);
		}

		JSONObject location = LocationHelper.getLocationJsonGeoLocationInfo(minAccuracy, timeout, mIncludeHeadingAndSpeed);

		// dont cancel request updates if already in tracking, would cancel it.
		if (myActivity != null && !LocationHelper.isTracking)
			myActivity.runOnUiThread(removeLocationUpdatesinUI);

		// return value is set to Empty and execution of the composite block continues without error.
		if (location == null && ignoreErrors)
			location = new JSONObject();

		Services.Log.info("getMyLocation", "End return location."); //$NON-NLS-1$ //$NON-NLS-2$
		return location;
	}

	private	static boolean mIncludeHeadingAndSpeed;

	private static ProgressDialog m_ProgressDialog = null;

	private static Runnable requestLocationUpdatesinUI = new Runnable(){
		@Override
		public void run(){
			requestLocationUpdates(true, false) ;
		}
	};

	private static Runnable requestLocationUpdatesinUINoDialog = new Runnable(){
		@Override
		public void run(){
			requestLocationUpdates(false, false) ;
		}
	};
	
	private static Runnable requestLocationUpdatesinUIForTracking = new Runnable(){
		@Override
		public void run(){
			requestLocationUpdates(false, true) ;
		}
	};


	private static void requestLocationUpdates(boolean showdialog, boolean applyTrakingParameters) {

		if (showdialog)
		{
			//wait for location
			m_ProgressDialog = new ProgressDialog(myActivity);
			//m_ProgressDialog.setTitle(Strings.EMPTY);
			m_ProgressDialog.setMessage(myActivity.getResources().getText(R.string.GXM_WaitingForLocation));
			// show only if possible
			if (myActivity instanceof GenexusActivity)
			{
				GenexusActivity gxActivity = (GenexusActivity) myActivity;
				// only show if activity is "active". fix crash with slide.start and app update
				if (gxActivity.isActive())
					m_ProgressDialog.show();
			}
			else
			{
				m_ProgressDialog.show();
			}
		}

		//request location update
		if (applyTrakingParameters)
		{
			if (LocationHelper.fusedHelper!=null)
				LocationHelper.requestLocationUpdatesTracking(mMinTime, mMinDistance, false, mAction, mActionInterval, mTrackingAccuracy);
			else
				LocationHelper.requestLocationUpdates(mMinTime, mMinDistance, false, mAction, mActionInterval);
		}
		else
		{
			if (LocationHelper.fusedHelper!=null)
				LocationHelper.requestLocationUpdates(LocationHelper.fusedHelper, 0);
			else 
				LocationHelper.requestLocationUpdates(0, 0, mIncludeHeadingAndSpeed, "", 0);
		}
	}

	private static Runnable removeLocationUpdatesinUI = new Runnable(){
		@Override
		public void run(){
			removeLocationUpdates() ;
		}
	};

	private static void removeLocationUpdates() {
		//remove request of location update
		if (LocationHelper.fusedHelper!=null)
			LocationHelper.removeLocationUpdates(LocationHelper.fusedHelper);
		
		LocationHelper.removeLocationUpdates();
		if (m_ProgressDialog!=null)
		{
			m_ProgressDialog.dismiss();
			// release progress dialog.
			m_ProgressDialog = null;
		}
	}

	//read getmylocation parameters
	private static boolean readBoolean(List<String> values, int arrayIndex)
	{
		boolean includeHeadingAndSpeed = false;
		if (values.size() > arrayIndex)
		{
			includeHeadingAndSpeed = Boolean.parseBoolean(values.get(arrayIndex));
		}
		return includeHeadingAndSpeed;
	}

	private static Integer readInteger(List<String> values, int arrayIndex, int defaultValue)
	{
		Integer timeout = defaultValue;
		if (values.size() > arrayIndex)
		{
			try{
				timeout = Integer.valueOf(values.get(arrayIndex)); }
			catch (NumberFormatException ex)
			{ /* return 0 as default */}
		}
		return timeout;
	}

	private static Date readDate(List<String> values, int arrayIndex)
	{
		Date resultDate = new Date(0);
		if (values.size() > arrayIndex)
		{
			try{
				resultDate = Services.Strings.getDate(values.get(arrayIndex));
			}
			catch (NumberFormatException ex)
			{ /* return minDate as default */}
		}
		return resultDate;
	}

	private static String readString(List<String> values, int arrayIndex) {
		String valueStr = Strings.EMPTY;
		if (values.size() > arrayIndex)
		{
			valueStr = values.get(arrayIndex);
		}
		return valueStr;
	}

	//getaddress
	public static JSONArray getAddressFromLocation(Activity activity, List<String> values)
	{
		//read parameter
		Location location = getLocationFromParameter(values, 0);

		//convert geolocation to address.
		List<Address> addresses = null;
		Geocoder geocoder = new Geocoder(activity, Locale.getDefault());
		try {
			addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 10);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		JSONArray arrayResult = new JSONArray();
		if (addresses != null && addresses.size() > 0)
		{
			for (int j= 0; j < addresses.size(); j++)
			{
				Address address = addresses.get(j);
				StringBuilder result = new StringBuilder();

				for (int i = 0; i < address.getMaxAddressLineIndex(); i++)
					result.append(address.getAddressLine(i)).append(Strings.NEWLINE);

				result.append(address.getLocality()).append(Strings.NEWLINE);
				if (address.getPostalCode()!=null)
					result.append(address.getPostalCode()).append(Strings.NEWLINE);
				result.append(address.getCountryName()).append(Strings.NEWLINE);

				arrayResult.put(result.toString());
			}
			// Services.Log.info("getLocationFromAddress", arrayResult.toString()); //$NON-NLS-1$
		}

		return arrayResult;
	}

	//getlocation
	public static JSONArray getLocationFromAddress(Activity activity, List<String> values)
	{
		//read Parameters
		String address = Strings.EMPTY;
		if (values.size() > 0)
		{
			address = values.get(0);
		}
		//convert geolocation to address.
		List<Address> locations = null;
		Geocoder geocoder = new Geocoder(activity, Locale.getDefault());
		try {
			locations = geocoder.getFromLocationName(address, 10);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		JSONArray arrayResult = new JSONArray();
		if (locations!=null && locations.size()>0)
		{
			for (int j =0; j< locations.size(); j++)
			{
				Address location = locations.get(j);
				arrayResult.put(location.getLatitude() + Strings.COMMA + location.getLongitude());
			}
			// Services.Log.info("getLocationFromAddress", arrayResult.toString()); //$NON-NLS-1$
		}

		return arrayResult;
	}

	//getlatitude
	public static String getLatitudeFromLocation(List<String> values)
	{
		Location location = getLocationFromParameter(values, 0);
		//Services.Log.info("getLatitudeFromLocation", String.valueOf(location.getLatitude())); //$NON-NLS-1$
		return String.valueOf(location.getLatitude());
	}

	//getlongitude
	public static String getLongitudeFromLocation(List<String> values)
	{
		Location location = getLocationFromParameter(values, 0);
		//Services.Log.info("getLongitudeFromLocation", String.valueOf(location.getLongitude())); //$NON-NLS-1$
		return String.valueOf(location.getLongitude());
	}

	//getdistance
	public static String getDistanceFromLocations(List<String> values)
	{
		//read parameter
		Location location = getLocationFromParameter(values, 0);
		Location location2 = getLocationFromParameter(values, 1);

		float distance = location.distanceTo(location2);
		// Services.Log.info("getDistanceFromLocations", String.valueOf(distance)); //$NON-NLS-1$

		return String.valueOf(distance);
	}

	private static Integer mMinTime = 0;
	private static Integer mMinDistance = 0;
	private static String mAction = "";
	private static Integer mActionInterval = 0;
	private static Integer mTrackingAccuracy = 20; //default for compatibility

	//startTraking
	public static boolean startTracking(Activity activity, List<String> values)
	{
		myActivity = activity;
		
		//create a fusedHelper if not created already
		LocationHelper.createFusedLocationHelper();
						
		//read parameters
		mMinTime = readInteger(values, 0, 0) * 1000; //convert seconds to miliseconds
		mMinDistance = readInteger(values, 1, 0);
		
		mAction = readString(values, 2);
		mActionInterval = readInteger(values, 3, 0) ; //in seconds!
		
		mTrackingAccuracy = readInteger(values, 4, 20);  //default 20 for compatibility
		
		Services.Log.info("startTracking " + " minTime " + String.valueOf(mMinTime) + " minDistance " + String.valueOf(mMinDistance) ); //$NON-NLS-1$
		Services.Log.info("startTracking " + " Action " + String.valueOf(mAction) + " ActionInterval " + String.valueOf(mActionInterval) ); //$NON-NLS-1$
		Services.Log.info("startTracking " + " TrackingAccuracy " + String.valueOf(mTrackingAccuracy) ); //$NON-NLS-1$
		
		myActivity.runOnUiThread(requestLocationUpdatesinUIForTracking);
		LocationHelper.isTracking = true;
		
		//LocationHelper.locationsArray.add(LocationHelper.getLastKnownLocation());

		return true;
	}

	

	//startTraking
	public static boolean endTracking(Activity activity)
	{
		myActivity = activity;
		//read parameters

		myActivity.runOnUiThread(removeLocationUpdatesinUI);
		LocationHelper.isTracking = false;
		
		return true;
	}

	public static boolean clearLocationHistory()
	{
		LocationHelper.clearLocationHistory();
		return true;
	}

	public static JSONArray getLocationHistory(List<String> parameterValues)
	{
		// return json array of location in array of history
		Date startDate = readDate(parameterValues, 0);

		return LocationHelper.getLocationHistory(startDate);
	}


	public static Object isLocationServiceEnabled() {
		// TODO Auto-generated method stub
		return LocationHelper.isLocationServiceEnabled();
	}

	//helpers
	private static Location getLocationFromParameter(List<String> values, int position)
	{
		//read Parameters
		double latitude = 0;
		double longitude = 0;
		if (values.size() > position)
		{
			String[] valuesAddress = Services.Strings.split(values.get(position), ',');
			try{
				if (valuesAddress.length > 0)
					latitude = Float.parseFloat(valuesAddress[0]);
				if (valuesAddress.length > 1)
					longitude = Float.parseFloat(valuesAddress[1]);
			}
			catch (NumberFormatException ex)
			{ /* return 0 as default */
			}
		}
		Location location = new Location("POINT_LOCATION"); //$NON-NLS-1$
		location.setLatitude(latitude);
		location.setLongitude(longitude);
		return location;
	}
}
