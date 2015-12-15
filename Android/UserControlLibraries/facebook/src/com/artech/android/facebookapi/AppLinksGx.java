package com.artech.android.facebookapi;

import android.content.Intent;
import android.net.Uri;
import bolts.AppLinks;

import com.artech.actions.DynamicCallAction;
import com.artech.actions.UIContext;
import com.artech.activities.IIntentHandler;
import com.artech.base.model.Entity;
import com.artech.base.services.Services;
import com.facebook.Settings;

public class AppLinksGx implements IIntentHandler {

	@Override
	public boolean tryHandleIntent(UIContext context, Intent intent, Entity entity) {
		   Settings.sdkInitialize(context); 
		    Uri targetUrl =
		       AppLinks.getTargetUrlFromInboundIntent(context, intent);
		    if (targetUrl != null) {
		    	int iStartTarget = intent.getDataString().lastIndexOf("?"); 
		    	String myLink = intent.getDataString().substring(0, iStartTarget);
		    	// At this moment we are just supporting linking but no actions so we just create a DynamicCallAction and execute it
		    	String dynamicAll = createDynamicCallFromTargetUrl(myLink);
		    	DynamicCallAction.redirect(context, null, dynamicAll);
		    }
		return false;
	}

	private String createDynamicCallFromTargetUrl(String targetUrl) {
		String protocolHandler = String.format("%s://", Services.Application.getAppsLinksProtocol());
		targetUrl = targetUrl.replace(protocolHandler, "sd:");
		// Now is not coming encoded so here we support for  "Obj.Detal?123,124"  = "Obj-Detail---123--124"
		targetUrl = targetUrl.replace("---", "?");
		targetUrl = targetUrl.replace("--", ",");
		targetUrl = targetUrl.replace("-", ".");
		return targetUrl;
	}

	

}
