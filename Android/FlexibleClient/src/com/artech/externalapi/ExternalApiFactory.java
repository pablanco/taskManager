package com.artech.externalapi;

import java.lang.reflect.Constructor;

import com.artech.base.services.Services;
import com.artech.base.utils.NameMap;

public class ExternalApiFactory
{
	private static final NameMap<ExternalApiDefinition> mClasses = new NameMap<ExternalApiDefinition>();
    private static final NameMap<Constructor<?>> sConstructorMap = new NameMap<Constructor<?>>();
    private static final NameMap<Void> sDummyDefinitions = new NameMap<Void>();

    private static ExternalApi createInstance(String name, Class<?> clazz)
    {
		Constructor<?> constructor = sConstructorMap.get(name);
        try
        {
            if (constructor == null)
            {
               	constructor = clazz.getConstructor();
               	sConstructorMap.put(name, constructor); // Put in cache in order to avoid unnecessary reflection
            }
            if (constructor == null)
            {
            	Services.Log.Error(String.format("External Api class '%s' does not have an appropriate constructor.", clazz.getName())); //$NON-NLS-1$
            	return null;
            }

            return (ExternalApi) constructor.newInstance();
        }
        catch (Exception e)
        {
        	return null;
        }
    }

    public static void addApi(ExternalApiDefinition apiDefinition)
    {
    	mClasses.put(apiDefinition.Name, apiDefinition);
    }

    public static void addDummyApi(String apiName)
    {
    	sDummyDefinitions.put(apiName, null);
    }
    
	public static ExternalApi getInstance(String apiName)
	{
		ExternalApiDefinition apiDefinition = mClasses.get(apiName);
		if (apiDefinition != null)
		{
			try
			{
				return createInstance(apiDefinition.ClassName, Class.forName(apiDefinition.ClassName));
			}
			catch (ClassNotFoundException e)
			{
				Services.Log.Error(String.format("External object class '%s' could not be loaded via reflection.", apiDefinition.Name)); //$NON-NLS-1$
			}
		}
		else
		{
			// Dummy APIs can be overriden by a real one later. 
			// That's why this is tested only if apiDefinition == null.
			if (sDummyDefinitions.containsKey(apiName))
				return new DummyExternalApi(apiName);
		}
		
		return null;
	}
}
