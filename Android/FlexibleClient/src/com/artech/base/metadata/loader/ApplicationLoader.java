package com.artech.base.metadata.loader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Locale;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sqldroid.SQLDroidDriver;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.support.v4.app.NotificationCompat;
import android.util.Pair;

import com.artech.R;
import com.artech.activities.ActivityLauncher;
import com.artech.android.ContextImpl;
import com.artech.android.GooglePlayServicesHelper;
import com.artech.android.api.LocationHelper;
import com.artech.android.gam.GAMUser;
import com.artech.android.gcm.DeviceRegister;
import com.artech.android.json.NodeObject;
import com.artech.android.notification.NotificationHelper;
import com.artech.application.MyApplication;
import com.artech.base.metadata.AttributeDefinition;
import com.artech.base.metadata.DataProviderDefinition;
import com.artech.base.metadata.DomainDefinition;
import com.artech.base.metadata.GenexusApplication;
import com.artech.base.metadata.GxObjectDefinition;
import com.artech.base.metadata.IPatternMetadata;
import com.artech.base.metadata.IViewDefinition;
import com.artech.base.metadata.InstanceProperties;
import com.artech.base.metadata.ObjectParameterDefinition;
import com.artech.base.metadata.ProcedureDefinition;
import com.artech.base.metadata.StructureDataType;
import com.artech.base.metadata.StructureDefinition;
import com.artech.base.metadata.enums.Connectivity;
import com.artech.base.metadata.enums.GxObjectTypes;
import com.artech.base.metadata.images.ImageCatalog;
import com.artech.base.metadata.languages.LanguageCatalog;
import com.artech.base.metadata.settings.WorkWithSettings;
import com.artech.base.metadata.settings.WorkWithSettingsLoader;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.metadata.theme.ThemeClassFactory;
import com.artech.base.metadata.theme.ThemeDefinition;
import com.artech.base.metadata.theme.TransformationDefinition;
import com.artech.base.model.EntityList;
import com.artech.base.serialization.INodeCollection;
import com.artech.base.serialization.INodeObject;
import com.artech.base.services.AndroidContext;
import com.artech.base.services.IContext;
import com.artech.base.services.IProgressNotification;
import com.artech.base.services.Services;
import com.artech.base.synchronization.SynchronizationAlarmReceiver;
import com.artech.base.synchronization.SynchronizationHelper;
import com.artech.base.utils.PlatformHelper;
import com.artech.base.utils.Strings;
import com.artech.common.SecurityHelper;
import com.artech.common.ServiceHelper;
import com.artech.layers.GxObjectFactory;
import com.artech.layers.LocalUtils;
import com.genexus.Application;
import com.genexus.ApplicationContext;
import com.genexus.ClientContext;
import com.genexus.GXReorganization;

public class ApplicationLoader
{
	public static boolean MetadataReady = false;

	public final static int STEP_INIT = 0;
	public final static int STEP_WWSD_SETTINGS = 1;
	public final static int STEP_THEMES = 2;
	public final static int STEP_RESOURCES = 3;
	public final static int STEP_DOMAINS = 4;
	public final static int STEP_ATTRIBUTES = 5;
	public final static int STEP_SDTS = 6;
	public final static int STEP_TRANSACTIONS = 7;
	public final static int STEP_PANELS = 8;
	public final static int STEP_PROCEDURES = 9;
	public final static int STEP_FINISH = 10;

	private final static String REOR_VER_STAMP = "reor_ver_stamp"; //$NON-NLS-1$
	private final static String REOR_MD5_HASH = "reor_md5_hash"; //$NON-NLS-1$

	public static LoadResult loadApplication(GenexusApplication application, IContext context, IProgressNotification progress)
	{
		MetadataReady = false;

		String baseUri = application.getAPIUri();
		String serverUrl = baseUri;
		if (baseUri.endsWith("/")) //$NON-NLS-1$
			serverUrl = baseUri.substring(0, baseUri.length() - 1);

		Services.Application.setRootUri(serverUrl);
		Services.Application.setBaseUri(serverUrl + "/rest"); //$NON-NLS-1$
		Services.Application.setAppEntry(application.getAppEntry());


		setApplicationUpdateParameters(application, context);
		if (!MetadataLoader.FILES_IN_RAW)
		{
			// this code only should run in debug mode, with no files in raw
			// run the old way get metadata first from server.
			if (needsApplicationUpdate(application, context))
				return LoadResult.result(LoadResult.RESULT_UPDATE);
		}

		try
		{
			// Load all metadata files.
			loadMetadata(context, progress);

			// Post-processing.
			initializeMain(application);

			// create database, before register for notification
			if (!createApplicationDatabase(context))
				return LoadResult.error(new Exception("Reorganization not found in package."));

			registerforNotification(context);
			//initialize Location services if used
			initLocationServices();

			updateProgress(progress, STEP_FINISH, null);

			MetadataReady = true;
			if (MetadataLoader.MUST_RELOAD_METADATA)
			{
				// save the new minor version, for this current mayor
				context.saveMinorVersion(MetadataLoader.getPrefsName() + application.getMajorVersion() + "-MinorVersion", MetadataLoader.REMOTE_MINOR_VERSION); //$NON-NLS-1$
				MetadataLoader.MUST_RELOAD_METADATA = false;
			}

			// Load current user data from last session. This must be done before synchronizer in case it uses the GAMUser object.
			SecurityHelper.restoreLoginInformation();

			// Always acquire anonymous user on start. Fail if not connected.
			if (!Strings.hasValue(GAMUser.getCurrentUserId()) && MyApplication.getApp().isSecure() && MyApplication.getApp().getEnableAnonymousUser())
			{
				if (!SecurityHelper.tryAutomaticLogin())
				{
					String errorMessage = Services.Strings.getResource(R.string.GXM_NetworkError, "connection failed");
					return LoadResult.error(new Exception(errorMessage));
				}
			}

			if (MetadataLoader.FILES_IN_RAW)
			{
				// if offline and syncronizer , dont check metadata in background, do it now.
				if (MyApplication.getApp().isOfflineApplication()
						&& (MyApplication.getApp().getRunSynchronizerAtStartup() ||
						(MyApplication.getApp().getSynchronizerReceiveAfterElapsedTime() && !Services.Strings.hasValue(MyApplication.getApp().getSynchronizerReceiveCustomProcedure()) ))
						)
				{
					CheckMetadataApplication();
					if (MetadataLoader.MUST_RELOAD_APP)
					{
						return LoadResult.result(LoadResult.RESULT_UPDATE_RELAUNCH);
					}
				}
				else
				{
					//post in background load of new metadata if necessary
					CheckMetadataInBackground();
				}
			}
			else
			{
				//Temp , see where to call it
				syncOfflineData();
			}

			return LoadResult.result(LoadResult.RESULT_OK);
		}
		catch (Throwable ex)
		{
			Services.Log.Error("ApplicationLoader exception", ex);
			return LoadResult.error(ex);
		}
	}

