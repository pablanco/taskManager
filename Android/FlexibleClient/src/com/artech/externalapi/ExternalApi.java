package com.artech.externalapi;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.artech.actions.ApiAction;
import com.artech.actions.UIContext;
import com.artech.base.metadata.ActionDefinition;
import com.artech.base.utils.SafeBoundsList;
import com.artech.base.utils.Strings;

public abstract class ExternalApi
{
	private String mDebugTag = null;
	private ApiAction mAction;

	public void setDebugTag(String tag) { mDebugTag = tag; }
	public void setAction(ApiAction action) { mAction = action; }

	public ApiAction getAction() { return mAction; }
	public ActionDefinition getDefinition() { return mAction.getDefinition(); }
	public UIContext getContext() { return mAction.getContext(); }
	public Activity getActivity() { return mAction.getMyActivity(); }
	
	private HashMap<HashKey, IMethodInvoker> mHandlers = new HashMap<>();
	
	/**
	 * Utility method to convert parameter values from objects to strings.
	 * Should be used at the start of execute() if only string values are to be received.
	 */
	protected static SafeBoundsList<String> toString(List<Object> values)
	{
		SafeBoundsList<String> strValues = new SafeBoundsList<>();
		for (Object obj : values)
			strValues.add(obj != null ? obj.toString() : Strings.EMPTY);

		return strValues;
	}

	public @NonNull	ExternalApiResult execute(String method, List<Object> parameters)
	{
		return invokeMethod(method, parameters);
	}

	public ExternalApiResult afterActivityResult(int requestCode, int resultCode, Intent result, String method)
	{
		return null;
	}

	/**
	 * Register a IMethodInvoker to handle a particular method of the External Object.
	 */
	protected void addMethodHandler(String method, int argsCount, IMethodInvoker handler)
	{
		mHandlers.put(new HashKey(method, argsCount), handler);
	}

	/**
	 * Register a ISimpleMethodInvoker to handle a particular method of the External Object.
	 * Unlike addMethodHandler, this method doesn't have to return an ExternalApiResult, and
	 * is assumed to always succeed.
	 */
	protected void addSimpleMethodHandler(String method, int argsCount, final ISimpleMethodInvoker handler)
	{
		// Use a wrapper to map a "simple" output to ExternalApiResult.
		mHandlers.put(new HashKey(method, argsCount), new IMethodInvoker()
		{
			@Override
			public @NonNull ExternalApiResult invoke(List<Object> parameters)
			{
				Object returnValue = handler.invoke(parameters);
				return ExternalApiResult.success(returnValue);
			}
		});
	}

	protected @NonNull ExternalApiResult invokeMethod(String method, List<Object> parameters)
	{
		if (!TextUtils.isEmpty(mDebugTag))
			logMethodInvocation(mDebugTag, method, parameters);

		IMethodInvoker handler = mHandlers.get(new HashKey(method, parameters.size()));

		if (handler != null)
			return handler.invoke(parameters);
		else
			return ExternalApiResult.failureUnknownMethod(this, method);
	}
	
	protected @NonNull ExternalApiResult invokeMethod(String method)
	{
	    return invokeMethod(method, Collections.emptyList());
	}
	
	protected interface IMethodInvoker
	{
		@NonNull ExternalApiResult invoke(List<Object> parameters);
	}

	protected interface ISimpleMethodInvoker
	{
		Object invoke(List<Object> parameters);
	}

	protected void logMethodInvocation(String tag, String method, List<Object> params)
	{
		String message = "CALL\n    " + method + "\nPARAMETERS\n";
		if (params.isEmpty()) {
			message += "    None\n";
		} else {
			for (Object param : params) {
				message += "    " + String.valueOf(param) + "\n";
			}
		}
		Log.d(tag, message);
	}

	private class HashKey
	{
	    private final String mMethodName;
	    private final int mArgsCount;
	    private final int mHashCode;
	    
        public HashKey(String methodName, int argsCount)
	    {
            mMethodName = methodName;
            mArgsCount = argsCount;
	        mHashCode = methodName.toLowerCase(Locale.US).hashCode() * 31 + argsCount;
	    }
        
        @Override
        public boolean equals(Object o)
        {
            if (!(o instanceof HashKey))
                return false;

            HashKey other = (HashKey) o;
            return this.mArgsCount == other.mArgsCount && this.mMethodName.equalsIgnoreCase(other.mMethodName);
        }
        
        @Override
        public int hashCode()
        {
            return mHashCode;
        }
	}
}
