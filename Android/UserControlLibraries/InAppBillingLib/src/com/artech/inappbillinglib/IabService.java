package com.artech.inappbillinglib;

import android.util.Log;

import com.artech.application.MyApplication;
import com.artech.base.services.Services;
import com.artech.inappbillinglib.util.IabHelper;
import com.artech.inappbillinglib.util.IabResult;

public class IabService {
	private static final String TAG = "IabService";
	private static IabHelper sIabHelper = null;
	
	/**
	 * Returns the singleton instance of IabHelper. Its initialization is done
	 * on first usage and returns it only after the setup has completed successfully.
	 * 
	 * @return the ready to use IabHelper singleton.
	 * @throws IllegalStateException if it fails to setup the IabHelper.
	 */
	public static IabHelper getInstance() throws IllegalStateException {
		synchronized (IabService.class) {
			if (sIabHelper == null) {
				sIabHelper = new IabHelper(MyApplication.getAppContext(), MyApplication.getApp().getInAppBillingPublicKey());
				setupSync();
			}
			return sIabHelper;
		}
	}
	
	/**
	 * Does the IabHelper synchronously.
	 * Must be called from within a synchronized block.
	 */
	private static void setupSync() throws IllegalStateException {
		sIabHelper.startSetup(mIabSetupFinishedListener);
		if (sIabHelper != null) {
			try {
				IabService.class.wait();
			} catch (InterruptedException e) {
				sIabHelper = null;
			}
		}
		if (sIabHelper == null) {
			throw new IllegalStateException("IAB helper setup failed.");
		}
	}
	
	private static IabHelper.OnIabSetupFinishedListener mIabSetupFinishedListener = new IabHelper.OnIabSetupFinishedListener() {
		
		@Override
		public void onIabSetupFinished(IabResult result) {
			Log.d(TAG, result.isSuccess() ?
					"IabHelper init succeded." :
					"IabHelper init failed. Reason: " + result.getMessage());
			if (result.isFailure() && result.getResponse() == IabHelper.BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE) {
				MyApplication.getInstance().showMessage(
						Services.Strings.getResource(R.string.common_google_play_services_needs_enabling_title));
			}
			synchronized (IabService.class) {
				if (result.isFailure()) {
					sIabHelper = null;
				}
				IabService.class.notify();
			}
		}
	};
	
    public static void dispose() {
    	synchronized (IabService.class) {
    		if (sIabHelper != null) {
    			sIabHelper.dispose();
    			sIabHelper = null;
    			Log.d(TAG, "IabHelper disposed.");
    		}
		}
    }
}
