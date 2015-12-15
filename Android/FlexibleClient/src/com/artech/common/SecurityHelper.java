package com.artech.common;

import java.util.ArrayList;

import org.json.JSONObject;

import android.net.Uri;

import com.artech.actions.UIContext;
import com.artech.activities.ActivityLauncher;
import com.artech.android.gam.GAMHelper;
import com.artech.application.MyApplication;
import com.artech.base.metadata.IViewDefinition;
import com.artech.base.services.ServiceResponse;
import com.artech.base.services.Services;
import com.artech.base.utils.ResultDetail;
import com.artech.base.utils.Strings;
import com.artech.providers.EntityDataProvider;

public class SecurityHelper
{
	private final static String FIELD_USER_ID = "user_guid"; //$NON-NLS-1$
	private final static String FIELD_ACCESS_TOKEN = "access_token"; //$NON-NLS-1$
	private final static String FIELD_REFRESH_TOKEN = "refresh_token"; //$NON-NLS-1$
	private final static String FLAG_IS_ANONYMOUS = "is_anonymous_user"; //$NON-NLS-1$
	private final static String FLAG_DISABLE_ANONYMOUS = "disable_anonymous_login"; //$NON-NLS-1$

	public final static String TYPE_STANDARD = "password"; //$NON-NLS-1$
	private final static String TYPE_RENEW = "refresh_token"; //$NON-NLS-1$
	private final static String TYPE_ANONYMOUS = "device"; //$NON-NLS-1$

	/**
	 * If the caller needs the user to be logged in, and we have no current login, show the login screen.
	 * @param from Caller activity.
	 * @param definition View definition (normally a panel or dashboard).
	 * @return True if the user was sent to the login screen; otherwise false.
	 */
	public static boolean callLoginIfNecessary(UIContext from, IViewDefinition definition)
	{
		boolean needsLogin = MyApplication.getApp().isSecure();
		if (definition != null)
			needsLogin = definition.isSecure();

		if (needsLogin && !Services.Strings.hasValue(ServiceHelper.getToken()))
		{
			// We don't have a token, but we may be able to reuse the previous one, try that first.
			if (restoreLoginInformation())
				return false; // Token recovered, login not necessary.

			// If the server supports anonymous logins, the connection-failed catcher will perform it, so return false here.
			if (MyApplication.getApp().getEnableAnonymousUser())
				return false;

			// Go to login.
			ActivityLauncher.callLogin(from);
			return true;
		}
		else
			return false; // Token already set or no security needed.
	}

	public static boolean restoreLoginInformation()
	{
		boolean needsLogin = MyApplication.getApp().isSecure();
		if (needsLogin && !Services.Strings.hasValue(ServiceHelper.getToken()))
		{
			// We don't have a token, but we may be able to reuse the previous one, try that first.
			String accessToken = MyApplication.getInstance().getStringPreference(FIELD_ACCESS_TOKEN);
			if (Services.Strings.hasValue(accessToken))
			{
				ServiceHelper.setToken(accessToken);
				GAMHelper.restoreUserData();
				return true; // Token recovered!
			}
		}

		return false;
	}

	public enum Handled
	{
		/** There was no error, or there was but we didn't do anything special about it. */
		NOT_HANDLED,
		/** Called another activity, such as login or the not authorized panel. */
		CALLED_ACTIVITY,
		/** Finished the caller activity. */
		FINISHED_ACTIVITY
	}

	public static class Token
	{
		private boolean PreviousAuthorizationError;
	}

	/**
	 * Handles status codes for security errors (not authenticated/not authorized) after a server call.
	 * <ul>
	 *   <li>For <b>"NOT AUTHENTICATED"</b>, shows the login screen.</li>
	 *   <li>For <b>"NOT AUTHORIZED"</b>, shows the "Not authorized object" if set, otherwise it does nothing.</li>
 	 * </ul>
	 * @param from Caller activity.
	 * @param statusCode Status code returned by the server.
	 * @param statusMessage Message returned by the server.
	 * @param token Object to accumulate information on repeated calls to this method for the same data source.
	 * @return True if the user was sent to another screen; otherwise false.
	 */
	public static Handled handleSecurityError(UIContext from, int statusCode, String statusMessage, Token token)
	{
		if (token == null)
			token = new Token(); // If the caller did not supply a token it's not interested in "previous authorization error".

		if (statusCode == DataRequest.ERROR_SECURITY_AUTHENTICATION)
		{
			// Go to login.
			ActivityLauncher.callLogin(from);
			return Handled.CALLED_ACTIVITY;
		}
		else if (statusCode == DataRequest.ERROR_SECURITY_AUTHORIZATION)
		{
			// Call the not authorized panel, if defined.
			String notAuthorizedPanel = MyApplication.getApp().getNotAuthorizedObject();
			if (Services.Strings.hasValue(notAuthorizedPanel))
			{
				if (Services.Strings.hasValue(statusMessage))
					MyApplication.getInstance().showMessage(statusMessage);

				// If this is a repeat error, finish the activity instead. That way we prevent a
				// "not authorized -> back -> not authorized -> back" looping.
				if (!token.PreviousAuthorizationError)
				{
					if (ActivityLauncher.call(from, notAuthorizedPanel))
					{
						token.PreviousAuthorizationError = true;
						return Handled.CALLED_ACTIVITY;
					}
				}
				else
				{
					from.getActivity().finish();
					return Handled.FINISHED_ACTIVITY;
				}
			}
		}

		return Handled.NOT_HANDLED;
	}

