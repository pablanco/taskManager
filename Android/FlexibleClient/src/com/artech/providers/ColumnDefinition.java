package com.artech.providers;

import java.io.Serializable;

import com.artech.base.metadata.DataItem;

class ColumnDefinition implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	private final String mName;
	private final int mType;
	private final boolean mIsKey;
	
	public ColumnDefinition(String name, int type)
	{
		this(name, type, false);
	}
	
	public ColumnDefinition(String name, int type, boolean isKey)
	{
		mName = name;
		mType = type;
		mIsKey = isKey;
	}
	
	ColumnDefinition(DataItem dataItem)
	{
		this(
			dataItem.getName(),
			dataItem.getStorageType(),
			dataItem.isKey());
	}

	@Override
	public String toString()
	{
		return mName;
	}
	
	public String getName() { return mName; }
	public String getSqlName() { return EntityDatabaseHelper.sqlName(mName); }
	
	public int getType() { return mType; }
	public boolean isKey() { return mIsKey; }

	public final static int TYPE_CHARACTER = 1; // Character, VarChar, LongVarChar
	public final static int TYPE_INTEGER = 2; // Numeric(X,0)
	public final static int TYPE_FLOAT = 3; // Numeric(X,>0)
	public final static int TYPE_DATETIME = 4; // Date, DateTime
	public final static int TYPE_BOOLEAN = 5; // Boolean
	public final static int TYPE_BLOB = 6; // Blob
	public final static int TYPE_URI = 7; // Image, Video, Audio
	public final static int TYPE_STRUCTURED = 8; // SDT, BC.
}
