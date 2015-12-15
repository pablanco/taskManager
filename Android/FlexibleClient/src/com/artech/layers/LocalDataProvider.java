package com.artech.layers;

import com.artech.base.application.IGxObject;
import com.artech.base.application.OutputResult;
import com.artech.base.model.PropertiesObject;
import com.artech.base.services.IGxProcedure;

public class LocalDataProvider implements IGxObject
{
	private final String mName;
	private final IGxProcedure mImplementation;

	public LocalDataProvider(String name)
	{
		mName = name;

		// Data providers are actually generated as procedures.
		mImplementation = GxObjectFactory.getProcedure(name);
	}

	@Override
	public OutputResult execute(PropertiesObject parameters)
	{
		if (mImplementation != null)
		{
			LocalUtils.beginTransaction();
			try
			{
				mImplementation.execute(parameters);
			}
			finally {
				LocalUtils.endTransaction();
			}
		
			return OutputResult.ok();
		}
		else
			return LocalUtils.outputNoImplementation(mName);
	}
}