	/**
	 * Reads the minimum metadata necessary to make startup decisions.
	 */
	public static boolean preloadApplication()
	{
		if (MyApplication.getApp() != null && Strings.hasValue(Services.Application.getAppEntry()))
		{
			if (MyApplication.getApp().getMain() != null)
				return true; // Already done.

			boolean previous = MetadataLoader.READ_RESOURCES;
			try
			{
				MetadataLoader.READ_RESOURCES = false; // To use updated metadata if already downloaded.
				ContextImpl context = new ContextImpl(MyApplication.getAppContext());

				loadPatternSettings(context);
				return loadMainObjectProperties(MyApplication.getApp(), context);
			}
			catch (LoadException e)
			{
				Services.Log.error(e);
				return false;
			}
			finally
			{
				MetadataLoader.READ_RESOURCES = previous;
			}
		}
		else
			return false;
	}

	//check metadata delayed
	private static void CheckMetadataInBackground()
	{
		Thread thread = new Thread(null, doCheckMetadata,"BackgroundCheckMetadata"); //$NON-NLS-1$
		thread.start();
	}

	private static final Runnable doCheckMetadata = new Runnable(){
		@Override
		public void run(){
			CheckMetadataApplication();
		}
	};

	private static void CheckMetadataApplication()
	{
		if (needsApplicationUpdate(MyApplication.getApp(),new ContextImpl(MyApplication.getAppContext() )))
		{
			// Go Home and clear stack.
			MetadataLoader.MUST_RELOAD_APP = true;
			ActivityLauncher.callApplicationMain(MyApplication.getAppContext(), true, false);
		}
		else if (MetadataLoader.MUST_RELOAD_METADATA)
		{
			// must reaload metadata and re launch main of the app.
			// Go Home and clear stack.
			ActivityLauncher.callApplicationMain(MyApplication.getAppContext(), true, false);
		}
		else
		{
			//Temp , see where to call it
			syncOfflineData();
		}
	}

	private static void loadMetadata(IContext context, IProgressNotification progress) throws LoadException
	{
		ServiceHelper.createApplicationMetadata(context, MyApplication.getAppContext());

		// Services.Log.debug("start reading metadata"); //$NON-NLS-1$
		updateProgress(progress, STEP_INIT, null);
		loadAppId(context);

		// Read the project file, used below.
		MetadataFile metadata = new MetadataFile(context, Services.Application.getAppEntry());

		loadPatternSettings(context);
		updateProgress(progress, STEP_WWSD_SETTINGS, null);

		loadThemes(context);
		updateProgress(progress, STEP_THEMES, null);

		loadResources(context);
		updateProgress(progress, STEP_RESOURCES, null);

		loadDomains(context);
		updateProgress(progress, STEP_DOMAINS, null);

		loadAttributes(metadata);
		updateProgress(progress, STEP_ATTRIBUTES, null);

		loadSDTs(metadata);
		updateProgress(progress, STEP_SDTS, null);

		loadEntities(metadata);
		updateProgress(progress, STEP_TRANSACTIONS, null);

		loadPatternInstances(metadata);
		updateProgress(progress, STEP_PANELS, null);

		loadProcedures(metadata);
		updateProgress(progress, STEP_PROCEDURES, null);

		MyApplication.getInstance().getDefinition().setLoaded(true);
		// Services.Log.debug("end reading metadata"); //$NON-NLS-1$
	}

	private static void setApplicationUpdateParameters(GenexusApplication application, IContext context)
	{
		int currentMinorVersion = (int) context.getMinorVersion(MetadataLoader.getPrefsName() + application.getMajorVersion() + "-MinorVersion", application.getMinorVersion()); //$NON-NLS-1$
		if (currentMinorVersion != application.getMinorVersion())
			MetadataLoader.READ_RESOURCES = false;

		// read if has raw resources., not load zip if is the same version as raw
		boolean filesInRaw = MetadataLoader.getHasAppIdInRaw(context);
		if (!filesInRaw)
		{
			MetadataLoader.FILES_IN_RAW = false;
		}

	}

	private static boolean needsApplicationUpdate(GenexusApplication application, IContext context)
	{
		if (Services.HttpService.isOnline())
		{
			Services.Log.debug("start get remote version"); //$NON-NLS-1$
			MetadataLoader.REMOTE_VERSION = Services.HttpService.getRemoteMetadataVersion();
			Services.Log.debug("end get remote version"); //$NON-NLS-1$
			Services.Log.debug("start check for update"); //$NON-NLS-1$
			MetadataLoader.REMOTE_MAJOR_VERSION = Services.HttpService.getRemoteMajorVersion(application.getAppEntry());
			MetadataLoader.REMOTE_MINOR_VERSION = Services.HttpService.getRemoteMinorVersion(application.getAppEntry());
			MetadataLoader.REMOTE_VERSION_URL = Services.HttpService.getRemoteUrlVersion(application.getAppEntry());


			// For prototyper all applications has currentMajorVersion = -1
			int currentMajorVersion = application.getMajorVersion();
			if (currentMajorVersion > 0)
			{
				// get the last minor downloaded in this major
				int currentMinorVersion = (int) context.getMinorVersion(MetadataLoader.getPrefsName() + application.getMajorVersion() + "-MinorVersion", application.getMinorVersion()); //$NON-NLS-1$
				if (currentMajorVersion < MetadataLoader.REMOTE_MAJOR_VERSION)
					return true;

				if (currentMajorVersion == MetadataLoader.REMOTE_MAJOR_VERSION && currentMinorVersion < MetadataLoader.REMOTE_MINOR_VERSION)
					MetadataLoader.MUST_RELOAD_METADATA = true;

				if (currentMinorVersion != application.getMinorVersion())
					MetadataLoader.READ_RESOURCES = false;
			}
			Services.Log.debug("end check for update"); //$NON-NLS-1$
		}
		return false; // Either minor updates, or no update.
	}

