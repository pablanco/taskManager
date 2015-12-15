package com.artech.base.metadata;

import java.util.ArrayList;
import java.util.List;

import com.artech.base.metadata.enums.Connectivity;
import com.artech.base.utils.Function;
import com.artech.base.utils.ListUtils;

public abstract class GxObjectDefinition
{
	private final short mType;
	private final String mName;
	private final List<ObjectParameterDefinition> mParameters;
	private Connectivity mConnectivitySupport;

	public GxObjectDefinition(short type, String name)
	{
		mType = type;
		mName = name;
		mParameters = new ArrayList<ObjectParameterDefinition>();
		mConnectivitySupport = Connectivity.Inherit;
	}
	
	public void setConnectivitySupport(Connectivity conn) {
		mConnectivitySupport = conn;
	}
	
	public Connectivity getConnectivitySupport() {
		return mConnectivitySupport;
	}

	public short getType()
	{
		return mType;
	}

	public String getName()
	{
		return mName;
	}

	public List<ObjectParameterDefinition> getParameters()
	{
		return mParameters;
	}

	public ObjectParameterDefinition getParameter(int position)
	{
		return mParameters.get(position);
	}

	public ObjectParameterDefinition getParameter(String name)
	{
		for (ObjectParameterDefinition param : mParameters)
			if (param.getName().equalsIgnoreCase(name))
				return param;

		return null;
	}

	private List<ObjectParameterDefinition> mInParameters;
	private List<ObjectParameterDefinition> mOutParameters;

	public List<ObjectParameterDefinition> getInParameters()
	{
		if (mInParameters == null)
		{
			mInParameters = ListUtils.select(mParameters, new Function<ObjectParameterDefinition, Boolean>()
			{
				@Override
				public Boolean run(ObjectParameterDefinition p) { return p.isInput(); }
			});
		}

		return mInParameters;
	}

	public List<ObjectParameterDefinition> getOutParameters()
	{
		if (mOutParameters == null)
		{
			mOutParameters = ListUtils.select(mParameters, new Function<ObjectParameterDefinition, Boolean>()
			{
				@Override
				public Boolean run(ObjectParameterDefinition p) { return p.isOutput(); }
			});
		}

		return mOutParameters;
	}
}
