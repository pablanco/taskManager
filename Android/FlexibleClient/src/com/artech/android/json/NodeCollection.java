package com.artech.android.json;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.json.JSONArray;
import org.json.JSONException;

import com.artech.base.serialization.INodeCollection;
import com.artech.base.serialization.INodeObject;
import com.artech.base.services.Services;

public class NodeCollection implements INodeCollection, Serializable
{
	private static final long serialVersionUID = 1L;

	private transient JSONArray mCollection; // Transient because JSON classes are not serializable.

	public NodeCollection()
	{
		mCollection = new JSONArray();
	}

	public NodeCollection(JSONArray array)
	{
		mCollection = array;
	}

	/**
	 * Returns the JSON String corresponding to this Node Collection.
	 */
	@Override
	public String toString()
	{
		return mCollection.toString();
	}

	@Override
	public INodeObject getNode(int index)
	{
		try
		{
			return new NodeObject(mCollection.getJSONObject(index));
		}
		catch (Exception ex)
		{
			Services.Exceptions.handle(ex);
			return null;
		}
	}

	@Override
	public int length()
	{
		return mCollection.length();
	}

	@Override
	public void put(INodeObject value)
	{
		mCollection.put(((NodeObject)value).getInner());
	}

	public JSONArray getInner()
	{
		return mCollection;
	}

	@Override
	public Iterator<INodeObject> iterator()
	{
		return new CollectionIterator();
	}

	private void writeObject(ObjectOutputStream out) throws IOException
	{
		out.defaultWriteObject();
		out.writeBytes(mCollection.toString());
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		in.defaultReadObject();

		try
		{
			mCollection = new JSONArray((String)in.readObject());
		}
		catch (JSONException ex)
		{
			throw new IOException(ex.getMessage());
		}
	}

	class CollectionIterator implements Iterator<INodeObject>
	{
		private int mCurrent = 0;

		@Override
		public boolean hasNext()
		{
			return (mCurrent < mCollection.length());
		}

		@Override
		public INodeObject next()
		{
			if (!hasNext())
				throw new NoSuchElementException();

			return getNode(mCurrent++);
		}

		@Override
		public void remove()
		{
			throw new UnsupportedOperationException();
		}
	}
}