	private static ThreadLocal<Boolean> sInsideAutomaticLogin = new ThreadLocal<Boolean>();

	/**
	 * Should be called (on a background thread) whenever a connection to the server fails due
	 * to an AUTHENTICATION error. If the access token is expired and we have a refresh token then
	 * it will try a refresh. If refresh is not possible (or fails) but we have anonymous logins
	 * enabled, try anonymous login. If both fail, returns false.
	 *
	 * @return True if automatic login was successful, otherwise false.
	 */
	public static boolean tryAutomaticLogin()
	{
		// In case of a server bug, an authentication attempt may return 401.
		// This would cause an infinite recursion (and a stack overflow) if we continually retry.
		Boolean isRecursive = sInsideAutomaticLogin.get();
		if (isRecursive != null && isRecursive.booleanValue())
			return false;

		sInsideAutomaticLogin.set(true);
		try
		{
			if (tryRenewLogin())
				return true;

			// Anonymous login is disabled if last login was a 'full' one.
			if (!MyApplication.getInstance().getBooleanPreference(FLAG_DISABLE_ANONYMOUS, false))
			{
				if (tryAnonymousLogin())
					return true;
			}

			return false;
		}
		finally
		{
			sInsideAutomaticLogin.set(false);
		}
	}

	/**
	 * Tries to renew the current access token using the previously acquired refresh token.
	 * @return True if renewal was successful, otherwise false.
	 */
	private static boolean tryRenewLogin()
	{
		// Get the refresh token from the previous login.
		String refreshToken = MyApplication.getInstance().getStringPreference(FIELD_REFRESH_TOKEN);
		if (!Services.Strings.hasValue(refreshToken))
			return false;

		// Forget the renew token, so we won't attempt again until we get a new one.
		MyApplication.getInstance().setStringPreference(FIELD_REFRESH_TOKEN, Strings.EMPTY);

		// Try to renew the access token
		String oauthParameters = getOauthParameters(TYPE_RENEW, null, null, refreshToken);
		return doAutomaticLogin(oauthParameters, isAnonymousUser());
	}

	/**
	 * Tries to log in anonymously if the server has that feature enabled.
	 * @return True if anonymous login was successful, otherwise false.
	 */
	private static boolean tryAnonymousLogin()
	{
		if (!MyApplication.getApp().getEnableAnonymousUser())
			return false;

		// Try to get anonymous session.
		String oauthParameters = getOauthParameters(TYPE_ANONYMOUS, null, null, null);
		return doAutomaticLogin(oauthParameters, true);
	}

	private static boolean doAutomaticLogin(String oauthParameters, boolean isAnonymous)
	{
		String oauthUri = MyApplication.getApp().UriMaker.getLoginUrl();
		ServiceResponse loginResponse = ServiceHelper.StringToPostSecurity(oauthUri, oauthParameters);

		// See if the response was successful and "simulate" completed login if so.
		ResultDetail<?> result = afterLogin(loginResponse, true, isAnonymous);

		return result.getResult();
	}

	public static String getOauthParameters(String type, String user, String password, String refreshToken)
	{
		String clientId = MyApplication.getApp().getClientId();
		String secret = MyApplication.getApp().getSecret();
		return MyApplication.getApp().UriMaker.GetTokenParameters(clientId, secret, type, user, password, refreshToken);
	}

	public enum LoginStatus
	{
		/** Successful login */
		SUCCESS,

		/** Login failed. */
		FAILURE,

		/** The user/password was correct, but the login failed because the password must be changed. */
		CHANGE_PASSWORD,
	}

	public static ResultDetail<LoginStatus> afterLogin(ServiceResponse response)
	{
		return afterLogin(response, false, false);
	}

