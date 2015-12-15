package com.artech.controls.grids;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.artech.base.metadata.DataItem;
import com.artech.base.metadata.layout.ControlInfo;
import com.artech.base.metadata.layout.GridDefinition;
import com.artech.base.services.Services;
import com.artech.common.ImageHelper;
import com.artech.controls.ControlPropertiesDefinition;

public abstract class CustomGridDefinition extends ControlPropertiesDefinition
{
	private final GridDefinition mGrid;
	private final Context mContext;

	public CustomGridDefinition(Context context, GridDefinition grid)
	{
		super(grid);
		mContext = context;
		mGrid = grid;

		init(grid, grid.getControlInfo());
	}

	public GridDefinition getGrid() { return (GridDefinition) getItem(); }

	protected abstract void init(GridDefinition grid, ControlInfo controlInfo);

	protected DataItem getAttribute(String name)
	{
		if (Services.Strings.hasValue(name))
		{
			for (DataItem dataItem : mGrid.getDataSourceItems())
				if (name.equalsIgnoreCase(dataItem.getName()))
					return dataItem;

			// Not found?
			Services.Log.warning(String.format("Attribute or variable '%s' referenced in definition of control '%s' is not present in data.", name, mGrid.getName())); //$NON-NLS-1$
		}

		return null;
	}

	protected Drawable getDrawable(String imageName)
	{
		return getDrawable(imageName, 0);
	}

	protected Drawable getDrawable(String imageName, int defaultImageId)
	{
		Drawable drawable = ImageHelper.getStaticImage(imageName, true);
		if (drawable == null && defaultImageId > 0)
			drawable = mContext.getResources().getDrawable(defaultImageId);

		return drawable;
	}
}
