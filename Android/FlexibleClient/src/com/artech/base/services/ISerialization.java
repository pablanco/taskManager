package com.artech.base.services;

import com.artech.base.serialization.INodeCollection;
import com.artech.base.serialization.INodeObject;

public interface ISerialization
{
	INodeObject createNode();
	INodeObject createNode(String json);
	INodeObject createNode(Object json);

	INodeCollection createCollection();
	INodeCollection createCollection(String json);
	INodeCollection createCollection(Object json);

	/**
	 * Converts the string into a valid file name (strips invalid characters and
	 * trims to maximum size if necessary).
	 */
	String makeFileName(String name);

	boolean serializeObject(Object object, String filename);
	Object deserializeObject(String filename);
}
