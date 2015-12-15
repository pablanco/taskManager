package com.artech.base.metadata.enums;

import java.io.Serializable;
import java.security.InvalidParameterException;

import android.content.Intent;
import android.os.Bundle;

import com.artech.activities.IntentParameters;
import com.artech.base.metadata.IDataViewDefinition;
import com.artech.base.metadata.IViewDefinition;
import com.artech.base.services.Services;

public enum Connectivity {
	 Online, 
	 Offline,
	 Inherit;
	 
	 
	 public static Connectivity getConnectivitySupport(Connectivity callerSupport, Connectivity calledSupport)
	 	throws InvalidParameterException {
		 if (callerSupport == Connectivity.Inherit)
			 throw new InvalidParameterException("Connectivity of caller must be know at this time"); //$NON-NLS-1$
		 if (calledSupport == Connectivity.Inherit)
			 return callerSupport;
		 return calledSupport;
	 }
	 
	 public static Connectivity getConnectivitySupport(Intent intent, Connectivity calledSupport) throws Exception {
		 Connectivity connectivity = fromIntent(intent);
		 if (connectivity != null) {
			 return getConnectivitySupport(connectivity, calledSupport);
		 }
		 Services.Log.debug("SOMETHING GOES WRONG, Context without connectivity support type, assuming online");
		 return getConnectivitySupport(Connectivity.Online, calledSupport);
	 }

	public static Connectivity getConnectivitySupport(Intent intent, IViewDefinition dataView) {
		try {
			return Connectivity.getConnectivitySupport(intent, dataView.getConnectivitySupport());
		}
		catch (Exception e) {
			Services.Log.error(e);
			return Connectivity.Online;
		}	
	}

	public static Connectivity fromIntent(Intent intent) {
		return fromBundle(intent.getExtras());
	}
	
	public static Connectivity fromBundle(Bundle bundle) {
		if (bundle!=null)
		{
			Serializable connectivity = bundle.getSerializable(IntentParameters.Connectivity);
			if (connectivity != null) {
				return ((Connectivity) connectivity);
			}
		}
		return (com.artech.application.MyApplication.getApp().isOfflineApplication())? Connectivity.Offline : Connectivity.Online;
	}

	public static Connectivity getConnectivitySupport(Bundle bundle,
			IDataViewDefinition view) {
		 Connectivity connectivity = fromBundle(bundle);
		 if (connectivity != null) {
			 return getConnectivitySupport(connectivity, view.getConnectivitySupport());
		 }
		 Services.Log.debug("SOMETHING GOES WRONG, Context without connectivity support type, assuming online");
		 return getConnectivitySupport(Connectivity.Online, view.getConnectivitySupport());
	}
	 
}
