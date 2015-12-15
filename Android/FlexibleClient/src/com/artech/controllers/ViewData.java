package com.artech.controllers;

import com.artech.base.model.Entity;
import com.artech.base.model.EntityList;
import com.artech.base.providers.GxUri;
import com.artech.base.utils.Strings;
import com.artech.common.DataRequest;

public class ViewData
{
	private final GxUri mUri;
	private final String mResponseId;
	private final int mResponseSource;

	private final EntityList mData;
	private final boolean mMoreAvailable;

	private final int mStatusCode;
	private final String mStatusMessage;

	private final boolean mDataUnchanged;

	ViewData(GxUri uri, String responseId, int responseSource, EntityList data, boolean moreAvailable, int statusCode, String statusMessage, boolean dataUnchanged)
	{
		mUri = uri;
		mResponseId = responseId;
		mResponseSource = responseSource;

		mData = data;
		mMoreAvailable = moreAvailable;
		mStatusCode = statusCode;
		mStatusMessage = statusMessage;
		mDataUnchanged = dataUnchanged;
	}

	public static ViewData memberData(ViewData sourceData, EntityList data)
	{
		// Member data should not have the same uri, because order, filters, &c are not the same.
		return new ViewData(null, sourceData.getResponseId(), sourceData.getResponseSource(), data, false, DataRequest.ERROR_NONE, Strings.EMPTY, false);
	}

	public static ViewData empty(boolean moreAvailable)
	{
		return new ViewData(null, Strings.EMPTY, DataRequest.RESULT_SOURCE_SERVER, new EntityList(), moreAvailable, DataRequest.ERROR_NONE, Strings.EMPTY, false);
	}

	public static ViewData customData(EntityList data, int responseSource)
	{
		return new ViewData(null, Strings.EMPTY, responseSource, data, false, DataRequest.ERROR_NONE, Strings.EMPTY, false);
	}

	public static ViewData customData(Entity data, int responseSource)
	{
		EntityList list = new EntityList();
		list.add(data);
		return customData(list, responseSource);
	}

	@Override
	public String toString()
	{
		return String.format("<DATA Records: %s, Source: %s, More: %s, Status: %s>", mData.size(), mResponseSource, mMoreAvailable, mStatusMessage); //$NON-NLS-1$
	}

	public GxUri getUri() { return mUri; }
	public String getResponseId() { return mResponseId; }
	public int getResponseSource() { return mResponseSource; }

	public int getCount() { return mData.size(); }
	public EntityList getEntities() { return mData; }
	public Entity getSingleEntity() { return (mData.size() != 0 ? mData.get(0) : null); }
	public boolean isMoreAvailable() { return mMoreAvailable; }

	public int getStatusCode() { return mStatusCode; }
	public String getStatusMessage() { return mStatusMessage; }

	public boolean getDataUnchanged() { return mDataUnchanged; }

	public boolean hasErrors()
	{
		return mStatusCode != DataRequest.ERROR_NONE;
	}
}
