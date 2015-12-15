package com.artech.application;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.widget.Toast;

import com.artech.R;
import com.artech.actions.UIContext;
import com.artech.activities.GxAppIntentHandler;
import com.artech.activities.IntentHandlers;
import com.artech.android.ContextImpl;
import com.artech.android.DebugService;
import com.artech.android.ExceptionManager;
import com.artech.android.LogManager;
import com.artech.android.json.NodeCollection;
import com.artech.android.json.NodeObject;
import com.artech.base.metadata.ApplicationDefinition;
import com.artech.base.metadata.AttributeDefinition;
import com.artech.base.metadata.DomainDefinition;
import com.artech.base.metadata.GenexusApplication;
import com.artech.base.metadata.GxObjectDefinition;
import com.artech.base.metadata.IDataSourceDefinition;
import com.artech.base.metadata.IDataViewDefinition;
import com.artech.base.metadata.IPatternMetadata;
import com.artech.base.metadata.IViewDefinition;
import com.artech.base.metadata.ProcedureDefinition;
import com.artech.base.metadata.StructureDataType;
import com.artech.base.metadata.StructureDefinition;
import com.artech.base.metadata.WorkWithDefinition;
import com.artech.base.metadata.enums.Connectivity;
import com.artech.base.metadata.enums.Orientation;
import com.artech.base.metadata.images.ImageCatalog;
import com.artech.base.metadata.languages.Language;
import com.artech.base.metadata.languages.LanguageCatalog;
import com.artech.base.metadata.loader.ApplicationLoader;
import com.artech.base.metadata.loader.LoadResult;
import com.artech.base.metadata.loader.MetadataLoader;
import com.artech.base.metadata.settings.PlatformDefinition;
import com.artech.base.metadata.settings.WorkWithSettings;
import com.artech.base.metadata.theme.ThemeDefinition;
import com.artech.base.model.Entity;
import com.artech.base.providers.IApplicationServer;
import com.artech.base.serialization.INodeCollection;
import com.artech.base.serialization.INodeObject;
import com.artech.base.services.IApplication;
import com.artech.base.services.IDeviceService;
import com.artech.base.services.IResourcesService;
import com.artech.base.services.ISerialization;
import com.artech.base.services.Services;
import com.artech.base.services.UriBuilder;
import com.artech.base.synchronization.SynchronizationHelper.DataSyncCriteria;
import com.artech.base.utils.NameMap;
import com.artech.base.utils.PlatformHelper;
import com.artech.base.utils.ResultRunnable;
import com.artech.base.utils.Strings;
import com.artech.base.utils.Version;
import com.artech.common.ServiceHelper;
import com.artech.common.StringUtil;
import com.artech.compatibility.CompatibilityHelper;
import com.artech.externalapi.ExternalApiDefinition;
import com.artech.externalapi.ExternalApiFactory;
import com.artech.externalapi.IUserExternalApiDeclarations;
import com.artech.layers.ApplicationServer;
import com.artech.providers.DatabaseStorage;
import com.artech.providers.EntityDataProvider;
import com.artech.services.EntityService;
import com.artech.usercontrols.UcFactory;
import com.artech.usercontrols.UserControlDefinition;
import com.artech.utils.Cast;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

