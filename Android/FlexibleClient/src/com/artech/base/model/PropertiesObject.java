package com.artech.base.model;

import java.io.Serializable;

import com.artech.base.serialization.INodeObject;
import com.artech.base.services.IPropertiesObject;
import com.artech.base.services.Services;
import com.artech.base.utils.NameMap;
import com.artech.base.utils.Strings;

public class PropertiesObject implements Cloneable, Serializable, IPropertiesObject
{
	private static final long serialVersionUID = 1L;

	private NameMap<Object> mValues = new NameMap<Object>();

	private INodeObject mObject = null;
	private boolean mDeserialized = false;

	public PropertiesObject() { }


	public PropertiesObject(NameMap<Object> props)
	{
		mValues = props;
	}

	public NameMap<Object> getInternalProperties()
	{
		internalDeserialize();
		return mValues;
	}

	protected NameMap<Object> cloneProperties()
	{
		return new NameMap<Object>(mValues);
	}

	protected Iterable<String> getPropertyNames()
	{
		return mValues.keySet();
	}

	public void setInternalProperties(NameMap<Object> table)
	{
		mDeserialized = true;
		mValues = table;
	}

	@Override
	public boolean setProperty(String name, Object value)
	{
		if (name != null && value != null)
		{
			mValues.put(name, value);
			return true;
		}

		Services.Log.warning("PropertiesObject.setProperty", "Null key or value is not supported, ignoring."); //$NON-NLS-1$ //$NON-NLS-2$
		return false;
	}

	@Override
	public Object getProperty(String name)
	{
		internalDeserialize();
		return mValues.get(name);
	}

	@Override
	public String optStringProperty(String name)
	{
		Object objValue = getProperty(name);
		if (objValue == null)
			objValue = Strings.EMPTY;
		if (objValue instanceof String)
			return (String) objValue;
		return Strings.EMPTY;
	}

	protected void internalDeserialize()
	{
		if (!mDeserialized && mObject != null)
		{
			internalDeserialize(mObject);
			mDeserialized = true;
			mObject = null;
		}
	}

	protected void internalDeserialize(INodeObject data)
	{
		for (String attName : data.names())
		{
			try
			{
				if (data.isAtomic(attName))
					setProperty(attName, data.getString(attName));
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	public void deserialize(INodeObject obj)
	{
		mObject = obj;
	}

	public int optIntProperty(String val)
	{
		String lengthString = (String) getProperty(val);
		if (lengthString == null)
			return 0;
		try
		{
			return Integer.parseInt(lengthString);
		}
		catch (NumberFormatException ex)
		{ return 0;}
	}

	public long optLongProperty(String val)
	{
		String lengthString = (String) getProperty(val);
		if (lengthString == null)
			return 0;
		try
		{
			return Long.parseLong(lengthString);
		}
		catch (NumberFormatException ex)
		{ return 0;}
	}

	public boolean optBooleanProperty(String propName)
	{
		return getBooleanProperty(propName, false);
	}

	public boolean getBooleanProperty(String propName, boolean defaultValue)
	{
		Object value = getProperty(propName);
		if (value instanceof Boolean)
			return (Boolean)value;

		if (value instanceof String)
			return Services.Strings.tryParseBoolean((String)value, defaultValue);

		return defaultValue;
	}
}
