package com.artech.actions;

import com.artech.base.metadata.ActionDefinition;
import com.artech.base.services.Services;
import com.artech.base.synchronization.SynchronizationHelper;
import com.artech.base.synchronization.SynchronizationSendHelper;

public class SyncronizationAction extends Action
{
	private final String mExecuteNamespace;
	private final String mExecuteMethod;
	private final String mReturnValue;
	
	private static final String EXECUTE_NAMESPACE = "@executeNamespace";
	private static final String EXECUTE_METHOD = "@executeMethod";

	private static final String SEND_METHOD = "send";
	private static final String RECEIVE_METHOD = "receive";
	private static final String STATUS_METHOD = "serverstatus";
	
	protected SyncronizationAction(UIContext context, ActionDefinition definition, ActionParameters parameters)
	{
		super(context, definition, parameters);
		mExecuteNamespace = definition.optStringProperty(EXECUTE_NAMESPACE);
		mExecuteMethod = definition.optStringProperty(EXECUTE_METHOD);
		mReturnValue = definition.optStringProperty("@returnValue"); //$NON-NLS-1$
	}

	public static boolean isAction(ActionDefinition definition)
	{
		return (Services.Strings.hasValue(definition.optStringProperty(EXECUTE_NAMESPACE))
				&& Services.Strings.hasValue(definition.optStringProperty(EXECUTE_METHOD)));
	}

	@Override
	public boolean Do()
	{
		Object result = null;
		//Call Send or receive
		
		Services.Log.debug(" Sync method " + mExecuteNamespace + " " + mExecuteMethod);
		if (mExecuteMethod.equalsIgnoreCase(SEND_METHOD))
		{
			Services.Log.debug("callOfflineReplicator (Sync.Send) from Action Do "); //$NON-NLS-1$
			result = SynchronizationSendHelper.callOfflineReplicator();
		}
		if (mExecuteMethod.equalsIgnoreCase(RECEIVE_METHOD))
		{
			Services.Log.debug("callSynchronizer (Sync.Receive) from Action Do "); //$NON-NLS-1$
			result = SynchronizationHelper.callSynchronizer(false, true, true);
		}
		if (mExecuteMethod.equalsIgnoreCase(STATUS_METHOD))
		{
			result = SynchronizationHelper.callSynchronizerCheck();
		}
		
		// Check return value
		if (result != null)
			setOutputValue(mReturnValue, result);

		return true;
	}
}
