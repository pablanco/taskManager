package com.artech.base.metadata;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;

import com.artech.base.model.Entity;
import com.artech.base.services.Services;

public class DynamicCallDefinition
{
	// TODO: Move real parsing from DynamicCallAction to here. Add getObject(), getParameters(), &c.
	private final String mCallString;

	private DynamicCallDefinition(String callString)
	{
		mCallString = callString;
	}

	public static List<DynamicCallDefinition> from(Entity data)
	{
		List<DynamicCallDefinition> calls = new ArrayList<DynamicCallDefinition>();

		if (data != null)
		{
			String strCalls = data.optStringProperty("Gxdyncall"); //$NON-NLS-1$
			if (Services.Strings.hasValue(strCalls))
			{
				try
				{
					JSONArray jsonCalls = new JSONArray(strCalls);
					for (int i = 0; i < jsonCalls.length() ; i++)
					{
						String strCall = jsonCalls.getString(i);
						DynamicCallDefinition call = new DynamicCallDefinition(strCall);
						calls.add(call);
					}
				}
				catch (JSONException e)
				{
					Services.Log.Error("Error processing Gxdyncall.", e); //$NON-NLS-1$
				}
			}
		}

		return calls;
	}

	public String getCallString()
	{
		return mCallString;
	}
}
