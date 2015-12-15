package com.artech.utils;

import java.util.ArrayList;

public class Cast
{
	public static <T> T as(Class<T> t, Object o)
	{
		return (t.isInstance(o) ? t.cast(o) : null);
	}

	/**
	 * Iterates over a generic collection, returning all items that are instances of TItem.
	 */
	public static <TItem> Iterable<TItem> iterateAs(Class<TItem> itemClass, Iterable<?> collection)
	{
		ArrayList<TItem> result = new ArrayList<TItem>();
		for (Object item : collection)
		{
			if (itemClass.isInstance(item))
				result.add(itemClass.cast(item));
		}

		return result;
	}
}
