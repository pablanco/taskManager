package com.artech.base.serialization;

import java.util.List;

public interface INodeObject
{
	List<String> names();

	/**
	 * Returns true if the value mapped by name is 'atomic' (i.e. a String, Boolean, Integer, Long or Double,
	 * but not a JSONObject or JSONArray.
	 */
	boolean isAtomic(String name);

	Object get(String name);
	void put(String name, Object value);
	boolean has(String name);

	INodeObject getNode(String string);
	INodeObject optNode(String string);

	INodeCollection getCollection(String string);
	INodeCollection optCollection(String name);

	String getString(String name);
	String optString(String name);
	String optString(String name, String defaultValue);

	boolean optBoolean(String name);
	boolean optBoolean(String name, boolean defaultValue);

	int optInt(String name);
}
