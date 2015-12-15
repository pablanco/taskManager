package com.artech.layers;

import com.artech.base.application.IGxObject;
import com.artech.base.application.OutputResult;
import com.artech.base.model.PropertiesObject;

class DummyObject implements IGxObject
{
	private final String mName;

	public DummyObject(String name)
	{
		mName = name;
	}

	@Override
	public OutputResult execute(PropertiesObject parameters)
	{
		return RemoteUtils.outputNoDefinition(mName);
	}
}
