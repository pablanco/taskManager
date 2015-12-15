package com.artech.common;

import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.MailTo;
import android.net.Uri;
import android.provider.ContactsContract;

import com.artech.activities.ActivityLauncher;
import com.artech.application.MyApplication;
import com.artech.base.metadata.enums.ActionTypes;
import com.artech.base.metadata.enums.RequestCodes;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;

public class PhoneHelper
{
	public enum Capability
	{
		CallNumber,
		SendSms,
		SendEmail,
		LocateAddress,
		LocateGeolocation,
		AddContact,
		AddAppointment
	}

	// ********************************************************************************
	// Intents for the different device features.
	// ********************************************************************************

	private static Intent getShareIntent(String data, String to)
	{
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("text/plain"); //$NON-NLS-1$
		intent.putExtra(Intent.EXTRA_TEXT, data);
		intent.putExtra(Intent.EXTRA_EMAIL, new String[]{to});

		return intent;
	}

	private static Intent getSendEmailIntent(String email)
	{
		Uri uri = Uri.parse("mailto:" + email); //$NON-NLS-1$
		Intent emailIntent = new Intent(Intent.ACTION_SENDTO, uri);
		ActivityLauncher.setIntentFlagsNewDocument(emailIntent);
		return emailIntent;
	}

	private static Intent getSendEmailIntent(String email, String subject, String message)
	{
		Uri uri = Uri.parse("mailto:" + email); //$NON-NLS-1$
		Intent emailIntent = new Intent(Intent.ACTION_SENDTO, uri);
		ActivityLauncher.setIntentFlagsNewDocument(emailIntent);
		emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
		emailIntent.putExtra(Intent.EXTRA_TEXT, message);
		return emailIntent;
	}

	private static Intent getSendEmailIntent(String[] email, String[] ccEmail, String[] bccEmail, String subject, String message)
	{
		Uri uri = Uri.parse("mailto:" ); //$NON-NLS-1$
		Intent emailIntent = new Intent(Intent.ACTION_SENDTO, uri);
		ActivityLauncher.setIntentFlagsNewDocument(emailIntent);
		emailIntent.putExtra(Intent.EXTRA_EMAIL, email);
		emailIntent.putExtra(Intent.EXTRA_CC, ccEmail);
		emailIntent.putExtra(Intent.EXTRA_BCC, bccEmail);
		emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
		emailIntent.putExtra(Intent.EXTRA_TEXT, message);
		return emailIntent;
	}

	private static Intent getSendSmsIntent(String phone, String message)
	{
		Uri uri = Uri.parse("smsto:" + phone); //$NON-NLS-1$
		Intent intent = new Intent(Intent.ACTION_SENDTO, uri);
		ActivityLauncher.setIntentFlagsNewDocument(intent);
		intent.putExtra("sms_body", message);   //$NON-NLS-1$
		return intent;
	}

	private static Intent getLocateAddressIntent(String address)
	{
		Uri uri = Uri.parse("geo:0,0?q=" + address); //$NON-NLS-1$
		Intent addressIntent = new Intent(Intent.ACTION_VIEW, uri);
		ActivityLauncher.setIntentFlagsNewDocument(addressIntent);
		return addressIntent;
	}

	private static Intent getLocateGeoLocationIntent(String geolocation)
	{
		//This only center the map in this location, not show a pin
		//Uri uri = Uri.parse( "geo:" + geolocation  );
		
		//Center and show a pin with geolocation string.
		// the document way is not working as 05/2015 , see
		// https://developer.android.com/guide/components/intents-common.html#Maps
		// https://code.google.com/p/gmaps-api-issues/issues/detail?id=8050
		// for now avoid this method and only center in the location
		// WA , add an space and not use Label see
		// https://code.google.com/p/gmaps-api-issues/issues/detail?id=8050
		String query = geolocation;
		if (geolocation.length()>0)
		{
			//query = geolocation + "(" + geolocation + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			query = geolocation.replace("-", "- "); //$NON-NLS-1$ //$NON-NLS-2$
		}
		String allQueryUri = "geo:0,0?q=" + query; //$NON-NLS-1$
		Uri uri = Uri.parse( allQueryUri); //$NON-NLS-1$
				
		Intent addressIntent = new Intent(Intent.ACTION_VIEW, uri);
		ActivityLauncher.setIntentFlagsNewDocument(addressIntent);
		return addressIntent;
	}

