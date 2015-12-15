package com.artech.base.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Substitute class from Map<K, List<V>>
 */
public class MultiMap<K, V> // almost implements Map<K, V>
{
	private final HashMap<K, ArrayList<V>> mInner;

	public MultiMap()
	{
		mInner = new HashMap<K, ArrayList<V>>();
	}

	/**
	 * Adds the specified key-value pair to the map.
	 * Returns all the values associated to the key, including this one.
	 */
	public List<V> put(K key, V value)
	{
		ArrayList<V> keyValues = mInner.get(key);
		if (keyValues == null)
		{
			keyValues = new ArrayList<V>();
			mInner.put(key, keyValues);
		}

		keyValues.add(value);
		return keyValues;
	}

	/**
	 * Adds the specified key-value pairs to the map.
	 * Returns all the values associated to the key, including these ones.
	 */
	public List<V> putAll(K key, Collection<V> values)
	{
		ArrayList<V> keyValues = mInner.get(key);
		if (keyValues == null)
		{
			keyValues = new ArrayList<V>();
			mInner.put(key, keyValues);
		}

		keyValues.addAll(values);
		return keyValues;
	}

	public Set<K> keySet()
	{
		return mInner.keySet();
	}

	public Collection<V> values()
	{
		ArrayList<V> allValues = new ArrayList<V>();
		for (ArrayList<V> valuesList : mInner.values())
			allValues.addAll(valuesList);

		return allValues;
	}

	public void clear()
	{
		mInner.clear();
	}

	public void clear(K key)
	{
		mInner.remove(key);
	}

	public boolean containsKey(K key)
	{
		return mInner.containsKey(key);
	}

	public List<V> get(K key)
	{
		ArrayList<V> values = mInner.get(key);
		if (values != null)
			return values;
		else
			return new ArrayList<V>();
	}

	public int size()
	{
		int size = 0;
		for (ArrayList<V> valuesList : mInner.values())
			size += valuesList.size();

		return size;
	}

	public int getCount(K key)
	{
		ArrayList<V> values = mInner.get(key);
		if (values != null)
			return values.size();
		else
			return 0;
	}
}