	private static void updateProgress(IProgressNotification progress, int state, String message)
	{
		if (progress != null)
			progress.updateProgress(state, message);
	}

	private static void loadSDTs(MetadataFile metadata)
	{
		// We do two passes: first one creates all SDTs (with empty definition), second one deserializes them.
		// This is because SDT_A may have a member of type SDT_B, which is after it in the file.
		// If we did just one pass, the deserialized definition of SDT_A will be incomplete.
		INodeCollection arrSDTs = metadata.getSDTs();
		LinkedList<Pair<StructureDataType, INodeObject>> sdtDefinitions = new LinkedList<Pair<StructureDataType, INodeObject>>();

		for (int i = 0; i < arrSDTs.length() ; i++)
		{
			INodeObject jsonSdt = arrSDTs.getNode(i);
			StructureDataType sdt = new StructureDataType(jsonSdt);

			Services.Application.putSDT(sdt);
			sdtDefinitions.add(new Pair<StructureDataType, INodeObject>(sdt, jsonSdt));
		}

		for (Pair<StructureDataType, INodeObject> sdt : sdtDefinitions)
			sdt.first.deserialize(sdt.second);

		//Put SDT for offline replication.
		if (MyApplication.getApp().isOfflineApplication())
		{
			putOfflineSDT("GxSynchroEventResultSDT", resultSDTString, false); //$NON-NLS-1$
			putOfflineSDT("GxSynchroEventSDT", eventSDTString, true); //$NON-NLS-1$
			putOfflineSDT("GxSynchroInfoSDT", eventSynchroInfoSDTString, false); //$NON-NLS-1$

		}
	}

	private static void putOfflineSDT(String name, String jsonStringValue, boolean replace)
	{
		if (Services.Application.getSDT(name)==null || replace)
		{
			JSONObject jsonObj = null;
			try
			{
				jsonObj = new JSONObject(jsonStringValue);
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}

			if (jsonObj != null)
			{
				NodeObject resultSDT = new NodeObject(jsonObj);
				StructureDataType sdt = new StructureDataType(resultSDT);
				sdt.deserialize(resultSDT);
				Services.Application.putSDT(sdt);
			}
		}
	}

	private static final String resultSDTString = "{\"Name\" : \"GxSynchroEventResultSDT\", \"IsCollection\" : true, \"CollectionItemName\" : \"GxSynchroEventResultSDTItem\", " +
        "\"Items\" : [ { \"Name\" : \"EventId\", \"Type\" : \"guid\", \"Length\" : 4, \"Decimals\" : 0 }, { " +
            "\"Name\" : \"EventTimestamp\", \"Type\" : \"datetime\", \"Length\" : 8, \"Decimals\" : 5 }, { " +
            "\"Name\" : \"EventStatus\", \"Type\" : \"numeric\", \"Length\" : 4, \"Decimals\" : 0, \"Domain\" : \"eventstatus\" }, { " +
            "\"Name\" : \"EventErrors\", \"Type\" : \"longvarchar\", \"Length\" : 2097152, \"Domain\" : \"eventerrors\" }, { " +
            "\"Name\" : \"Mappings\", \"IsCollection\" : true, \"CollectionItemName\" : \"MappingsItem\", \"Items\" : [ { " +
                "\"Name\" : \"Table\", \"Type\" : \"varchar\", \"Length\" : 128 }, { " +
                "\"Name\" : \"Updates\", \"Type\" : \"longvarchar\", \"Length\" : 2097152 }, { " +
                "\"Name\" : \"Conditions\", \"Type\" : \"longvarchar\", \"Length\" : 2097152 } ] " +
        "} ] }"; //$NON-NLS-1$


	private static final String eventSDTString = "{ \"Name\" : \"GxSynchroEventSDT\", \"IsCollection\" : true, \"CollectionItemName\" : \"GxSynchroEventSDTItem\", " +
        "\"Items\" : [ { \"Name\" : \"EventId\", \"Type\" : \"guid\", \"Length\" : 4, \"Decimals\" : 0 }, { " +
            "\"Name\" : \"EventTimestamp\", \"Type\" : \"datetime\", \"Length\" : 8, \"Decimals\" : 5 }, { " +
            "\"Name\" : \"EventBC\", \"Type\" : \"varchar\", \"Length\" : 128 }, { " +
            "\"Name\" : \"EventAction\", \"Type\" : \"numeric\", \"Length\" : 4, \"Decimals\" : 0, \"Domain\" : \"eventaction\" }, { " +
            "\"Name\" : \"EventData\", \"Type\" : \"longvarchar\", \"Length\" : 2097152, \"Domain\" : \"eventdata\" }, { " +
            "\"Name\" : \"EventStatus\", \"Type\" : \"numeric\", \"Length\" : 4, \"Decimals\" : 0, \"Domain\" : \"eventstatus\" }, { " +
            "\"Name\" : \"EventFiles\", \"Type\" : \"longvarchar\", \"Length\" :20971524, \"Decimals\" : 0 }, { " +
            "\"Name\" : \"EventErrors\", \"Type\" : \"longvarchar\", \"Length\" : 2097152, \"Domain\" : \"eventerrors\" } ] " +
    	" } ";  //$NON-NLS-1$

