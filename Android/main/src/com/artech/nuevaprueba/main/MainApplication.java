package com.artech.nuevaprueba.main;

import com.artech.android.ContextImpl;
import com.artech.application.MyApplication;
import com.artech.base.metadata.GenexusApplication;
import com.artech.base.services.AndroidContext;
import com.artech.base.services.IGxProcedure;
import com.artech.base.services.Services;
import com.artech.providers.EntityDataProvider;
import com.artech.controls.ads.Ads;
import com.artech.nuevaprueba.main.controls.*;
import com.genexus.Application;
import com.genexus.ClientContext;
public class MainApplication extends MyApplication
{
	@Override
	public final void onCreate()
	{
		GenexusApplication application = new GenexusApplication();
		application.setName("nuevaprueba");
		application.setAPIUri("http://52.34.247.215/frameWorkExamples/taskManager/service/");
		application.setAppEntry("main");
		application.setMajorVersion(1);
		application.setMinorVersion(0);

		// Extensibility Point for Logging
 

		// Security
		application.setIsSecure(false);
		application.setEnableAnonymousUser(false);
		application.setClientId("");
		application.setSecret("");
		application.setLoginObject("");
		application.setNotAuthorizedObject("");
		application.setChangePasswordObject("");
		//application.setCompleteUserDataObject("");

		// Dynamic Url		
		application.setUseDynamicUrl(false);
		application.setDynamicUrlAppId("nuevaPrueba");

		// Ads
		application.setUseAds(false);
		application.setAdMobPublisherId("");

		// Notifications
		application.setUseNotification(false);
		application.setNotificationSenderId("");
		application.setNotificationRegistrationHandler("(none)");



		// Testing
		application.setUseTestMode(false);

		MyApplication.setApp(application);
		UserControls.initializeUserControls();

		super.onCreate();

		
		AndroidContext.ApplicationContext = new ContextImpl(getApplicationContext());
    }

	@Override
	public Class<? extends com.artech.services.EntityService> getEntityServiceClass()
	{
		return AppEntityService.class;
	}

	@Override
	public Class<? extends com.artech.android.audio.AudioService> getAudioServiceClass()
	{
		return AppAudioService.class;
	}

	@Override
	public Class<? extends com.artech.android.audio.AudioIntentReceiver> getAudioIntentReceiverClass()
	{
		return AppAudioIntentReceiver.class;
	}

	@Override
	public EntityDataProvider getProvider()
	{
		return new AppEntityDataProvider();
	}

}