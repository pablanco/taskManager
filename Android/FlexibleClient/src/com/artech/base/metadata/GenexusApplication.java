package com.artech.base.metadata;

import com.artech.base.services.Services;
import com.artech.base.services.UriBuilder;
import com.artech.base.synchronization.SynchronizationHelper.DataSyncCriteria;
import com.artech.base.synchronization.SynchronizationHelper.LocalChangesProcessing;
import com.artech.base.utils.Strings;

public class GenexusApplication
{
	private ApplicationDefinition mDefinition = new ApplicationDefinition();

	private String mName = "ApplicationName"; //$NON-NLS-1$
	private boolean mIsOfflineApplication = false;
	private boolean mUseInternalStorageForDatabase = true;
	private String mReorMD5Hash = "";
	private int mRemoteHandler = -1;
	private String mSynchronizerReceiveCustomProcedure = Strings.EMPTY;

	private String mBaseAPIUri;
	private String mAppEntry;
	private boolean mIsSecure = false;
	private int mServerType = UriBuilder.RUBY_SERVER;
	private String mAppId = Strings.EMPTY;
	private String mClientId = Strings.EMPTY;
	private String mSecret = Strings.EMPTY;
	private boolean mEnableAnonymousUser = false;
	private String mLoginObject = Strings.EMPTY;
	private String mNotAuthorizedObject = Strings.EMPTY;
	private String mChangePasswordObject = Strings.EMPTY;
	private boolean mUseDynamicUrl = false;
	private String mDynamicUrlAppId = Strings.EMPTY;
	private int mMajorVersion = -1;
	private int mMinorVersion = 0;
	private boolean mAutomaticUpdate = false;

	private IViewDefinition mMainView;
	private InstanceProperties mMainProperties;

	//remove default AdMob publisher id
	//private String mAdMobPublisherId = "a14ef0c02f2aea9"; //$NON-NLS-1$
	private String mAdMobPublisherId = ""; //$NON-NLS-1$

	private boolean mUseAds = false;

	private String mNotificationSenderId = Strings.EMPTY;
	private boolean mUseNotification = false;
	private String mNotificationRegistrationHandler = Strings.EMPTY;

	private String mInAppBillingPublicKey = Strings.EMPTY;
	private boolean mUseInAppBilling = false;

	private boolean mUseTestMode = false;
	
	private boolean mAllowNotTrustedCertificate = false;

	public String getName() { return mName; }
	public void setName(String value) { mName = value; }

	public ApplicationDefinition getDefinition()
	{
		return mDefinition;
	}

	public void resetDefinition()
	{
		mDefinition = new ApplicationDefinition();
	}

	public String getAppId() { return mAppId; }
	public void setAppId(String value) { mAppId = value; }

	public int getServerType() { return mServerType; }
	public void setServerType(int value) { mServerType = value; }

	//Dynamic Url
	public boolean getUseDynamicUrl() { return mUseDynamicUrl; }
	public void setUseDynamicUrl(boolean value) { mUseDynamicUrl = value; }
	public String getDynamicUrlAppId() { return mDynamicUrlAppId; }
	public void setDynamicUrlAppId(String value) { mDynamicUrlAppId = value; }

	// Uri Maker
	public UriBuilder UriMaker = new UriBuilder();

	// Metadata
	public IViewDefinition getMain() { return mMainView; }

	public void setMain(IViewDefinition value)
	{
		mMainView = value;
		mMainProperties = null;
	}

	public InstanceProperties getMainProperties()
	{
		if (mMainView != null)
			return mMainView.getInstanceProperties();
		else
			return mMainProperties;
	}

	public void setMainProperties(InstanceProperties properties)
	{
		mMainProperties = properties;
	}

	public String getAPIUri() { return mBaseAPIUri; }
	public void setAPIUri(String value) { mBaseAPIUri = value; }

	public String getAppEntry() { return mAppEntry; }
	public void setAppEntry(String value) { mAppEntry = value; }

	//Security
	public boolean isSecure() { return mIsSecure; }
	public void setIsSecure(boolean value) { mIsSecure = value; }

	public String getClientId() { return mClientId; }
	public void setClientId(String value) { mClientId = value; }

	public String getSecret() { return mSecret; }
	public void setSecret(String value) { mSecret = value; }

	public boolean getEnableAnonymousUser() { return mEnableAnonymousUser; }
	public void setEnableAnonymousUser(boolean value) { mEnableAnonymousUser = value; }

	public void setLoginObject(String value)
	{
		mLoginObject = value;
	}

	public String getLoginObject()
	{
		if (Services.Strings.hasValue(mLoginObject))
			return mLoginObject;

		return "gamsdlogin"; //$NON-NLS-1$
	}

	public String getNotAuthorizedObject() { return mNotAuthorizedObject; }
	public void setNotAuthorizedObject(String value) { mNotAuthorizedObject = value; }

	public String getChangePasswordObject() { return mChangePasswordObject; }
	public void setChangePasswordObject(String value) { mChangePasswordObject = value; }

	public String link(String objName)
	{
		return UriMaker.link(objName, mServerType, true);
	}

	public String linkObjectUrl(String objPartialUrl)
	{
		return UriMaker.link(objPartialUrl, mServerType, false);
	}

	// Update
	public int getMajorVersion() { return mMajorVersion; }
	public void setMajorVersion(int value) { mMajorVersion = value; }
	public int getMinorVersion() { return mMinorVersion; }
	public void setMinorVersion(int value) { mMinorVersion = value; }
	public boolean isAutomaticUpdate() { return mAutomaticUpdate; }
	public void setAutomaticUpdate(boolean value) { mAutomaticUpdate = value; }