public abstract class MyApplication extends Application
	implements IApplication, IDeviceService, ISerialization, IResourcesService
{
	private static GenexusApplication mCurrentApplication;
	private static MyApplication singleton;

	private static IApplicationServer sRemoteServer = new ApplicationServer(Connectivity.Online);
	private static IApplicationServer sLocalServer 	= new ApplicationServer(Connectivity.Offline);

	private static NameMap<Class<? extends Service>> sServiceClasses = new NameMap<Class<? extends Service>>();

	private Thread mUiThread;
	private Handler mUiThreadHandler;
	private Version mOSVersion;

	//Return the application instance
	public static MyApplication getInstance()
	{
		return singleton;
	}

	public static Context getAppContext()
	{
		return MyApplication.getInstance().getApplicationContext();
	}

	public static IApplicationServer getApplicationServer(Connectivity connectivity)
	{
		if (connectivity == Connectivity.Online)
			return  sRemoteServer;
		if (connectivity == Connectivity.Offline)
			return sLocalServer;
		Services.Log.Error("Invalid Connectivity Value, should be online or offline at this point");
		return sRemoteServer;
	}

	public static GenexusApplication getApp()
	{
		return mCurrentApplication;
	}

	private static ApplicationDefinition getAppDefinition()
	{
		if (mCurrentApplication!=null)
			return mCurrentApplication.getDefinition();
		Services.Log.Error("getAppDefinition mCurrentApplication null"); //$NON-NLS-1$
		return null;
	}

	@Override
	public ApplicationDefinition getDefinition()
	{
		return getAppDefinition();
	}

	public static void setApp(GenexusApplication app)
	{
		mCurrentApplication = app;

		// Clear any static data that points to old application.
		PlatformHelper.reset();
	}

	@Override
	public boolean isLoaded()
	{
		return (mCurrentApplication != null &&
				mCurrentApplication.getDefinition() != null &&
				mCurrentApplication.getDefinition().isLoaded());
	}

	@Override
	public void onCreate()
	{
        mUiThread = Thread.currentThread();
        mUiThreadHandler = new Handler();

		connectServices();
		connectUserControls();
		connectExternalApis();
		connectIntentHandlers();

		super.onCreate();
		DebugService.onCreate(this);
		singleton = this;
	}

	private static void connectIntentHandlers()
	{
		IntentHandlers.addHandler(new GxAppIntentHandler());
		IntentHandlers.addHandler("com.artech.android.facebookapi.AppLinksGx");
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		DebugService.onConfigurationChanged(newConfig);
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean handleIntent(UIContext ctx, Intent intent, Entity entity) {
		return IntentHandlers.tryHandleIntent(ctx, intent, entity);
	}

	private void connectServices()
	{
		Services.Application = this;
		Services.Device = this;
		Services.Exceptions = new ExceptionManager();
		Services.HttpService = new ServiceHelper();
		Services.Log = new LogManager();
		Services.Serializer = this;
		Services.Strings = new StringUtil();
		Services.Resources = this;
	}

	private static void connectUserControls() {
		// Load User Controls
		//Atts user controls
		// Load Default Controls
		UserControlDefinition [] definitions = { new UserControlDefinition("Check Box", "com.artech.controls.GxCheckBox") , //$NON-NLS-1$ //$NON-NLS-2$
												 new UserControlDefinition("Radio Button", "com.artech.controls.RadioGroupControl"), //$NON-NLS-1$ //$NON-NLS-2$
												 new UserControlDefinition("SeekBar", "com.artech.controls.SeekBarControl"), //$NON-NLS-1$ //$NON-NLS-2$
												 new UserControlDefinition("Combo Box", "com.artech.controls.SpinnerControl"),  //$NON-NLS-1$ //$NON-NLS-2$
												 new UserControlDefinition("Dynamic Combo Box", "com.artech.controls.DynamicSpinnerControl"), //$NON-NLS-1$ //$NON-NLS-2$
												 new UserControlDefinition("SDGeoLocation", "com.artech.controls.GxSDGeoLocation"), //$NON-NLS-1$ //$NON-NLS-2$
												};

		for (UserControlDefinition definition : definitions)
			UcFactory.addControl(definition.Name, definition);
	}

	private static void connectExternalApis()
	{
		// Load Default APIs.
		ExternalApiDefinition[] definitions =
		{
			new ExternalApiDefinition("sdactions", com.artech.android.api.SDActionsAPI.class),
			new ExternalApiDefinition("interop", com.artech.android.api.InteropAPI.class),
			new ExternalApiDefinition("geolocationapi", com.artech.android.api.GeoLocationAPI.class),
			new ExternalApiDefinition("calendar", com.artech.android.api.CalendarAPI.class),
			new ExternalApiDefinition("addressbook", com.artech.android.api.AddressBookAPI.class),
			new ExternalApiDefinition("clientinformation", com.artech.android.api.ClientInformationAPI.class),
			new ExternalApiDefinition("photolibraryapi", com.artech.android.api.PhotoLibraryAPI.class),
			new ExternalApiDefinition("cameraapi", com.artech.android.api.CameraAPI.class),
			new ExternalApiDefinition("ClientStorageApi", com.artech.android.api.ClientStorageApi.class),
			new ExternalApiDefinition("ProgressIndicator", com.artech.android.api.ProgressIndicatorApi.class),
			new ExternalApiDefinition("AudioAPI", com.artech.android.api.AudioApi.class),
			new ExternalApiDefinition("Store", "com.artech.inappbillinglib.StoreAPI"),
			new ExternalApiDefinition("StoreAPI", "com.artech.inappbillinglib.StoreAPI"),
			new ExternalApiDefinition("localnotifications", com.artech.android.api.LNotificationsAPI.class),
			new ExternalApiDefinition("twitterAPI", "com.artech.android.twitterapi.TwitterApi"),
			new ExternalApiDefinition("facebook", "com.artech.android.facebookapi.FacebookAPI"),
			new ExternalApiDefinition("ScannerAPI", com.artech.android.api.ScannerAPI.class),
			new ExternalApiDefinition("NetworkAPI", com.artech.android.api.NetworkAPI.class),
			new ExternalApiDefinition("Clipboard", com.artech.android.api.Clipboard.class),
			new ExternalApiDefinition("SynchronizationEventsAPI", com.artech.android.api.SynchronizationEventsAPI.class),
			new ExternalApiDefinition("GAMUser", com.artech.android.gam.GAMUserApi.class),
			new ExternalApiDefinition(com.artech.android.api.SharingApi.OBJECT_NAME, com.artech.android.api.SharingApi.class),
			new ExternalApiDefinition("GlobalEvents", com.artech.android.api.EventDispatcher.class),
		};

		// Give the chance to add more custom apis
		addUserDefinedApis();

		for (ExternalApiDefinition definition : definitions)
			ExternalApiFactory.addApi(definition);

		// Add "dummy" APIs.
		ExternalApiFactory.addDummyApi("LocationApi");
	}

	private static void addUserDefinedApis() {
		Class<?> cls = null;
		try {
			cls = Class.forName("com.artech.externalapi.UserExternalApiFactory");
		} catch (ClassNotFoundException e) {
		}
		if (cls != null) {
			Constructor<?> constructor = null;
			try {
				constructor = cls.getConstructor();
			} catch (NoSuchMethodException e) {
			}
			if (constructor != null) {
				IUserExternalApiDeclarations apis = null;
				try {
					apis = (IUserExternalApiDeclarations) constructor.newInstance();
				}
				catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (apis != null) {
					for (ExternalApiDefinition def : apis.getDeclarations()) {
						ExternalApiFactory.addApi(def);
					}
				}
			}
		}
	}

	@Override
	public LoadResult initialize()
	{
		// Load metadata.
		LoadResult loadResult = ApplicationLoader.loadApplication(mCurrentApplication, new ContextImpl(getApplicationContext()), null);

		if (loadResult.getCode() == LoadResult.RESULT_OK)
		{
			// Initialize DB offline storage.
			DatabaseStorage.initialize(getApplicationContext(), mCurrentApplication.getDefinition().getCacheDatabase());

			// If language changes, clear stored data, since it may have translations.
			clearCacheOnLanguageChange();

			for (MetadataLoadingListener listener : mMetadataLoadListeners) {
				listener.onMetadataLoadFinished();
			}
		}

		return loadResult;
	}

	public void registerOnMetadataLoadFinished(MetadataLoadingListener listener) {
		mMetadataLoadListeners.add(listener);
	}

	public void unregisterOnMetadataLoadFinished(MetadataLoadingListener listener) {
		mMetadataLoadListeners.remove(listener);
	}

	private List<MetadataLoadingListener> mMetadataLoadListeners = new ArrayList<>();

	public interface MetadataLoadingListener {
		void onMetadataLoadFinished();
	}

	@Override
	public void resetLoad()
	{
		if (mCurrentApplication != null)
			mCurrentApplication.resetDefinition();
	}

	private void clearCacheOnLanguageChange()
	{
		final String APP_LANGUAGE = "ApplicationLanguage";

		String lastLanguage = getInstance().getStringPreference(APP_LANGUAGE);
		String currentLanguage = Services.Resources.getCurrentLanguage();

		if (lastLanguage == null || !lastLanguage.equalsIgnoreCase(currentLanguage))
			EntityDataProvider.clearAllCaches();

		getInstance().setStringPreference(APP_LANGUAGE, currentLanguage);
	}

	public void showError(Context context, Throwable t) {
		showError( context, t.toString());
	}

	public void showError(Context context, String text)
	{
		AlertDialog.Builder builder=new AlertDialog.Builder(context);
		try
		{
			builder
				.setTitle(R.string.GXM_errtitle)
				.setMessage(text)
				.setPositiveButton(R.string.GXM_button_ok, null)
				.show();
		}
		catch (Exception e)
		{
			// Don't crash if the window has already been closed when showing the error message.
			Services.Log.Error("showError() exception", e);
		}
	}

	public void showMessage(Throwable t)
	{
		showMessage(t.toString());
	}

	public void showMessage(int textRes)
	{
		showMessage(Services.Strings.getResource(textRes));
	}

	public void showMessageShort(int textRes)
	{
		showMessageShort(Services.Strings.getResource(textRes));
	}

	public void showMessage(CharSequence text)
	{
		showToast(text, Toast.LENGTH_LONG);
	}

	public void showMessageShort(String text)
	{
		showToast(text, Toast.LENGTH_SHORT);
	}

	private void showToast(final CharSequence text, final int length)
	{
		if (Services.Strings.hasValue(text))
		{
			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					Toast.makeText(MyApplication.this, text, length).show();
				}
			});
		}
	}

	public void showMessageDialog(final Context context, final Exception exception)
	{
		showMessageDialog(context, exception.getMessage(), exception);
	}

	public void showMessageDialog(final Context context, String exceptionMessage, final Throwable exception)
	{
		AlertDialog.Builder builder = createMessageDialog(context, exceptionMessage, exception);
		builder.show();
	}

	public AlertDialog.Builder createMessageDialog(final Context context, String exceptionMessage, final Throwable exception)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(Services.Strings.getResource(R.string.GXM_errtitle));
		builder.setMessage(exceptionMessage);
		builder.setPositiveButton(R.string.GXM_button_ok, null);

		if (exception != null)
		{
			// Button for seeing call stack.
			builder.setNegativeButton(R.string.GXM_err_details, new OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					StringWriter sw = new StringWriter();
					exception.printStackTrace(new PrintWriter(sw));
					showMessageDialog(context, Services.Strings.getResource(R.string.GXM_err_details), sw.toString());
				}
			});
		}

		return builder;
	}

	public void showMessageDialog(Context context, String title, String text)
	{
		AlertDialog.Builder builder=new AlertDialog.Builder(context);
		builder
			.setTitle(title)
			.setMessage(text)
			.setPositiveButton(R.string.GXM_button_ok, null)
			.show();

	}

	public void showMessageDialog(Context context, String text) 
	{
		AlertDialog.Builder builder = createMessageDialog(context, text);
		builder.show();
	}

	public AlertDialog.Builder createMessageDialog(final Context context,  String text)
	{
		AlertDialog.Builder builder=new AlertDialog.Builder(context);
		builder
			.setMessage(text)
			.setPositiveButton(R.string.GXM_button_ok, null);
		return builder;
	}
	
	public abstract Class<? extends EntityService> getEntityServiceClass();
	public abstract Class<? extends com.artech.android.audio.AudioService> getAudioServiceClass();
	public abstract Class<? extends com.artech.android.audio.AudioIntentReceiver> getAudioIntentReceiverClass();

	public static void registerServiceClass(String key, Class<? extends Service> serviceClass)
	{
		sServiceClasses.put(key, serviceClass);
	}

	public static Class<? extends Service> getServiceClass(String key)
	{
		return sServiceClasses.get(key);
	}

	public abstract EntityDataProvider getProvider();

	@Override
	public INodeObject createNode() {

		return new NodeObject(new JSONObject());
	}

	@Override
	public INodeObject createNode(String json)
	{
		try
		{
			return new NodeObject(new JSONObject(json));
		}
		catch (JSONException e)
		{
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public INodeObject createNode(Object json)
	{
		// Try to deserialize from JSON object.
		INodeObject node = Cast.as(INodeObject.class, json);

		// Try to deserialize fron native JSON object (e.g. from some SD API actions).
		if (node == null && json instanceof JSONObject)
			node = new NodeObject((JSONObject)json);

		// Try to deserialize from string (e.g. from procedure call output).
		if (node == null && json instanceof String)
			node = createNode((String)json);

		// Special case: when the HTTP service doesn't know whether the data is a collection or not,
		// it might create a one-item JSONArray instead of an individual JSONObject. Account for that.
		if (node == null)
		{
			INodeCollection nodes = createCollection(json);
			if (nodes != null && nodes.length() == 1)
				return nodes.getNode(0);
		}

		return node;
	}

	@Override
	public INodeCollection createCollection()
	{
		return new NodeCollection(new JSONArray());
	}

	@Override
	public INodeCollection createCollection(String json)
	{
		try
		{
			return new NodeCollection(new JSONArray(json));
		}
		catch (JSONException e)
		{
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public INodeCollection createCollection(Object json)
	{
		// Try to deserialize from JSON object.
		INodeCollection nodes = Cast.as(INodeCollection.class, json);

		// Fix for Java. If collections of 1 element are serialized as a single node,
		// nodes will be null, but reading a single node will succeed. Create a fake
		// collection in that case.
		if (nodes == null)
		{
			INodeObject singleNode = Cast.as(INodeObject.class, json);
			if (singleNode != null)
			{
				nodes = Services.Serializer.createCollection();
				nodes.put(singleNode);
			}
		}

		// Try to deserialize fron native JSON object (e.g. from some SD API actions).
		if (nodes == null && json instanceof JSONArray)
			nodes = new NodeCollection((JSONArray)json);

		// Try to deserialize from string (e.g. from procedure call output).
		if (nodes == null && json instanceof String)
			nodes = createCollection((String)json);

		return nodes;
	}

	@Override
	public IViewDefinition getView(String name)
	{
		return getAppDefinition().getView(name);
	}

	@Override
	public IDataViewDefinition getDataView(String name)
	{
		return getAppDefinition().getDataView(name);
	}

	@Override
	public IDataSourceDefinition getDataSource(String dpName)
	{
		return getAppDefinition().getDataSource(dpName);
	}

	//Shared Preference handling

	public void setStringPreference(String key, String value)
	{
		SharedPreferences settings = getAppSharedPreferences();
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(key, value);
		editor.commit();
	}

	public String getStringPreference(String key)
	{
		SharedPreferences settings = getAppSharedPreferences();
		return settings.getString(key, Strings.EMPTY);
	}

	public void setBooleanPreference(String key, boolean value)
	{
		SharedPreferences settings = getAppSharedPreferences();
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(key, value);
		editor.commit();
	}

	public boolean getBooleanPreference(String key, boolean defaultValue)
	{
		SharedPreferences settings = getAppSharedPreferences();
		return settings.getBoolean(key, defaultValue);
	}

	public void setLongPreference(String key, long value)
	{
		SharedPreferences settings = getAppSharedPreferences();
		SharedPreferences.Editor editor = settings.edit();
		editor.putLong(key, value);
		editor.commit();
	}

	public long getLongPreference(String key, long defaultValue)
	{
		SharedPreferences settings = getAppSharedPreferences();
		return settings.getLong(key, defaultValue);
	}

	public SharedPreferences getAppSharedPreferences()
	{
		return getAppSharedPreferences(null);
	}

	public static SharedPreferences getAppSharedPreferences(String name)
	{
		String fullName = getInstance().getName() + getInstance().getAppEntry();
		if (Services.Strings.hasValue(name))
			fullName += "." + name;

		return getInstance().getSharedPreferences(fullName, MODE_PRIVATE);
	}


	@Override
	public StructureDefinition getBusinessComponent(String name)
	{
		return getAppDefinition().getBusinessComponent(name);
	}

	@Override
	public String getName()
	{
		return getApp().getName();
	}

	@Override
	public UriBuilder getUriMaker()
	{
		return getApp().UriMaker;
	}

	@Override
	public void putBusinessComponent(StructureDefinition bc)
	{
		getAppDefinition().putBusinessComponent(bc);
	}

	public boolean hasBusinessComponents()
	{
		return getAppDefinition().hasBusinessComponents();
	}
	
	@Override
	public WorkWithSettings getPatternSettings()
	{
		if (getAppDefinition()!=null)
			return getAppDefinition().getSettings();
		Services.Log.Error("getPatternSettings mCurrentApplication null"); //$NON-NLS-1$
		return null;
	}

	@Override
	public void setPatternSettings(WorkWithSettings settings)
	{
		getAppDefinition().putSettings(settings);
	}

	@Override
	public void putPattern(IPatternMetadata pattern, MetadataLoader loader, String filename)
	{
		getAppDefinition().putObject(pattern, loader, filename);
	}

	@Override
	public void setRootUri(String serverUrl)
	{
		getApp().UriMaker.setRootUri(serverUrl);
	}

	@Override
	public void setBaseUri(String baseUri)
	{
		getApp().UriMaker.setBaseUri(baseUri);
	}

	@Override
	public void setAppEntry(String file)
	{
		getApp().setAppEntry(file);
	}

	@Override
	public String getAppEntry()
	{
		return getApp().getAppEntry();
	}

	@Override
	public String getSynchronizer()
	{
		return getApp().getSynchronizer();
	}

	@Override
	public DataSyncCriteria getSynchronizerDataSyncCriteria()
	{
		return getApp().getSynchronizerDataSyncCriteria();
	}

	@Override
	public long getSynchronizerMinTimeBetweenSync()
	{
		return getApp().getSynchronizerMinTimeBetweenSync();
	}

	@Override
	public DomainDefinition getDomain(String name)
	{
		return getAppDefinition().getDomain(name);
	}

	@Override
	public void putDomain(DomainDefinition domain)
	{
		getAppDefinition().putDomain(domain);
	}

	@Override
	public GxObjectDefinition getGxObject(String name)
	{
		return getAppDefinition().getGxObject(GxObjectDefinition.class, name);
	}

	@Override
	public ProcedureDefinition getProcedure(String name)
	{
		return getAppDefinition().getGxObject(ProcedureDefinition.class, name);
	}

	@Override
	public void putGxObject(GxObjectDefinition gxObject)
	{
		getAppDefinition().putGxObject(gxObject);
	}

	@Override
	public AttributeDefinition getAttribute(String name)
	{
		return getAppDefinition().getAttribute(name);
	}

	@Override
	public void putAttribute(AttributeDefinition attribute)
	{
		getAppDefinition().putAttribute(attribute);
	}

	@Override
	public ThemeDefinition getTheme(String name)
	{
		return getAppDefinition().getTheme(name);
	}

	@Override
	public void putTheme(ThemeDefinition theme)
	{
		getAppDefinition().putTheme(theme);
	}

	@Override
	public void setApplicationId(String appId)
	{
		mCurrentApplication.setAppId(appId);
	}

	@Override
	public void setServerSideType(int serverType)
	{
		mCurrentApplication.setServerType(serverType);
	}

	@Override
	public String link(String objName)
	{
		return mCurrentApplication.link(objName);
	}

	@Override
	public String linkObjectUrl(String objPartialUrl)
	{
		return mCurrentApplication.linkObjectUrl(objPartialUrl);
	}

	@Override
	public void putSDT(StructureDataType sdt)
	{
		getAppDefinition().putSDT(sdt);
	}

	@Override
	public StructureDataType getSDT(String name)
	{
		return getAppDefinition().getSDT(name);
	}

	@Override
	public int getOS()
	{
		return PlatformDefinition.OS_ANDROID;
	}

	@Override
	public Version getOSVersion()
	{
		if (mOSVersion == null)
			mOSVersion = new Version(Build.VERSION.RELEASE);

		return mOSVersion;
	}

	@Override
	public int getSDKVersion()
	{
		return Build.VERSION.SDK_INT;
	}

	@Override
	public boolean isDeviceNotificationEnabled()
	{
		return true;
	}

	@Override
	@SuppressLint("NewApi")
	public int getScreenSmallestWidth()
	{
		if (CompatibilityHelper.isApiLevel(Build.VERSION_CODES.HONEYCOMB_MR2))
			return getApplicationContext().getResources().getConfiguration().smallestScreenWidthDp;

		// Calculated (approximately, may consider decorations differently) for older versions.
		DisplayMetrics displayMetrics = getApplicationContext().getResources().getDisplayMetrics();
        int smallestWidthPixels = Math.min(displayMetrics.widthPixels, displayMetrics.heightPixels);
        return Math.round(smallestWidthPixels / displayMetrics.density);
	}

	@Override
	public Orientation getScreenOrientation()
	{
		// Map unknown Android values to Orientation.UNKNOWN.
		int orientation = getResources().getConfiguration().orientation;

		switch (orientation)
		{
			case Configuration.ORIENTATION_PORTRAIT :
				return Orientation.PORTRAIT;
			case Configuration.ORIENTATION_LANDSCAPE :
				return Orientation.LANDSCAPE;
			default :
				return Orientation.UNDEFINED;
		}
	}

	@Override
	public Locale getLocale()
	{
		// Returns the device's current locale.
		return Locale.getDefault();
	}

	@Override
	public int dipsToPixels(int dips)
	{
		final float scale = getApplicationContext().getResources().getDisplayMetrics().density;
		return (int)(dips * scale + 0.5f);
	}

	@Override
	public int pixelsToDips(int pixels)
	{
		final float scale = getApplicationContext().getResources().getDisplayMetrics().density;
		return (int)(pixels / scale + 0.5f);
	}

	@Override
	public void initialize(LanguageCatalog languages, ImageCatalog images)
	{
		getAppDefinition().setImageCatalog(images);
		getAppDefinition().setLanguageCatalog(languages);
	}

	@Override
	public String getImageUri(String imageName)
	{
		if (getApp()==null)
			return null;
		return getAppDefinition().getImageCatalog().getImageUri(imageName);
	}

	@Override
	public int getImageResourceId(String imageName)
	{
		if (getApp() == null)
			return 0;

		String resourceName = getAppDefinition().getImageCatalog().getImageResourceName(imageName);
		if (Services.Strings.hasValue(resourceName))
			return getResources().getIdentifier(resourceName, "drawable", getPackageName()); //$NON-NLS-1$
		else
			return 0;
	}

	@Override
	public String getCurrentLanguage()
	{
		if (getApp() == null)
			return null;

		Language currentLanguage = getAppDefinition().getLanguageCatalog().getCurrentLanguage();
		return (currentLanguage != null ? currentLanguage.getName() : null);
	}

	@Override
	public String getCurrentLanguageProperty(String property)
	{
		if (getApp() != null)
		{
			Language currentLanguage = getAppDefinition().getLanguageCatalog().getCurrentLanguage();
			if (currentLanguage != null)
				return currentLanguage.getProperties().get(property);
		}

		return null;
	}

	@Override
	public String getTranslation(String message)
	{
		if (getApp() == null)
			return message;

		return getAppDefinition().getLanguageCatalog().getTranslation(message);
	}

	@Override
	public String getTranslation(String message, String language)
	{
		if (getApp() == null)
			return message;

		return getAppDefinition().getLanguageCatalog().getTranslation(message, language);
	}

	@Override
	public String getExpressionTranslation(String expression)
	{
		if (getApp() == null)
			return expression;

		return getAppDefinition().getLanguageCatalog().getExpressionTranslation(expression);
	}

	@Override
	public IPatternMetadata getPattern(String name)
	{
		return getAppDefinition().getObject(name);
	}

	@Override
	public WorkWithDefinition getWorkWithForBC(String bcName)
	{
		return getAppDefinition().getWorkWithForBC(bcName);
	}

	private static final String FILE_RESERVED_CHARS = "|\\?*<\":>+[]/' ";     //$NON-NLS-1$
	private static final int FILE_NAME_MAXIMUM_LENGTH = 127;

	@Override
	public String makeFileName(String name)
	{
		if (name != null)
		{
			for (int i = 0; i < FILE_RESERVED_CHARS.length(); i++)
				name = name.replace(FILE_RESERVED_CHARS.charAt(i), '_');

			// When trimming, keep the ending part (supposed to be more representative,
			// at least when creating names from URLs).
			if (name.length() > FILE_NAME_MAXIMUM_LENGTH)
				name = name.substring(name.length() - FILE_NAME_MAXIMUM_LENGTH);
		}

		return name;
	}

	@Override
	public boolean serializeObject(Object object, String filename)
	{
		try
		{
			FileOutputStream file = openFileOutput(filename, Context.MODE_PRIVATE);
			ObjectOutputStream output = new ObjectOutputStream(file);
			output.writeObject(object);
			output.close();
			return true;
		}
		catch (IOException ex)
		{
			Services.Log.Error(String.format("Error serializing object to '%s'.", filename), ex);
			return false;
		}
	}

	@Override
	public Object deserializeObject(String filename)
	{
		try
		{
			FileInputStream file = openFileInput(filename);
			ObjectInputStream input = new ObjectInputStream(file);
			Object obj = input.readObject();
			input.close();
			return obj;
		}
		catch(Exception ex)
		{
			Services.Log.Error(String.format("Error deserializing object from '%s'.", filename), ex);
			return false;
		}
	}

	@Override
	public boolean isMainThread()
	{
		return (mUiThread == null || Thread.currentThread() == mUiThread);
	}

	@Override
	public void runOnUiThread(Runnable r)
	{
		if (isMainThread())
			r.run();
		else if (mUiThreadHandler != null)
			mUiThreadHandler.post(r);
	}

	@Override
	public void postOnUiThread(Runnable r)
	{
		if (mUiThreadHandler != null)
			mUiThreadHandler.post(r);
	}

	@Override
	public void postOnUiThreadDelayed(Runnable r, long delayMillis)
	{
		if (mUiThreadHandler != null)
			mUiThreadHandler.postDelayed(r, delayMillis);
	}

	@Override
	public String getAppsLinksProtocol() {
		return Strings.EMPTY;
	}

	private static class Reference<V>
	{
		V value;
	}

	@Override
	public <V> V invokeOnUiThread(final ResultRunnable<V> r)
	{
		// Simple scenario.
		if (isMainThread())
			return r.run();

		// Complicated scenario. First wrap the ResultRunnable into a standard Runnable
		// that will store the result in a local variable notify() itself when finished.
		final Reference<V> result = new Reference<V>();
		final Runnable runnable = new Runnable()
		{
			@Override
			public void run()
			{
				result.value = r.run();
				synchronized (this) { notify(); }
			}
		};

		// Then post said runnable to the queue, and wait for it to complete.
		synchronized(runnable)
		{
			mUiThreadHandler.post(runnable);
			try
			{
				runnable.wait();
			}
			catch (InterruptedException e)
			{
				Services.Log.error(e);
				return null;
			}
		}

		// Finally return the stored result, if any.
		return result.value;
	}

	@Override
	public void invokeOnUiThread(final Runnable r)
	{
		invokeOnUiThread(new ResultRunnable<Void>()
		{
			@Override
			public Void run()
			{
				r.run();
				return null;
			}
		});
	}

	@Override
	public boolean isLiveEditingEnabled() {
		return false;
	}

	//get analitics tracker like:
	// https://developers.google.com/analytics/devguides/collection/android/v4/
	// use only one tracker per app
	private Tracker mTracker = null;

	public synchronized Tracker getTracker()
	{
		if (mTracker == null)
		{
			GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
			String trackerId = getString(R.string.ga_trackingId);
			mTracker = analytics.newTracker(trackerId);
		}

		return mTracker;
	}

	public synchronized GoogleAnalytics getAnalytics()
	{
		return GoogleAnalytics.getInstance(this);
	}
}
