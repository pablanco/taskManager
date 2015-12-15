package com.artech.layers;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.artech.android.json.NodeCollection;
import com.artech.android.json.NodeObject;
import com.artech.application.MyApplication;
import com.artech.base.application.IProcedure;
import com.artech.base.application.OutputResult;
import com.artech.base.metadata.ObjectParameterDefinition;
import com.artech.base.metadata.ProcedureDefinition;
import com.artech.base.model.Entity;
import com.artech.base.model.EntityList;
import com.artech.base.model.PropertiesObject;
import com.artech.base.serialization.INodeCollection;
import com.artech.base.serialization.INodeObject;
import com.artech.base.services.ServiceResponse;
import com.artech.base.services.Services;
import com.artech.common.ServiceHelper;

class RemoteProcedure implements IProcedure
{
	private final String mName;
	private final ProcedureDefinition mDefinition;

	public RemoteProcedure(String name, ProcedureDefinition definition)
	{
		mName = name;
		mDefinition = definition;
	}

	@Override
	public OutputResult execute(PropertiesObject parameters)
	{
		if (mDefinition == null)
			return RemoteUtils.outputNoDefinition(mName);

		String url = MyApplication.getApp().UriMaker.MakeGetAllUriBC(mDefinition.getName());
		try
		{
			// Prepare input (encode into JSON content).
			JSONObject jsonParameters = prepareProcedureInput(parameters);

			// Execute
			ServiceResponse response = ServiceHelper.postJson(url, jsonParameters);

			// Read output parameters from server response.
			readProcedureOutput(response, parameters);

			// Return errors and/or messages, if any.
			return RemoteUtils.translateOutput(response);
		}
		catch (IOException e)
		{
			return OutputResult.error(Services.HttpService.getNetworkErrorMessage(e));
		}
	}

	@Override
	public OutputResult executeMultiple(List<PropertiesObject> parameters)
	{
		if (mDefinition == null)
			return RemoteUtils.outputNoDefinition(mName);

		// The URL is of the form <server>/gxmulticall?<procName>
		String url = MyApplication.getApp().UriMaker.makeMultiCallUri(mDefinition.getName());

		try
		{
			// Prepare input (encode into JSON content).
			JSONArray jsonArray = prepareMultipleCallInput(parameters);

			// Perform the multicall.
			ServiceResponse response = ServiceHelper.postJson(url, jsonArray);

			// Server call does not support output parameters; just read response for any errors.
			return RemoteUtils.translateOutput(response);
		}
		catch (IOException e)
		{
			return OutputResult.error(Services.HttpService.getNetworkErrorMessage(e));
		}
	}

	private JSONObject prepareProcedureInput(PropertiesObject parameters)
	{
		JSONObject jsonParameters = new JSONObject();
		for (Map.Entry<String, Object> parameter : parameters.getInternalProperties().entrySet())
		{
			try
			{
				//Services.Log.debug("ProcCall", "Input " + parameter.getKey() + " " + toJsonValue(parameter.getValue()) );
				jsonParameters.put(parameter.getKey(), toJsonValue(parameter.getValue()));
			}
			catch (JSONException e)
			{
				Services.Log.Error("putParameter", "Exception in JSONObject.put()", e);  //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		return jsonParameters;
	}

	private JSONArray prepareMultipleCallInput(List<PropertiesObject> parameters)
	{
		// Post body is values, a JSON array comprising one array for each item, e.g.
		// [["Item1Parm1", "Item1Parm2", "Item1Parm3"], ["Item2Parm1", "Item2Parm2", "Item2Parm3"]].
		JSONArray array = new JSONArray();
		for (PropertiesObject itemValues : parameters)
		{
			JSONArray item = new JSONArray();

			// Order them the same as in the procedure parameters.
			for (int i = 0; i < mDefinition.getParameters().size(); i++)
			{
				ObjectParameterDefinition procParameter = mDefinition.getParameter(i);
				if (procParameter.isInput())
				{
					Object itemValue = itemValues.getProperty(procParameter.getName());
					item.put(itemValue);
				}
			}

			array.put(item);
		}

		return array;
	}

	private static Object toJsonValue(Object value)
	{
		if (value == null)
			return JSONObject.NULL;

		// Convert to "real" JSON if it's a structure.
		if (value instanceof Entity)
		{
			INodeObject json = ((Entity)value).serialize();
			return ((NodeObject)json).getInner();
		}

		// Convert to "real" JSON if it's a structured collection.
		if (value instanceof EntityList)
		{
			INodeCollection json = ((EntityList)value).serialize();
			return ((NodeCollection)json).getInner();
		}

		// Atomic or unknown type.
		return value;
	}

	private void readProcedureOutput(ServiceResponse response, PropertiesObject parameters)
	{
		// Read output parameters from JSON response and assign them to parameters collection.
		if (response.Data != null)
		{
			for (int i = 0; i < mDefinition.getParameters().size(); i++)
			{
				ObjectParameterDefinition procParameter = mDefinition.getParameter(i);
				if (procParameter.isOutput())
				{
					// Read result parameter from procedure.
					String value = response.get(procParameter.getName());
					//Services.Log.debug("ProcCall", "Output: " + procParameter.getName() + " "+ value );
					parameters.setProperty(procParameter.getName(), value);
				}
			}
		}
	}
}