	private static final String eventSynchroInfoSDTString = "{ \"Name\" : \"GxSynchroInfoSDT\", " +
	        "\"Items\" : [ { \"Name\" : \"GxAppVersion\", \"Type\" : \"character\", \"Length\" : 20 } ] " +
	    	" } ";  //$NON-NLS-1$

	private static void loadPatternSettings(IContext context)
	{
		WorkWithSettings wwSettings = WorkWithSettingsLoader.load(context);
		if (wwSettings != null)
			Services.Application.setPatternSettings(wwSettings);
	}

	private static boolean loadMainObjectProperties(GenexusApplication application, IContext context) throws LoadException
	{
		String resourceName = String.format("%s.properties", Strings.toLowerCase(Services.Application.getAppEntry()));
		INodeObject json = MetadataLoader.getDefinition(context, resourceName);
		if (json != null)
		{
			INodeObject jsonProperties = json.optNode("properties");
			if (jsonProperties != null)
			{
				InstanceProperties mainProperties = new InstanceProperties();
				mainProperties.deserialize(jsonProperties);

				application.setMainProperties(mainProperties);
				return true;
			}
		}

		return false;
	}

	private static void loadPatternInstances(MetadataFile metadata) throws LoadException
	{
		INodeCollection patterns = metadata.getPatternInstances();

		DashboardMetadataLoader dashboardLoader = new DashboardMetadataLoader();
		WorkWithMetadataLoader workWithLoader = new WorkWithMetadataLoader();

		for (INodeObject instance : patterns)
		{
			String instanceType = instance.getString("Type"); //$NON-NLS-1$
			String instanceName = instance.getString("Name"); //$NON-NLS-1$

			MetadataLoader loader = null;
			String fileName = null;

			if (instanceType.equalsIgnoreCase(GxObjectTypes.DashboardGuid))
			{
				// Dashboard
				loader = dashboardLoader;
				fileName = Strings.toLowerCase(instanceName) + ".menu"; //$NON-NLS-1$
			}
			else if (instanceType.equalsIgnoreCase(GxObjectTypes.SDPanelGuid))
			{
				// Panel.
				loader = workWithLoader;
				fileName = Strings.toLowerCase(instanceName) + ".panel"; //$NON-NLS-1$
			}
			else if (instanceType.equalsIgnoreCase(GxObjectTypes.WorkWithGuid))
			{
				// WWSD.
				loader = workWithLoader;
				fileName = Strings.toLowerCase(instanceName) + ".ww"; //$NON-NLS-1$
			}

			try
			{
				if (loader != null)
				{
					IPatternMetadata data = loader.load(metadata.getContext(), fileName);
					if (data != null)
					{
						data.setName(instanceName);
						Services.Application.putPattern(data, loader, fileName);
					}
				}
				else
					Services.Log.Error(String.format("Instance '%s' has an unrecognized type (%s).", instanceName, instanceType)); //$NON-NLS-1$
			}
			catch (Exception ex)
			{
				throw LoadException.from(instanceName, ex);
			}
		}
	}

	private static void loadEntities(MetadataFile metadata) throws LoadException
	{
		ArrayList<String> bcNames = new ArrayList<String>();

		// Try reading list of BCs from new file (bc_list.json).
		// Unlike the other get() methods, this may return null. In that case read from the older file.
		INodeCollection bcList = metadata.getBCs();
		if (bcList != null)
		{
			for (INodeObject jsonBC : bcList)
			{
				String bcName = jsonBC.optString("n"); //$NON-NLS-1$
				if (Strings.hasValue(bcName))
					bcNames.add(bcName);
			}
		}
		else
		{
			// Try reading list of BCs from old file (gx_entity_list.json).
			INodeObject entities = MetadataLoader.getDefinition(metadata.getContext(), "gx_entity_list"); //$NON-NLS-1$
			if (entities != null)
			{
				INodeObject jsonMetadata = entities.getNode("Metadata"); //$NON-NLS-1$
				for (INodeObject jsonEntity : jsonMetadata.optCollection("ObjectList")) //$NON-NLS-1$
				{
					String objType = jsonEntity.getString("ObjectType"); //$NON-NLS-1$
					if (objType.equals("BC")) //$NON-NLS-1$
					{
						String bcName = jsonEntity.getString("ObjectName"); //$NON-NLS-1$
						if (Strings.hasValue(bcName))
							bcNames.add(bcName);
					}
				}
			}
		}

		// Read the definition for each BC listed before.
		for (String bcName : bcNames)
		{
			EntityDefinitionLoader loader = new EntityDefinitionLoader();
			try
			{
				IPatternMetadata data = loader.load(metadata.getContext(), bcName);
				if (data != null)
					Services.Application.putBusinessComponent((StructureDefinition)data);
			}
			catch (Exception ex)
			{
				throw LoadException.from(bcName, ex);
			}
		}
	}

	// Information about server side application
	private static void loadAppId(IContext context)
	{
		INodeObject appid = MetadataLoader.getDefinition(context, "appid"); //$NON-NLS-1$

		if (appid != null)
		{
			Services.Application.setApplicationId(appid.getString("id")); //$NON-NLS-1$
			Integer serverType = (Integer) appid.get("servertype"); //$NON-NLS-1$
			if (serverType != null)
				Services.Application.setServerSideType( serverType);
		}
	}

	private static void loadDomains(IContext context) throws LoadException
	{
		INodeObject domains = MetadataLoader.getDefinition(context, "domains"); //$NON-NLS-1$
		if (domains != null)
		{
			try
			{
				INodeCollection arrEntities = domains.getCollection("Domains"); //$NON-NLS-1$
				for (int i = 0; i < arrEntities.length() ; i++)
				{
					INodeObject obj = arrEntities.getNode(i);
					DomainDefinition def = new DomainDefinition(obj);
					Services.Application.putDomain(def);
				}
			}
			catch (Exception ex)
			{
				throw LoadException.from("Domains", ex);
			}
		}
	}

