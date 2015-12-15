package com.artech.layers;

import com.artech.application.MyApplication;
import com.artech.base.services.IGxBusinessComponent;
import com.artech.base.services.IGxProcedure;
import com.artech.base.utils.ReflectionHelper;
import com.genexus.GXProcedure;
import com.genexus.GXReorganization;

public class GxObjectFactory
{
	public static IGxBusinessComponent getBusinessComponent(String name)
	{
		//String className = /*getPackageName() + ".bcs." +*/ "Sdt" + name + "";
		String className = name ;
		// if has module
		if (name.contains("."))
		{
			int index = name.lastIndexOf(".");
			className = name.substring(0, index+1).toLowerCase() + "Sdt" + name.substring(index+1);
		}
		else
		{
			className = "Sdt" + className;
		}
		return createInstanceInt(IGxBusinessComponent.class, className);
	}

	public static IGxProcedure getProcedure(String name)
	{
		String className = /*getPackageName() + ".procs." +*/ name.toLowerCase() + "";
		return createInstanceInt(IGxProcedure.class, className);
	
	}

	public static GXProcedure getComboValuesClass(String name)
	{
		String className = /*getPackageName() + ".procs." +*/ name.toLowerCase() + "";
		return createInstanceInt(GXProcedure.class, className);
	
	}
	
	public static GXReorganization getReorganization()
	{
		String className = "Reorganization";
		return createInstanceDefault(GXReorganization.class, className);
	}
	
	@SuppressWarnings("unused")
	private static String getPackageName()
	{
		return MyApplication.getAppContext().getPackageName();
	}

	private static <T> T createInstanceDefault(Class<T> base, String className)
	{
		Class<? extends T> clazz = ReflectionHelper.getClass(base, className);
		return ReflectionHelper.createDefaultInstance(clazz, true);
	}
	
	private static <T> T createInstanceInt(Class<T> base, String className)
	{
		Class<? extends T> clazz = ReflectionHelper.getClass(base, className);
		return ReflectionHelper.createDefaultInstance(clazz, false);
	}
}
