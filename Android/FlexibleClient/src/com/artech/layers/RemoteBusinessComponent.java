package com.artech.layers;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.artech.android.json.NodeObject;
import com.artech.base.application.IBusinessComponent;
import com.artech.base.application.OutputResult;
import com.artech.base.metadata.StructureDefinition;
import com.artech.base.metadata.enums.DisplayModes;
import com.artech.base.model.Entity;
import com.artech.base.serialization.INodeObject;
import com.artech.base.services.ServiceResponse;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.artech.common.ServiceDataResult;
import com.artech.common.ServiceHelper;

/**
 * Implementation of the IBusinessComponent interface that uses a remote server
 * (via a REST interface) to perform reads and updates.
 * @author matiash
 */
class RemoteBusinessComponent implements IBusinessComponent
{
	private final String mName;
	private final StructureDefinition mDefinition;
	private Entity mBoundEntity;
	private int mMode;

	public RemoteBusinessComponent(String name, StructureDefinition definition)
	{
		mName = name;
		mDefinition = definition;
		mMode = DisplayModes.INSERT;
	}

	@Override
	public void initialize(Entity entity)
	{
		mBoundEntity = entity;
		mMode = DisplayModes.INSERT;

		if (mDefinition != null)
		{
			if (!loadDefaultsForBC(mBoundEntity))
				mBoundEntity.initialize();
		}
	}

	@Override
	public OutputResult load(Entity entity, List<String> key)
	{
		if (mDefinition == null)
			return RemoteUtils.outputNoDefinition(mName);

		mBoundEntity = entity;
		mMode = DisplayModes.EDIT; // Could be DELETE, will be determined later.

		entity.setKey(key);
		return loadEntity(mBoundEntity);
	}

	@Override
	public OutputResult save(Entity entity)
	{
		if (mDefinition == null)
			return RemoteUtils.outputNoDefinition(mName);

		mBoundEntity = entity;
		ServiceResponse response;

		if (mMode == DisplayModes.INSERT)
			response = callService(entity, Entity.OPERATION_INSERT);
		else if (mMode == DisplayModes.EDIT)
			response = callService(entity, Entity.OPERATION_UPDATE);
		else
			throw new IllegalArgumentException(String.format("Unknown mode: %s", mMode)); //$NON-NLS-1$

		return RemoteUtils.translateOutput(response);
	}

	@Override
	public OutputResult delete()
	{
		if (mDefinition == null)
			return RemoteUtils.outputNoDefinition(mName);

		mMode = DisplayModes.DELETE;
		ServiceResponse response = callService(mBoundEntity, Entity.OPERATION_DELETE);
		return RemoteUtils.translateOutput(response);
	}

	private boolean loadDefaultsForBC(Entity entity)
	{
		JSONObject data = ServiceHelper.getEntityDefaultsBC(mDefinition.getName());
		try
		{
			if (data != null)
			{
				entity.load(new NodeObject(data));
				entity.setProperty("gx_md5_hash", Strings.EMPTY); //$NON-NLS-1$
				return true;
			}
		}
		catch (Exception e)
		{
			// Ignore this error.
			Services.Log.Error("Exception getting defaults", e); //$NON-NLS-1$
		}

		return false;
	}

	private OutputResult loadEntity(Entity entity)
	{
		ServiceDataResult result = ServiceHelper.getEntityDataBC(mDefinition.getName(), entity.getKey());

		if (!result.isOk())
			return RemoteUtils.translateOutput(result);

		if (result.getData().length() != 0)
		{
			try
			{
				JSONObject jsonBC = result.getData().getJSONObject(0);
				entity.load(new NodeObject(jsonBC));
				return OutputResult.ok();
			}
			catch (JSONException e)
			{
				// Should never happen, or isOk() would have returned false.
				return OutputResult.error(e.getMessage());
			}
		}
		else
			return OutputResult.error("Internal error: loadEntity returned nothing.");
	}

	private ServiceResponse callService(Entity entity, int operation)
	{
		if (entity == null)
			throw new IllegalArgumentException("No entity provided."); //$NON-NLS-1$

		INodeObject data = entity.serialize();
		String uri = mDefinition.getName();

		// Call service.
		ServiceResponse response;
		List<String> entityKey = entity.getKey();

		switch (operation)
		{
			case Entity.OPERATION_INSERT :
				response = Services.HttpService.insertEntityData(uri, entityKey, data);
				break;

			case Entity.OPERATION_UPDATE :
				response = Services.HttpService.saveEntityData(uri, entityKey, data);
				break;

			case Entity.OPERATION_DELETE :
				response = Services.HttpService.deleteEntityData(uri, entityKey);
				break;

			default :
				throw new IllegalArgumentException(String.format("Unknown operation: %s", operation)); //$NON-NLS-1$
		}

		if (response != null && response.getResponseOk())
		{
			// Reload data (e.g. autonumbers) in case of insert or delete.
			if (operation == Entity.OPERATION_INSERT || operation == Entity.OPERATION_UPDATE )
				entity.deserialize(response.Data);
		}

		return response;
	}
}