	private static void loadProcedures(MetadataFile metadata) throws LoadException
	{
		try
		{
			INodeCollection arrProcs = metadata.getProcedures();
			for (int i = 0; i < arrProcs.length() ; i++)
			{
				INodeObject obj = arrProcs.getNode(i);
				GxObjectDefinition def = readOneGxObject(obj);
				Services.Application.putGxObject(def);
			}
		}
		catch (Exception ex)
		{
			throw LoadException.from("Procedures", ex);
		}

		//Put procs for offline replication.
		if (MyApplication.getApp().isOfflineApplication())
		{
			if (Services.Application.getGxObject("GxOfflineEventReplicator")==null)
			{
				JSONObject jsonObj = null;
				try {
					jsonObj = new JSONObject(eventProcString);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				if (jsonObj!=null)
				{
					NodeObject offlineProc = new NodeObject(jsonObj);
					GxObjectDefinition proc = readOneGxObject(offlineProc);
					Services.Application.putGxObject(proc);
				}
			}
		}
	}

	private static String eventProcString = "{ \"n\" : \"GxOfflineEventReplicator\", \"t\" : \"P\", " +
        "\"p\" : [ { \"m\" : \"in\", \"n\" : \"GxPendingEvents\", \"Name\" : \"GxPendingEvents\", \"Type\" : \"gx_sdt\", " +
            "\"TypeName\" : \"GxSynchroEventSDT\", \"t\" : \"gx_sdt\", \"isc\" : false, \"isa\" : false }, { " +
            "\"m\" : \"out\", \"n\" : \"EventResults\", \"Name\" : \"EventResults\", \"Type\" : \"gx_sdt\", " +
            "\"TypeName\" : \"GxSynchroEventResultSDT\", \"t\" : \"gx_sdt\", \"isc\" : false, \"isa\" : false } ] " +
    "} ";  //$NON-NLS-1$


	public static Connectivity readConnectivity(INodeObject obj) {
		String connectivity = obj.optString("@idConnectivitySupport");
		if (!Services.Strings.hasValue(connectivity)) {
			//read the other format
			connectivity = obj.optString("idConnectivitySupport");
		}
		if (Services.Strings.hasValue(connectivity)) {
			if (connectivity.equalsIgnoreCase("idOffline")) {
				return Connectivity.Offline;
			} else if (connectivity.equalsIgnoreCase("idOnline")) {
				return Connectivity.Online;
			}
		}
		return Connectivity.Inherit;
	}

	private static GxObjectDefinition readOneGxObject(INodeObject obj)
	{
		String objName = obj.getString("n");
		String objType = obj.optString("t"); // Procedure (default) or Data Provider

		GxObjectDefinition gxObject;
		if (Strings.hasValue(objType) && objType.equalsIgnoreCase("D"))
			gxObject = new DataProviderDefinition(objName);
		else
			gxObject = new ProcedureDefinition(objName);

		// Read Connectivity Support consider Inherit for old metadata without this information
		gxObject.setConnectivitySupport(readConnectivity(obj));

		for (INodeObject procParam : obj.getCollection("p")) //$NON-NLS-1$
		{
			// New or old format?
			String parameterName = procParam.optString("Name"); //$NON-NLS-1$
			if (!Strings.hasValue(parameterName))
				parameterName = procParam.getString("n"); //$NON-NLS-1$

			String parameterMode = procParam.getString("m"); //$NON-NLS-1$

			ObjectParameterDefinition parDef = new ObjectParameterDefinition(parameterName, parameterMode);
			parDef.readDataType(procParam);

			gxObject.getParameters().add(parDef);
		}

		return gxObject;
	}

	private static void loadResources(IContext context)
	{
		LanguageCatalog languages = new LanguageCatalog();
		INodeObject languagesFile = MetadataLoader.getDefinition(context, "languages"); //$NON-NLS-1$
		if (languagesFile != null)
			languages = LanguagesMetadataLoader.loadFrom(context, languagesFile);

		ImageCatalog images = new ImageCatalog();
		INodeObject imagesFile = MetadataLoader.getDefinition(context, "GXImages"); //$NON-NLS-1$
		if (imagesFile != null)
			images = ImagesMetadataLoader.loadFrom(context, imagesFile);

		Services.Resources.initialize(languages, images);
	}

	private static void loadAttributes(MetadataFile metadata) throws LoadException
	{
		try
		{
			for (INodeObject obj : metadata.getAttributes())
			{
				AttributeDefinition def = new AttributeDefinition(obj);
				Services.Application.putAttribute(def);
			}
		}
		catch (Exception ex)
		{
			throw LoadException.from("Attributes", ex);
		}
	}

	private static void loadThemes(IContext context) throws LoadException
	{
		String theTheme = PlatformHelper.calculateAppThemeName();
		if (!Services.Strings.hasValue(theTheme))
			return;

		INodeObject themes = MetadataLoader.getDefinition(context, "themes"); //$NON-NLS-1$
		if (themes != null)
		{
			try
			{
				INodeCollection arrThemes = themes.optCollection("Themes"); //$NON-NLS-1$
				for (int i = 0; i < arrThemes.length() ; i++)
				{
					INodeObject obj = arrThemes.getNode(i);
					String themeName = obj.optString("Name"); //$NON-NLS-1$
					if (themeName.length() > 0 && themeName.equalsIgnoreCase(theTheme))
					{
						ThemeDefinition def = readOneTheme(context, themeName);
						if (def != null)
							Services.Application.putTheme(def);
					}
				}
			}
			catch (Exception ex)
			{
				throw LoadException.from("Themes", ex);
			}
		}
	}

	private static ThemeDefinition readOneTheme(IContext context, String name)
	{
		INodeObject theme = MetadataLoader.getDefinition(context, Strings.toLowerCase(name) + ".theme"); //$NON-NLS-1$

		if (theme != null)
		{
			ThemeDefinition themeDef = new ThemeDefinition(name);

			INodeCollection arrStyles = theme.optCollection("Styles"); //$NON-NLS-1$
			for (int i = 0; i < arrStyles.length() ; i++)
			{
				INodeObject obj = arrStyles.getNode(i);
				ThemeClassDefinition classDef = readOneStyleAndChilds(themeDef, null, obj);
				themeDef.putClass(classDef);
			}

			for (INodeObject jsonTransformation : theme.optCollection("Transformations"))
			{
				TransformationDefinition transformation = new TransformationDefinition(jsonTransformation);
				themeDef.putTransformation(transformation);
			}

			return themeDef;
		}

		return null;
	}

	public static ThemeClassDefinition readOneStyleAndChilds(ThemeDefinition theme, ThemeClassDefinition parentClass, INodeObject styleJson)
	{
		String className = styleJson.getString("Name"); //$NON-NLS-1$
		ThemeClassDefinition themeClass = ThemeClassFactory.createClass(theme, className, parentClass);

		themeClass.setName(className);
		themeClass.deserialize(styleJson);

		INodeCollection arrStyles = styleJson.optCollection("Styles"); //$NON-NLS-1$
		for (int i = 0; i < arrStyles.length() ; i++)
		{
			INodeObject obj = arrStyles.getNode(i);

			ThemeClassDefinition classDef = readOneStyleAndChilds(theme, themeClass, obj);
			themeClass.getChildItems().add(classDef);
			theme.putClass(classDef);
		}

		return themeClass;
	}

	private static boolean initializeMain(GenexusApplication application)
	{
		IViewDefinition mainView = application.getDefinition().getView(application.getAppEntry());
		application.setMain(mainView);
		return (mainView != null);
	}

	private static void registerforNotification(IContext context)
	{
		Context appContext = MyApplication.getAppContext();

		// Notification only available if enabled them and if device is 2.2
		if (MyApplication.getApp().getUseNotification()
				&& Services.Device.isDeviceNotificationEnabled()
			&& GooglePlayServicesHelper.isPlayServicesAvailable(appContext)  // need google play services to work now
				)
		{
			if ( Services.Strings.hasValue(MyApplication.getApp().getNotificationSenderId())  )
			{
				//GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(appContext);
				String regid = DeviceRegister.getRegistrationId(appContext);

				//if already register we have a registration id
				if (Services.Strings.hasValue(regid))
				{
					//re register the device every run.
					// run in background.
					ReRegisterInServer();
				}
				else
				{
					DeviceRegister.registerDeviceInGCM();
				}

			}
			Services.Log.debug("registration id: " + DeviceRegister.getRegistrationId(appContext) );

		}
	}

	private static void ReRegisterInServer()
	{
		Thread thread = new Thread(null, doBackgroundProcessing,"Background"); //$NON-NLS-1$
		thread.start();
	}

	private static final Runnable doBackgroundProcessing = new Runnable(){
		@Override
		public void run()
		{
			Context appContext = MyApplication.getAppContext();
			DeviceRegister.registerWithServer(MyApplication.getAppContext(), DeviceRegister.getRegistrationId( appContext));
		}
	};


	private static boolean createApplicationDatabase(IContext context)
	{
		if (MyApplication.getApp().isOfflineApplication())
		{
			Services.Log.debug("Is OfflineApplication");
			// Create only if database file not exists.
			String filePath = AndroidContext.ApplicationContext.getDataBaseFilePath();
			File file = new File(filePath);

			//Time Stamp
			String reorTimeStamp = Application.getClientContext().getClientPreferences().getREORG_TIME_STAMP();
			String currentDBreorTimeStamp = MyApplication.getInstance().getStringPreference(REOR_VER_STAMP);

			//Md5 File
			String reorMd5Hash = MyApplication.getApp().getReorMD5Hash();
			String currentDBreorMd5Hash = MyApplication.getInstance().getStringPreference(REOR_MD5_HASH);

			if (!file.exists() ||
				(Services.Strings.hasValue(reorTimeStamp) && Services.Strings.hasValue(currentDBreorTimeStamp) && !reorTimeStamp.equalsIgnoreCase(currentDBreorTimeStamp) )
				&& (Services.Strings.hasValue(reorMd5Hash) && Services.Strings.hasValue(currentDBreorMd5Hash) && !reorMd5Hash.equalsIgnoreCase(currentDBreorMd5Hash) )
			)
				//if not exist, or exits an is an old version
				// and also a different reor.
			{
				//Services.Log.debug("Reor md5" + MyApplication.getApp().getReorMD5Hash());
				Services.Log.debug("Create database in: " + file.getAbsolutePath()); //$NON-NLS-1$
				Services.Log.debug("Reor Time Stamp: " + reorTimeStamp + " DB Time Stamp: " + currentDBreorTimeStamp); //$NON-NLS-1$ //$NON-NLS-2$
				Services.Log.debug("Reor MD5 Hash: " + reorMd5Hash + " DB MD5 Hash: " + currentDBreorMd5Hash); //$NON-NLS-1$ //$NON-NLS-2$

				EntityList pendingsEventsInDb = null;
				boolean executeReor= false;
				//if file exits
				if (file.exists())
				{
					Services.Log.debug("Creating new database, create backup of old database: " + file.getAbsolutePath()+ ".backup"); //$NON-NLS-1$ //$NON-NLS-2$
					try
					{
						FileUtils.copyFile(file, new File(file.getAbsolutePath() + ".backup")); //$NON-NLS-1$
					}
					catch (IOException e)
					{
						Services.Log.debug("Error backing up database.", e);
					}

					// keep pendings events if exists to restore after create.
					pendingsEventsInDb = SynchronizationHelper.getPendingEventsList("0"); //$NON-NLS-1$ // All

				}

				// create via reorganization or copy from raw if exist.

				//Copy Database from raw
				if (copyDatabaseFromRaw(file))
				{
					Services.Log.debug("End Copy file with database from raw : " + file.getAbsolutePath()); //$NON-NLS-1$

					//TODO: remove all files from blobs files directory?
				}
				else
				{
					Services.Log.debug("Running reor to create database in: " + file.getAbsolutePath()); //$NON-NLS-1$
					executeReor= true;
					// 	create via reorganization.
					GXReorganization reor = GxObjectFactory.getReorganization();
					if (reor != null)
					{
						reor.execute(); // Can be null if application does not use tables (rare, but possible).

						Services.Log.debug("Creating event table in database : " + file.getAbsolutePath()); //$NON-NLS-1$
						SynchronizationHelper.callReorCreatePendingEvents( false);

						//TODO: remove all files from blobs files directory?
					}
					else
					{
						// if reor is null and App has BCs show an error message.
						if (MyApplication.getInstance().hasBusinessComponents())
						{
							Services.Log.Error("Database creation failed: could not find Reorganization programs."); //$NON-NLS-1$
							return false;
						}
						//Only execute reor for pending events, just for apps with no tables.
						Services.Log.debug("Creating only event table in database : " + file.getAbsolutePath()); //$NON-NLS-1$
						SynchronizationHelper.callReorCreatePendingEvents( true);
					}
				}

				//reorganization mode to off., next connection will use correct autocommit mode.
				ApplicationContext.getInstance().setReorganization(false);

				// Set autocommit in false again, if properties say that.
				if (pendingsEventsInDb!=null && executeReor)
				{
					// drop before connection because it not useful any more, use a new one with correct auto commit mode
					int remoteHandle = Application.getNewRemoteHandle(ClientContext.getModelContext());
					//set this handle as app handle, store in as int in App. Use in all reflection calls
					MyApplication.getApp().setRemoteHandle(remoteHandle);
				}

				if (pendingsEventsInDb!=null && pendingsEventsInDb.size()>0)
				{
					// restore pending events saved before create database.
					Services.Log.debug("Restore previous pending events to new db"); //$NON-NLS-1$
					SynchronizationHelper.restorePendingToDatabase(pendingsEventsInDb);
				}

				// reor sucessfully, save time stamp.
				MyApplication.getInstance().setStringPreference(REOR_VER_STAMP, reorTimeStamp);
				MyApplication.getInstance().setStringPreference(REOR_MD5_HASH, reorMd5Hash);
			}
			else
			{
				//if file exits, check if database has Pending Events last version.
				if (file.exists())
				{
					Services.Log.debug("Check PendingEvents table in database : " + file.getAbsolutePath()); //$NON-NLS-1$

					try
					{
						// begin connection if not exists at app startup
						SynchronizationHelper.getPendingEventsList("1"); //$NON-NLS-1$ // Pending
					}
					catch (Exception ex)
					{
						// ignore error if PendingEventFiles att not exits.
					}
					//check for att
					String sqlSentToExecute = "PRAGMA table_info(GXPendingEvent)"; //$NON-NLS-1$

					PreparedStatement statement;
					try {
						LocalUtils.beginTransaction();

						statement = SQLDroidDriver.getCurrentConnection().prepareStatement(sqlSentToExecute);
						Services.Log.debug("Check PendingEvents Table atts."); //$NON-NLS-1$

						boolean hasFilesToSendAttribute = false;
						ResultSet resultSet = statement.executeQuery();
						while (resultSet.next())
						{
							String data = resultSet.getString("name"); //$NON-NLS-1$
							hasFilesToSendAttribute = data.equalsIgnoreCase("PendingEventFiles"); //$NON-NLS-1$
							if (hasFilesToSendAttribute)
								break;
						}
						statement.close();
						//
						//add it if necessary
						if (!hasFilesToSendAttribute)
						{
							Services.Log.debug("Add PendingEventFiles to PendingEvents Table."); //$NON-NLS-1$
							sqlSentToExecute = "ALTER TABLE [GXPendingEvent] ADD COLUMN [PendingEventFiles] TEXT NOT NULL DEFAULT ''";
							statement = SQLDroidDriver.getCurrentConnection().prepareStatement(sqlSentToExecute);
							statement.execute();
							statement.close();
							LocalUtils.commit();
						}

					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					finally
					{
						LocalUtils.endTransaction();
					}
					//end
				}
			}

			Services.Log.debug("Using database : " + file.getAbsolutePath()); //$NON-NLS-1$
			
			//Test get pending events
			//List<Entity> pendings = SynchronizationHelper.getPendingEventsList();
			//pendings.toString();
			//send Pending event temp: remove, do it if online and has pending event or by property
			//SynchronizationHelper.sendPendingsToServer();
		}
		return true;
	}

	private static boolean copyDatabaseFromRaw(File file)
	{
		InputStream is = AndroidContext.ApplicationContext.getResourceStream(MyApplication.getApp().getName()+ "_sqlite", "raw");

		if (is == null)
			is = AndroidContext.ApplicationContext.getResourceStream(MyApplication.getApp().getAppEntry().toLowerCase(Locale.US) + "_sqlite", "raw");

		if (is != null)
		{
			Services.Log.debug("Copy file with data database from raw to: " + file.getAbsolutePath()); //$NON-NLS-1$
			try
			{
				FileUtils.copyInputStreamToFile(is, file);
			}
			catch (IOException e)
			{
				Services.Log.error(e);
			}

			//if checksum files exist, copy to android and insert them.
			String fileCheckSumPath = AndroidContext.ApplicationContext.getDataBaseSyncHashesFilePath();
			File fileCheckSum = new File(fileCheckSumPath);
			is = AndroidContext.ApplicationContext.getResourceStream(MyApplication.getApp().getAppEntry().toLowerCase(Locale.US) + "_hashes", "raw");
			if (is !=null)
			{
				Services.Log.debug("Copy file with checksum database from raw to: " + fileCheckSum.getAbsolutePath()); //$NON-NLS-1$
				try
				{
					FileUtils.copyInputStreamToFile(is, fileCheckSum);
				}
				catch (IOException e)
				{
					Services.Log.error(e);
				}

				// read file and copy to check sum file
				// using readJsonFromDisk, convertJsonArraytoHash , storeHashMapOnDisk
				JSONArray array = SynchronizationHelper.readJsonArrayFromDisk();
				LinkedHashMap<String, String> hash = SynchronizationHelper.convertJsonArraytoHash(array);
				SynchronizationHelper.storeHashMapOnDisk(hash);

				// not set last sync time to now, could be a sync after pre load database
				//SynchronizationHelper.setSyncLastTime(new Date().getTime());

				//copy all blobs from assets/blobs to internal storage.
				AssetManager assetManager = MyApplication.getAppContext().getAssets();
				try
				{
					String[] files = assetManager.list("blobs"); //$NON-NLS-1$
					for(String filename : files)
					{
						InputStream in = assetManager.open("blobs/" + filename);  //$NON-NLS-1$
						String outputFile = AndroidContext.ApplicationContext.getFilesBlobsApplicationDirectory() + "/" + filename;
						File fileFromAssets = new File(outputFile);
						FileUtils.copyInputStreamToFile(in, fileFromAssets);
					}
				}
				catch (IOException e)
				{
					Services.Log.warning("createApplicationDatabase", "cannot get assets files " + e.getMessage()); //$NON-NLS-1$
				}
			}
			return true;
		}
		return false;
	}

	//static final Handler delayHandler = new Handler();
	private static NotificationCompat.Builder builder = null;

	private static void syncOfflineData()
	{
		//must be done after metadata is loaded and the version if the correct one.
		// should run onbackground.

		// calculate time dif
		long minTimeBetweenSync = MyApplication.getApp().getSynchronizerMinTimeBetweenSync();
		long nowTime = new Date().getTime();
		long lastSync = SynchronizationHelper.getSyncLastTime();
		boolean shouldRunSync = true;
		// minTimeBetweenSync in seconds
		if (lastSync!=0 && ((nowTime-lastSync) < (minTimeBetweenSync * 1000)))
		{
			shouldRunSync = false;
			if (MyApplication.getApp().isOfflineApplication())
				Services.Log.debug("MinTimeBetweenSync time not happened yet.");  //$NON-NLS-1$
		}
		//Run Sync if automatic, or elapsed time and not custom procedure.
		if (MyApplication.getApp().isOfflineApplication()
				&& (MyApplication.getApp().getRunSynchronizerAtStartup() ||
				(MyApplication.getApp().getSynchronizerReceiveAfterElapsedTime() && !Services.Strings.hasValue(MyApplication.getApp().getSynchronizerReceiveCustomProcedure()) ))
				&& shouldRunSync )
		{


			//Show notification
			builder = NotificationHelper.createOngoingNotification(Services.Strings.getResource(R.string.GXM_ReceivingData)
				, Strings.EMPTY /* Services.Strings.getResource(R.string.GXM_ReceivingProgress)*/, R.drawable.gx_stat_notify_sync);
			// check for data to sync

			try {

				// call MainSynchronizer
				Services.Log.debug("callSynchronizer (Sync.Receive) from Application load "); //$NON-NLS-1$
				int syncResult = SynchronizationHelper.callSynchronizer(true, true, false);

				// Only if result is 2 , restore dabase and reintent.
				if (syncResult==SynchronizationHelper.SYNC_FAIL_SERVERREINSTALL)
				{
					// Is empty and error is 2 we should re install preload database.
					// restore database and initial hashed.
					//Copy Database from raw
					String filePath = AndroidContext.ApplicationContext.getDataBaseFilePath();
					File file = new File(filePath);
					if (copyDatabaseFromRaw(file))
					{
						Services.Log.debug("Synchronizer failed , retry with app initial data.");
						// App to an initial state, re try sync
						syncResult = SynchronizationHelper.callSynchronizer(true, true, false);
					}
					else
					{
						//Empty hashes.
						Services.Log.debug("Synchronizer failed , retry without local tables hashes.");
						// call sync again, with no hashes.
						syncResult = SynchronizationHelper.callSynchronizer(true, false, false);
					}
				}

				boolean failed = syncResult!=SynchronizationHelper.SYNC_OK;

				NotificationHelper.updateOngoingNotification(builder, Services.Strings.getResource(R.string.GXM_ReceivingData),
					failed?Services.Strings.getResource(R.string.GXM_ReceptionFailed):Services.Strings.getResource(R.string.GXM_ReceptionCompleted),
					failed?R.drawable.gx_stat_notify_sync_error:R.drawable.gx_stat_notify_sync_ok);
				//	End show loading in current activity, how?.

				if (failed)
				{
					// change failed message to notification
					//NotificationHelper.changeNotOngoingNotification(builder, true);
					// keep sync failed for 10 secs.
					Services.Device.postOnUiThreadDelayed(new Runnable() {
						  @Override
						  public void run() {
							  NotificationHelper.closeOngoingNotification(builder);
						  }
						}, 10000);

				}
				else
				{
					// keep sync successfully for 5 secs.
					Services.Device.postOnUiThreadDelayed(new Runnable() {
						  @Override
						  public void run() {
							  NotificationHelper.closeOngoingNotification(builder);
						  }
						}, 5000);
				}

			} catch (Exception e) {
				e.printStackTrace();
				// close notification.
				NotificationHelper.closeOngoingNotification(builder);
			}
			finally{
				//hide indicator, now do it in synchonizer itselft.
			}

		}

		if (MyApplication.getApp().isOfflineApplication()
				&& MyApplication.getApp().getSynchronizerReceiveAfterElapsedTime() )
		{
			//if offline , sync auto and sync by time (After Elapsed Time)
			//Schedule Sync Receiver.
			SynchronizationAlarmReceiver alarm = new SynchronizationAlarmReceiver();
			alarm.SetAlarm(MyApplication.getAppContext());
			Services.Log.debug("set sync alarm after elapsed time");

		}

	}

	private static void initLocationServices()
	{
		Boolean hasReadLocationPermission = PackageManager.PERMISSION_GRANTED == MyApplication.getInstance().checkCallingOrSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION);
		if (hasReadLocationPermission)
			LocationHelper.createFusedLocationHelper();
	}

}