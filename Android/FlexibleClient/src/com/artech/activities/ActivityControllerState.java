package com.artech.activities;

import java.util.LinkedHashMap;

import com.artech.app.ComponentId;
import com.artech.app.ComponentParameters;
import com.artech.controllers.DataViewController;
import com.artech.fragments.IDataView;

class ActivityControllerState
{
	private final LinkedHashMap<ComponentId, DataViewController> mControllers;

	public ActivityControllerState()
	{
		mControllers = new LinkedHashMap<ComponentId, DataViewController>();
	}

	public void save(LinkedHashMap<IDataView, DataViewController> controllers)
	{
		for (DataViewController dataView : controllers.values())
		{
			dataView.detachController();
			mControllers.put(dataView.getId(), dataView);
		}
	}

	public DataViewController restoreController(ComponentId id, ComponentParameters params, ActivityController parent, IDataView dataView)
	{
		DataViewController restored = mControllers.get(id);
		if (restored != null)
		{
			mControllers.remove(id);

			// We might be asked to restore a controller when it's not possible (for example if a component
			// has different objects in different layouts). In that case, DO NOT restore it.
			if (restored.getDefinition() != params.Object)
				return null;

			restored.attachController(parent, dataView);
		}

		return restored;
	}
}
