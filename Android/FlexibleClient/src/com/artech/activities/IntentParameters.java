package com.artech.activities;

public class IntentParameters
{
	public static final int REQUEST_CODE_PROMPT = 3;

	// TODO: Intent extra names should begin with the package name.
	public static final String DataView = "DataView"; //$NON-NLS-1$
	public static final String Parameters = "Parameters"; //$NON-NLS-1$
	public static final String Mode = "Mode"; //$NON-NLS-1$

	public static final String DashBoardMetadata = "DashBoardMetadata"; //$NON-NLS-1$
	public static final String IsSelecting = "IsSelecting"; //$NON-NLS-1$

	public static final String IS_STARTUP_ACTIVITY = "IsStartupActivity"; //$NON-NLS-1$

	public static final String AttName 			= "AttName"; //$NON-NLS-1$
	public static final String RangeBegin 		= "RangeBegin"; //$NON-NLS-1$
	public static final String RangeEnd 		= "RangeEnd"; //$NON-NLS-1$
	public static final String FilterDefault	= "FilterDefault"; //$NON-NLS-1$
	public static final String FilterRangeFk	= "FilterRangeFk"; //$NON-NLS-1$

	public static final String BCFieldParameters = "BCFieldParameters"; //$NON-NLS-1$

	public static final String ServerURL		= "ServerURL";  //$NON-NLS-1$

	public static final String NotificationAction		= "NotificationAction";  //$NON-NLS-1$
	public static final String NotificationParameters		= "NotificationParameters";  //$NON-NLS-1$

    public static final String EXTERNAL_LOGIN_RESULT = "ExternalLoginResult";
    public static final String ExternalLoginCall = "ExternalLoginCall";

	/**
	 * Intent parameters associated to the Filters activity and related functionality.
	 */
	public static class Filters
	{
		public static final String DataSourceId = "DataSourceId"; //$NON-NLS-1$
		public static final String DataSource = "DataSource"; //$NON-NLS-1$
		public static final String Uri = "GxUri"; //$NON-NLS-1$
		public static final String FiltersFK = "FiltersFK"; //$NON-NLS-1$
	}

	/**
	 * Intent parameters used exclusively for communication with EntityService.
	 */
	public static class Service
	{
		public static final String DataViewSession = "DataViewSession"; //$NON-NLS-1$
		public static final String DataProvider = "DataProvider"; //$NON-NLS-1$
		public static final String RequestType = "RequestType"; //$NON-NLS-1$
		public static final String IntentFilter = "IntentFilter"; //$NON-NLS-1$
		public static final String RequestCount = "RequestCount"; //$NON-NLS-1$
	}

	/***
	 * Used to pass Connectivity Support property
	 */
	public static String Connectivity = "Connectivity"; //$NON-NLS-1$
}