	// Ads
	public String getAdMobPublisherId() { return mAdMobPublisherId; }

	public void setAdMobPublisherId(String publisherId)
	{
		if (publisherId != null && publisherId.length() != 0)
			mAdMobPublisherId = publisherId;
	}

	public void setUseAds(boolean value) { mUseAds = value; }
	public boolean getUseAds() { return mUseAds; }

	// Notifications
	public boolean getUseNotification() { return mUseNotification; }
	public void setUseNotification(boolean value) { mUseNotification = value; }
	public String getNotificationSenderId() { return mNotificationSenderId; }
	public void setNotificationSenderId(String value) { mNotificationSenderId = value; }
	public String getNotificationRegistrationHandler() { return mNotificationRegistrationHandler; }
	public void setNotificationRegistrationHandler(String value) { mNotificationRegistrationHandler = value; }

	// Https validation
	public boolean getAllowNotTrustedCertificate() { return mAllowNotTrustedCertificate; }
	public void setAllowNotTrustedCertificate(boolean value) { mAllowNotTrustedCertificate = value; }

	// In App Billing
	public boolean getUseInAppBilling() { return mUseInAppBilling; }
	public void setUseInAppBilling(boolean value) { mUseInAppBilling = value; }
	public String getInAppBillingPublicKey() { return mInAppBillingPublicKey; }
	public void setInAppBillingPublicKey(String value) { mInAppBillingPublicKey = value; }

	// Testing
	public boolean getUseTestMode() { return mUseTestMode; }
	public void setUseTestMode(boolean value) { mUseTestMode = value; }
	
	// Offline applications.
	public boolean isOfflineApplication() { return mIsOfflineApplication; }
	public void setIsOfflineApplication(boolean value) { mIsOfflineApplication = value; }

	public boolean getUseInternalStorageForDatabase() { return mUseInternalStorageForDatabase; }
	public void setUseInternalStorageForDatabase(boolean value) { mUseInternalStorageForDatabase = value; }

	//reor md5
	public String getReorMD5Hash() { return mReorMD5Hash; }
	public void setReorMD5Hash(String value) { mReorMD5Hash = value; }

	// Sync Send
	public boolean getSynchronizerSendAutomatic()
	{
		return getSendLocalChangesProcessing() == LocalChangesProcessing.WhenConnected;
	}

	// Sync Save pending events
	public boolean getSynchronizerSavePendingEvents()
	{
		return getSendLocalChangesProcessing() != LocalChangesProcessing.Never;
	}

	// Sync Receive After elapsed time
	public boolean getSynchronizerReceiveAfterElapsedTime()
	{
		return getSynchronizerDataSyncCriteria() == DataSyncCriteria.AfterElapsedTime;
	}

	// Sync Receive custom proc
	public String getSynchronizerReceiveCustomProcedure() { return mSynchronizerReceiveCustomProcedure; }
	public void setSynchronizerReceiveCustomProcedure(String value) { mSynchronizerReceiveCustomProcedure = value; }

	public boolean getRunSynchronizerAtStartup()
	{
		return getSynchronizerDataSyncCriteria() == DataSyncCriteria.Automatic;
		//return mRunSynchronizerAtStartup;
	}

	//public void setRunSynchronizerAtStartup(boolean value) { mRunSynchronizerAtStartup = value; }

	public String getSynchronizer()
	{
		if (getMain() != null)
		{
			if (getMain() instanceof IDataViewDefinition)
				return ((IDataViewDefinition)getMain()).getPattern().getInstanceProperties().getSynchronizer();
			else if (getMain() instanceof DashboardMetadata)
				return getMain().getInstanceProperties().getSynchronizer();
		}

		return null;
	}

	public DataSyncCriteria getSynchronizerDataSyncCriteria()
	{
		if (getMain() != null)
		{
			if (getMain() instanceof IDataViewDefinition)
				return ((IDataViewDefinition)getMain()).getPattern().getInstanceProperties().getSynchronizerDataSyncCriteria();
			else if (getMain() instanceof DashboardMetadata)
				return getMain().getInstanceProperties().getSynchronizerDataSyncCriteria();
		}
		return DataSyncCriteria.Manual;
	}

	public long getSynchronizerMinTimeBetweenSync()
	{
		if (getMain() != null)
		{
			if (getMain() instanceof IDataViewDefinition)
				return ((IDataViewDefinition)getMain()).getPattern().getInstanceProperties().getSynchronizerMinTimeBetweenSync();
			else if (getMain() instanceof DashboardMetadata)
				return getMain().getInstanceProperties().getSynchronizerMinTimeBetweenSync();
		}
		return 0;
	}

	public LocalChangesProcessing getSendLocalChangesProcessing()
	{
		if (getMain() != null)
		{
			if (getMain() instanceof IDataViewDefinition)
				return ((IDataViewDefinition)getMain()).getPattern().getInstanceProperties().getSendLocalChangesProcessing();
			else if (getMain() instanceof DashboardMetadata)
				return getMain().getInstanceProperties().getSendLocalChangesProcessing();
		}
		return LocalChangesProcessing.UserDefined;
	}

	public long getSynchronizerMinTimeBetweenSends()
	{
		if (getMain() != null)
		{
			if (getMain() instanceof IDataViewDefinition)
				return ((IDataViewDefinition)getMain()).getPattern().getInstanceProperties().getSynchronizerMinTimeBetweenSends();
			else if (getMain() instanceof DashboardMetadata)
				return getMain().getInstanceProperties().getSynchronizerMinTimeBetweenSends();
		}
		return 0;
	}
	
	public int getRemoteHandle() { return mRemoteHandler; }
	public void setRemoteHandle(int value) { mRemoteHandler = value; }
}