	private static Intent getCallNumberIntent(String number)
	{
		Uri uri = Uri.parse("tel:" + number); //$NON-NLS-1$
		Intent callNumberIntent = new Intent(Intent.ACTION_DIAL, uri);
		ActivityLauncher.setIntentFlagsNewDocument(callNumberIntent);
		return callNumberIntent;
	}

	private static Intent getAddContactIntent(String firstName, String lastName, String number, String email, byte[] photo, String company)
	{
		String fullName;
		if (Services.Strings.hasValue(firstName) && Services.Strings.hasValue(lastName))
			fullName = firstName + Strings.SPACE + lastName;
		else
			fullName = firstName;

		Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
		intent.setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE);
		ActivityLauncher.setIntentFlagsNewDocument(intent);

		// In order to properly set first and last name, we must use a ContentProvider;
		// they cannot be set via Intent. See http://androidcookbook.com/Recipe.seam?recipeId=334
		intent.putExtra(ContactsContract.Intents.Insert.NAME, fullName);

		if (photo != null)
			intent.putExtra(ContactsContract.CommonDataKinds.Photo.PHOTO, photo);

		intent.putExtra(ContactsContract.Intents.Insert.EMAIL, email);
		intent.putExtra(ContactsContract.Intents.Insert.PHONE, number);
		intent.putExtra(ContactsContract.Intents.Insert.COMPANY, company);

