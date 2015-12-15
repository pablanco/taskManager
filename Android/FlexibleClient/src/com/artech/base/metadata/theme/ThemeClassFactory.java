package com.artech.base.metadata.theme;

import java.lang.reflect.Constructor;
import java.util.ArrayList;

import com.artech.base.utils.NameMap;

public class ThemeClassFactory
{
	private static final ArrayList<Class<? extends ThemeClassDefinition>> sClasses;
	private static final NameMap<Class<? extends ThemeClassDefinition>> sClassesByName;

	static
	{
		sClasses = new ArrayList<Class<? extends ThemeClassDefinition>>();
		sClassesByName = new NameMap<Class<? extends ThemeClassDefinition>>();

		// Known classes are registered here, though user controls may add more.
		register(ThemeApplicationClassDefinition.CLASS_NAME, ThemeApplicationClassDefinition.class);
		register(ThemeApplicationBarClassDefinition.CLASS_NAME, ThemeApplicationBarClassDefinition.class);
		register(ThemeFormClassDefinition.CLASS_NAME, ThemeFormClassDefinition.class);
		register(TabControlThemeClassDefinition.CLASS_NAME, TabControlThemeClassDefinition.class);
	}

	public static void register(String className, Class<? extends ThemeClassDefinition> clazz)
	{
		sClasses.add(clazz);
		sClassesByName.put(className, clazz);
	}

	public static ThemeClassDefinition createClass(ThemeDefinition theme, String className, ThemeClassDefinition parentClass)
	{
		Class<? extends ThemeClassDefinition> clazz;
		if (parentClass != null && sClasses.contains(parentClass.getClass()))
			clazz = parentClass.getClass(); // Derived theme classes must be of the same (Java) class.
		else
			clazz = sClassesByName.get(className);

		if (clazz != null)
		{
			try
			{
				Constructor<? extends ThemeClassDefinition> constructor = clazz.getConstructor(ThemeDefinition.class, ThemeClassDefinition.class);
				return constructor.newInstance(theme, parentClass);
			}
			catch (Exception e)
			{
				String errorMessage = String.format("Error creating class '%s' by reflection. Does it have the proper constructor?", clazz.getName());
				throw new IllegalArgumentException(errorMessage, e);
			}
		}
		else
			return new ThemeClassDefinition(theme, parentClass);
	}
}
