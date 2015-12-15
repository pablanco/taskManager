package com.artech.controllers;

import java.util.ArrayList;

import android.os.AsyncTask;

import com.artech.R;
import com.artech.app.ComponentParameters;
import com.artech.application.MyApplication;
import com.artech.base.application.IBusinessComponent;
import com.artech.base.application.OutputResult;
import com.artech.base.metadata.DataItem;
import com.artech.base.metadata.IDataSourceDefinition;
import com.artech.base.metadata.StructureDefinition;
import com.artech.base.metadata.VariableDefinition;
import com.artech.base.metadata.enums.DisplayModes;
import com.artech.base.model.Entity;
import com.artech.base.model.EntityList;
import com.artech.base.providers.IApplicationServer;
import com.artech.base.services.Services;
import com.artech.base.utils.ListUtils;
import com.artech.base.utils.Strings;
import com.artech.common.DataRequest;
import com.artech.compatibility.CompatibilityHelper;

public class DataSourceControllerBC implements IDataSourceControllerInternal
{
	private final DataViewController mParent;
	private final DataSourceModel mModel;
	private final ComponentParameters mParams;
	private final int mId;

	private IApplicationServer mServer;
	private StructureDefinition mStructure;
	private IBusinessComponent mBusinessComponent;
	private Entity mBCEntity;

	private IDataSourceBoundView mView;
	private AsyncTask<?, ?, ?> mCurrentLoadTask;
	private ViewData mCurrentResult;
	
	public DataSourceControllerBC(DataViewController parent, DataSourceModel model, StructureDefinition bc)
	{
		mParent = parent;
		mModel = model;
		mId = DataSourceController.createDataSourceId();

		mParams = parent.getComponentParams();
		initBusinessComponent(bc);
	}
	
	private void initBusinessComponent(StructureDefinition bc)
	{
		mStructure = bc;
		mServer = MyApplication.getApplicationServer(getParent().getModel().getConnectivity());
		mBusinessComponent = mServer.getBusinessComponent(bc.getName());
		mBCEntity = new Entity(bc);
		addVariablesToBC(mBCEntity);
	}

	private void startLoading()
	{
		CompatibilityHelper.executeAsyncTask(new AsyncTask<Void, Void, ViewData>()
		{
			@Override
			protected void onPreExecute()
			{
				mCurrentLoadTask = this;
			}

			@Override
			protected ViewData doInBackground(Void... params)
			{
				return loadBusinessComponent();
			}

			@Override
			protected void onPostExecute(ViewData result)
			{
				if (mView != null && !isCancelled())
					mView.update(result);

				mCurrentResult = result;
				mCurrentLoadTask = null;
			}
		});
	}
	
	private ViewData loadBusinessComponent()
	{
		if (mParams.Mode == DisplayModes.INSERT)
		{
			mBusinessComponent.initialize(mBCEntity);
			loadNamedParameters(mBCEntity, mParams);
			return ViewData.customData(mBCEntity, DataRequest.RESULT_SOURCE_SERVER);
		}
		else
		{
			Entity entity = new Entity(mStructure);
			OutputResult loadResult = mBusinessComponent.load(entity, mParams.Parameters);

			if (loadResult.isOk())
			{
				mBCEntity = entity;
				addVariablesToBC(mBCEntity);
				return ViewData.customData(mBCEntity, DataRequest.RESULT_SOURCE_SERVER);
			}
			else
			{
				// If we cannot 'GET' the entity, then we won't be able to update/delete it either.
				String message = String.format(Services.Strings.getResource(R.string.GXM_InvalidMode), DisplayModes.getString(mParams.Mode), loadResult.getErrorText());
				return new ViewData(null, null, DataRequest.RESULT_SOURCE_SERVER, new EntityList(), false, DataRequest.ERROR_SERVER, message, false);
			}
		}
	}

	private static void loadNamedParameters(Entity entity, ComponentParameters params)
	{
		for (DataItem def : entity.getLevel().Items)
		{
			String result = params.NamedParameters.get(def.getName());
			if (Strings.hasValue(result))
				entity.setProperty(def.getName(), result);
		}
	}

	private void addVariablesToBC(Entity bcEntity)
	{
		ArrayList<VariableDefinition> variables = new ArrayList<VariableDefinition>();
		
		// Variables from the Form.
		if (mParent.getComponentParams() != null && mParent.getComponentParams().Object != null)
			variables.addAll(mParent.getComponentParams().Object.getVariables());
		
		// Variables from the Data Provider. Although the DP is not called as of now,
		// they still must have a definition to work properly in the client.
		if (mModel != null && mModel.getDefinition() != null)
			variables.addAll(ListUtils.itemsOfType(mModel.getDefinition().getDataItems(), VariableDefinition.class));
		
		if (variables.size() != 0)
			bcEntity.setExtraMembers(variables);

		// Get the &Mode variable ready for usage in events.
		DisplayModes.setVariable(bcEntity, mParams.Mode);
	}
	
	public IApplicationServer getServer()
	{
		return mServer;
	}
	
	public IBusinessComponent getBusinessComponent()
	{
		return mBusinessComponent;
	}

	public Entity getBCEntity()
	{
		return mBCEntity;
	}
	
	public short getMode()
	{
		return mParams.Mode;
	}
	
	@Override
	public int getId()
	{
		return mId;
	}

	@Override
	public String getName()
	{
		return mStructure.getName();
	}

	@Override
	public IDataViewController getParent()
	{
		return mParent;
	}

	@Override
	public IDataSourceDefinition getDefinition()
	{
		// This is a shim. The DataSourceModel is not actually used.
		return mModel.getDefinition();
	}

	@Override
	public DataSourceModel getModel()
	{
		// This is a shim. The DataSourceModel is not actually used.
		return mModel;
	}

	@Override
	public void onRequestMoreData() { }

	@Override
	public void onResume()
	{
		if (mCurrentLoadTask != null || mCurrentResult != null)
			return; // Already loading (or already loaded).
		
		startLoading();
	}

	@Override
	public void onRefresh(boolean keepPosition)
	{
		if (mCurrentLoadTask != null)
			return; // Already loading, refresh is ignored.

		if (mParams.Mode == DisplayModes.INSERT)
			return; // Doesn't make sense to refresh when inserting.
		
		startLoading(); 
	}

	@Override
	public void onPause()
	{
		if (mCurrentLoadTask != null)
			mCurrentLoadTask.cancel(false);
	}

	@Override
	public void attach(IDataSourceBoundView view)
	{
		mView = view;
	}

	@Override
	public void detach()
	{
		mView = null;
	}

	@Override
	public IDataSourceBoundView getBoundView()
	{
		return mView;
	}
}