		return intent;
	}

	private static Intent getAddAppointmentIntent(String title, Date beginTime, Date endTime, String place)
	{
		Intent intent = new Intent(Intent.ACTION_EDIT);
		intent.setType("vnd.android.cursor.item/event");  //$NON-NLS-1$
		ActivityLauncher.setIntentFlagsNewDocument(intent);

		if (beginTime != null)
			intent.putExtra("beginTime", beginTime.getTime()); //$NON-NLS-1$

		if (endTime != null)
			intent.putExtra("endTime", endTime.getTime()); //$NON-NLS-1$

		intent.putExtra("title", title); //$NON-NLS-1$
		intent.putExtra("eventLocation", place); //$NON-NLS-1$
		return intent;
	}

	// ********************************************************************************
	// Actions associated to a "semantic domain" (e.g. pressing a "Phone" variable.
	// These launch phone activities, but in a standalone way (they do not have
	// continuations and therefore do not need startActivityForResult().
	// ********************************************************************************

	public static void launchDomainAction(Context context, String type, String value)
	{
		if (type.equals(ActionTypes.CallNumber))
			launchCallNumber(context, value);
		else if (type.equals(ActionTypes.SendEmail))
			launchSendEmail(context, value);
		else if (type.equals(ActionTypes.ViewVideo))
			launchviewVideo(context, value);
		else if (type.equals(ActionTypes.ViewAudio))
			launchViewAudio(context, value);
		else if (type.equals(ActionTypes.ViewUrl))
			launchViewUrl(context, value);
		else if (type.equals(ActionTypes.LocateAddress))
			launchLocateAddress(context, value);
		else if (type.equals(ActionTypes.LocateGeoLocation))
			launchLocateGeoLocation(context, value);
	}

	public static boolean isDomainActionSupported(String type)
	{
		if (type.equals(ActionTypes.CallNumber))
			return isCapabilitySupported(Capability.CallNumber);
		else if (type.equals(ActionTypes.SendEmail))
			return isCapabilitySupported(Capability.SendEmail);
		else if (type.equals(ActionTypes.ViewVideo) || type.equals(ActionTypes.ViewAudio) || type.equals(ActionTypes.ViewUrl))
			return true; // Handled by our own activities, nothing to check.
		else if (type.equals(ActionTypes.LocateAddress))
			return isCapabilitySupported(Capability.LocateAddress);
		else if (type.equals(ActionTypes.LocateGeoLocation))
			return isCapabilitySupported(Capability.LocateGeolocation);

		Services.Log.warning(String.format("Unknown domain action: '%s'.", type));
		return false;
	}

	@SuppressWarnings("WeakerAccess")
	public static boolean isCapabilitySupported(Capability capability)
	{
		switch (capability)
		{
			case CallNumber : return isIntentSupported(getCallNumberIntent("555-5555"));
			case SendSms : return isIntentSupported(getSendSmsIntent("555-5555", "Example"));
			case SendEmail : return isIntentSupported(getSendEmailIntent("example@example.com"));
			case LocateAddress : return isIntentSupported(getLocateAddressIntent("X"));
			case LocateGeolocation : return isIntentSupported(getLocateGeoLocationIntent("0,0"));
			case AddContact : return isIntentSupported(getAddContactIntent("Example", "Example", "", "", null, ""));
			case AddAppointment : return isIntentSupported(getAddAppointmentIntent("Example", new Date(), new Date(), ""));

			default :
				Services.Log.warning(String.format("Unknown capability: '%s'.", capability));
				return false;
		}
	}

	public static boolean launchFromWebView(Context context, String url)
	{
		Uri uri = Uri.parse(url);

		String scheme = uri.getScheme();
		if (!Strings.hasValue(scheme))
			return false;

		if (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
			return false; // Let the caller WebView handle this url itself.

		if (MailTo.isMailTo(url))
		{
			MailTo mailTo = MailTo.parse(url);
			launchSendEmail(context, mailTo.getTo(), mailTo.getSubject(), mailTo.getBody());
			return true;
		}
		else // if (scheme.equalsIgnoreCase("tel") || scheme.equalsIgnoreCase("sms") || scheme.equalsIgnoreCase("geo"))
		{
			Intent intent = new Intent(Intent.ACTION_VIEW, uri);
			ActivityLauncher.setIntentFlagsNewDocument(intent);
			return startActivity(context, intent);
		}
	}

	private static void launchCallNumber(Context context, String number)
	{
		startActivity(context, getCallNumberIntent(number));
	}

	private static void launchSendEmail(Context context, String email)
	{
		startActivity(context, getSendEmailIntent(email));
	}

	private static void launchSendEmail(Context context, String email, String subject, String body)
	{
		startActivity(context, getSendEmailIntent(email, subject, body));
	}

	private static void launchviewVideo(Context context, String link)
	{
		if (Strings.hasValue(link))
			ActivityLauncher.CallViewVideo(context, link);
	}

	private static void launchViewAudio(Context context, String link)
	{
		if (Strings.hasValue(link))
			ActivityLauncher.CallViewAudio(context, link);
	}

	private static void launchViewUrl(Context context, String link)
	{
		if (Strings.hasValue(link))
			ActivityLauncher.CallComponent(context, link);
	}

	private static void launchLocateAddress(Context context, String address)
	{
		startActivity(context, getLocateAddressIntent(address));
	}

	private static void launchLocateGeoLocation(Context context, String geolocation)
	{
		startActivity(context, getLocateGeoLocationIntent(geolocation));
	}

	// ********************************************************************************
	// Actions associated to an API call (e.g Interop.SendSms).
	// These launch phone activities, with requestCode = ACTION, so that composite
	// execution continues when they finish.
	// ********************************************************************************

	public static void share(Activity fromActivity, String data, String to)
	{
		// "Share" uses a chooser. Most likely the user shares to different apps,
		// and it makes no sense to force him into the default one.
		startActionWithChooser(fromActivity, getShareIntent(data, to), 0);
	}

	public static boolean callNumber(Activity fromActivity, String number)
	{
		// The "call number" intent starts a different task, and therefore the call
		// startActivityForResult() fails immediately and causes a weird
		// resume -> result -> pause -> resume sequence. This causes a number of problems with
		// actions continuations, so do not supporr them after this api call.
		return startAction(fromActivity, getCallNumberIntent(number), RequestCodes.ACTION);
	}

	public static boolean sendEmail(Activity fromActivity, String email, String subject, String message)
	{
		return startAction(fromActivity, getSendEmailIntent(email, subject, message));
	}

	public static boolean sendEmail(Activity fromActivity, String[] email, String[] ccEmail, String[] bccEmail, String subject, String message)
	{
		return startAction(fromActivity, getSendEmailIntent(email, ccEmail, bccEmail, subject, message));
	}

	public static boolean sendSms(Activity fromActivity, String email, String message)
	{
		return startAction(fromActivity, getSendSmsIntent(email, message));
	}

	public static boolean addContact(Activity fromActivity, String name, String secondName, String number, String email, byte[] photo, String company)
	{
		return startAction(fromActivity, getAddContactIntent(name,secondName, number, email, photo, company));
	}

	public static boolean addAppointment(Activity fromActivity, String title, Date beginTime, Date endTime, String place)
	{
		return startAction(fromActivity, getAddAppointmentIntent(title, beginTime, endTime, place));
	}

	// ********************************************************************************
	// Helper functions for IntentChooser, startActivity and startActivityForResult.
	// ********************************************************************************

	private static boolean startActivity(Context context, Intent intent)
	{
		// If no one can handle this intent, calling startActivity() will crash. So check it first.
		if (isIntentSupported(intent))
		{
			context.startActivity(intent);
			return true;
		}
		else
			return false;
	}

	/**
	 * Launch the intent, without presenting a chooser if a default application is set.
	 * Should be used when it's "natural" to use the default application (e.g. send sms, email)
	 * but not when it's "natural" to expect a choice (e.g. share).
	 * See http://developer.android.com/reference/android/content/Intent.html#ACTION_CHOOSER
	 */
	public static boolean startAction(Activity fromActivity, Intent intent)
	{
		// When calling a "native" activity, we almost always return with the back button
		// so result code is "CANCELED". Therefore starts these intents with ALWAYS_SUCCESSFUL
		// to catch them later in onActivityResult().
		return startAction(fromActivity, intent, RequestCodes.ACTION_ALWAYS_SUCCESSFUL);
	}

	private static boolean startAction(Activity fromActivity, Intent intent, int requestCode)
	{
		// If no one can handle this intent, calling startActivity() will crash. So check it first.
		if (isIntentSupported(intent))
		{
			fromActivity.startActivityForResult(intent, requestCode);
			return true;
		}
		else
			return false;
	}

	/**
	 * Launch the intent, presenting a chooser even if a default application is set.
	 * Should be used when it's "natural" to expect a choice of target application
	 * (e.g. each share action could be to a different app, such as email or whatsapp).
	 * See http://developer.android.com/reference/android/content/Intent.html#ACTION_CHOOSER
	 */
	private static void startActionWithChooser(Activity fromActivity, Intent intent, int chooserTitleResId)
	{
		Intent chooserIntent = getChooserIntent(intent, chooserTitleResId);

		// When calling a "native" activity, we almost always return with the back button
		// so result code is "CANCELED". Therefore starts these intents with ALWAYS_SUCCESSFUL
		// to catch them later in onActivityResult().
		fromActivity.startActivityForResult(chooserIntent, RequestCodes.ACTION_ALWAYS_SUCCESSFUL);
	}

	private static Intent getChooserIntent(Intent target, int titleResId)
	{
		CharSequence title = null;
		if (titleResId > 0)
			title = Services.Strings.getResource(titleResId);

		return Intent.createChooser(target, title);
	}

	/**
	 * Checks whether there is an activity registered that can handle the given intent.
	 */
	private static boolean isIntentSupported(Intent intent)
	{
		Context context = MyApplication.getAppContext();
		List<ResolveInfo> apps = context.getPackageManager().queryIntentActivities(intent, 0);
		return (apps.size() != 0);
	}
}