	private static ResultDetail<LoginStatus> afterLogin(ServiceResponse response, boolean isAutomatic, boolean isAnonymous)
	{
		if (response.getResponseOk())
		{
			String userId = response.get(FIELD_USER_ID);
			String accessToken = response.get(FIELD_ACCESS_TOKEN);
			String refreshToken = response.get(FIELD_REFRESH_TOKEN);

			afterLoginCommon(userId, accessToken, refreshToken, isAutomatic, isAnonymous);
			return ResultDetail.ok(LoginStatus.SUCCESS);
		}

		// An error occurred. Build the message.
		ArrayList<String> messages = new ArrayList<String>();
		if (Services.Strings.hasValue(response.get("error_description"))) //$NON-NLS-1$
			messages.add(response.get("error_description")); //$NON-NLS-1$
		if (Services.Strings.hasValue(response.ErrorMessage))
			messages.add(response.ErrorMessage);

		String message = Services.Strings.join(messages, Strings.NEWLINE);

		// Check for special error codes.
		if (response.StatusCode == DataRequest.ERROR_SECURITY_CHANGE_PASSWORD)
			return ResultDetail.error(message, LoginStatus.CHANGE_PASSWORD);

		return ResultDetail.error(message, LoginStatus.FAILURE);
	}

	public static ResultDetail<?> afterExternalLogin(String resultUri)
	{
		Uri uri = Uri.parse(resultUri);
		if (uri == null)
			return ResultDetail.FALSE;

		// Read fields from login-redirected URI.
		String userId = uri.getQueryParameter(FIELD_USER_ID);
		String accessToken = uri.getQueryParameter(FIELD_ACCESS_TOKEN);
		String refreshToken = uri.getQueryParameter(FIELD_REFRESH_TOKEN);

		if (Services.Strings.hasValue(accessToken))
		{
			// Sucessful login.
			afterLoginCommon(userId, accessToken, refreshToken, false, false);
			return ResultDetail.TRUE;
		}
		else
		{
			// Read and return error detail from URI.
			String errorMessage = uri.getQueryParameter("error_message"); //$NON-NLS-1$
			return ResultDetail.error(errorMessage);
		}
	}

	private static void afterLoginCommon(String userId, String accessToken, String refreshToken, boolean isAutomatic, boolean isAnonymous)
	{
		// Set the token for requests.
		ServiceHelper.setToken(accessToken);

		if (!isAutomatic)
		{
			// If user id is different from previous one (or NOT received from the server) then clear caches.
			// That way we are assured that the cache is ALWAYS cleared if there is a chance that the
			// new user is not the same as the previous one.
			String previousUserId = MyApplication.getInstance().getStringPreference(FIELD_USER_ID);
			if (!Services.Strings.hasValue(userId) || !userId.equalsIgnoreCase(previousUserId))
				EntityDataProvider.clearAllCaches();
		}

		// Remember user id, access token, and refresh token.
		MyApplication.getInstance().setStringPreference(FIELD_ACCESS_TOKEN, accessToken);
		MyApplication.getInstance().setStringPreference(FIELD_USER_ID, userId);
		MyApplication.getInstance().setStringPreference(FIELD_REFRESH_TOKEN, refreshToken);
		MyApplication.getInstance().setBooleanPreference(FLAG_IS_ANONYMOUS, isAnonymous);

		// In case of a "real" login, disable anonymous login on token expiration.
		MyApplication.getInstance().setBooleanPreference(FLAG_DISABLE_ANONYMOUS, !isAnonymous);

		// Obtain user information from server and store it in the device.
		// This currently (sept 2013) fails for the anonymous user. So make up userInfo in that case.
		JSONObject jsonUserInfo = ServiceHelper.getJSONFromUrl(MyApplication.getApp().UriMaker.getUserInformationUrl());
		if (jsonUserInfo != null)
			GAMHelper.afterLogin(jsonUserInfo);
		else
			GAMHelper.afterLogin(userId, isAnonymous);
	}

	public static boolean isLoggedIn()
	{
		return Services.Strings.hasValue(ServiceHelper.getToken());
	}

	public static boolean isAnonymousUser()
	{
		return (MyApplication.getApp().getEnableAnonymousUser() &&
				MyApplication.getInstance().getBooleanPreference(FLAG_IS_ANONYMOUS, false));
	}

	public static void logout()
	{
		// Call server to clear session (ignoring any errors).
		// This must go first because the server needs the token to identify the client logging out.
		ServiceHelper.StringToPostSecurity(MyApplication.getApp().UriMaker.getLogoutUrl(), null);

		// Clear current access token.
		ServiceHelper.setToken(Strings.EMPTY);

		// Clear remembered properties.
		MyApplication.getInstance().setStringPreference(FIELD_ACCESS_TOKEN, Strings.EMPTY);
		MyApplication.getInstance().setStringPreference(FIELD_USER_ID, Strings.EMPTY);
		MyApplication.getInstance().setStringPreference(FIELD_REFRESH_TOKEN, Strings.EMPTY);
		MyApplication.getInstance().setBooleanPreference(FLAG_IS_ANONYMOUS, false);
		MyApplication.getInstance().setBooleanPreference(FLAG_DISABLE_ANONYMOUS, false);

		// Clear user information.
		GAMHelper.afterLogout();

		// Also clear cache.
		EntityDataProvider.clearAllCaches();
	}
}
