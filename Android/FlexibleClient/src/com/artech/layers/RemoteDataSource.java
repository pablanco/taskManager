package com.artech.layers;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONObject;

import com.artech.android.json.NodeObject;
import com.artech.base.metadata.StructureDefinition;
import com.artech.base.model.Entity;
import com.artech.base.providers.GxUri;
import com.artech.base.providers.IDataSourceResult;
import com.artech.common.ServiceDataResult;
import com.artech.common.ServiceHelper;

class RemoteDataSource
{
	public IDataSourceResult execute(GxUri uri, int sessionId, int start, int count, Date ifModifiedSince)
	{
		String fullUri = uri.toString(sessionId, start, count);
		boolean isCollection = uri.getDataSource().isCollection();
		StructureDefinition dataStructure = uri.getDataSource().getStructure();

		ServiceDataResult serverResult = ServiceHelper.getDataFromProvider(fullUri, ifModifiedSince, isCollection);
		return new DataProviderResult(serverResult, dataStructure);
	}

	private static class DataProviderResult implements IDataSourceResult
	{
		private final ServiceDataResult mResult;
		private final StructureDefinition mDataStructure;
		private List<Entity> mData;

		private DataProviderResult(ServiceDataResult serviceResult, StructureDefinition dataStructure)
		{
			mResult = serviceResult;
			mDataStructure = dataStructure;
		}

		@Override
		public boolean isOk() { return mResult.isOk(); }

		@Override
		public boolean isUpToDate() { return mResult.isUpToDate(); }

		@Override
		public Date getLastModified() { return mResult.getLastModified(); }

		@Override
		public int getErrorType() { return mResult.getErrorType(); }

		@Override
		public String getErrorMessage() { return mResult.getErrorMessage(); }

		@Override
		public List<Entity> getData()
		{
			if (mData == null)
			{
				LinkedList<Entity> data = new LinkedList<Entity>();
				for (JSONObject obj : mResult.getDataObjects())
				{
					Entity entity = new Entity(mDataStructure);
					entity.loadHeader(new NodeObject(obj));
					data.add(entity);
				}

				mData = data;
			}

			return mData;
		}
	}
}
