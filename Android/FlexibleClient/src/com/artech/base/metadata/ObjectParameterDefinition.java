package com.artech.base.metadata;

import java.io.Serializable;

import com.artech.base.serialization.INodeObject;
import com.artech.base.utils.Strings;

public class ObjectParameterDefinition extends DataItem implements Serializable
{
	private static final long serialVersionUID = 1L;

	private final String mName;
	private final String mMode;

	public ObjectParameterDefinition(String name, String mode)
	{
		super(null);

		if (!Strings.hasValue(mode))
			mode = MODE_IN;

		mName = name;
		mMode = mode;
	}

	public void readDataType(INodeObject json)
	{
		DataItemTypeReader.readDataType(this, json);
	}

	@Override
	public String getName() { return mName; }
	public String getMode() { return mMode; }

	@Override
	public String toString()
	{
		return String.format("%s:%s (%s)", mMode, mName, getType());
	}

	private static final String MODE_IN = "in"; //$NON-NLS-1$
	private static final String MODE_INOUT = "inout"; //$NON-NLS-1$
	private static final String MODE_OUT = "out"; //$NON-NLS-1$

	public boolean isInput()
	{
		return (MODE_IN.equalsIgnoreCase(mMode) || MODE_INOUT.equalsIgnoreCase(mMode));
	}

	public boolean isOutput()
	{
		return (MODE_OUT.equalsIgnoreCase(mMode) || MODE_INOUT.equalsIgnoreCase(mMode));
	}
}
