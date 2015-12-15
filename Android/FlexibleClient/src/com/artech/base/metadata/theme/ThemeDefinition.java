package com.artech.base.metadata.theme;

import java.io.Serializable;

import com.artech.base.utils.NameMap;

public class ThemeDefinition implements Serializable
{
	private static final long serialVersionUID = 1L;

	private final String mName;
	private final NameMap<ThemeClassDefinition> mClasses = new NameMap<>();
	private final NameMap<TransformationDefinition> mTransformations = new NameMap<>();

	public ThemeDefinition(String name)
	{
		mName = name;
	}

	public String getName() { return mName; }

	public ThemeApplicationClassDefinition getApplicationClass()
	{
		ThemeClassDefinition appClass = getClass(ThemeApplicationClassDefinition.CLASS_NAME);
		if (appClass != null)
			return (ThemeApplicationClassDefinition)appClass;

		return new ThemeApplicationClassDefinition(this, null);
	}

	public ThemeClassDefinition getClass(String id)
	{
		return mClasses.get(id);
	}

	public TransformationDefinition getTransformation(String id)
	{
		return mTransformations.get(id);
	}

	public void putClass(ThemeClassDefinition def)
	{
		mClasses.put(def.getName(), def);
	}

	public void removeClass(String defName) {
		mClasses.remove(defName);
	}

	public void putTransformation(TransformationDefinition transformation)
	{
		mTransformations.put(transformation.getName(), transformation);
	}

	public void removeTransformation(String defName) {
		mTransformations.remove(defName);
	}
}
