package com.artech.controls.grids;

import java.util.List;

import com.artech.actions.UIContext;
import com.artech.android.ViewHierarchyVisitor;
import com.artech.android.layout.ControlHelper;
import com.artech.android.layout.ControlProperties;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.common.ExecutionContext;
import com.artech.controls.IDataViewHosted;
import com.artech.controls.IGxControl;

/**
 * UIContext specialization associated to a Grid item.
 */
class GridItemUIContext extends UIContext
{
	private final GridHelper mGridHelper;
	private final ControlProperties mControlProperties;
	private boolean mControlPropertiesTrackingEnabled;

	public GridItemUIContext(UIContext parentUIContext, GridHelper gridHelper, GridItemViewInfo gridItem)
	{
		super(parentUIContext.getActivity(), null, gridItem.getView(), parentUIContext.getConnectivitySupport());
		mControlProperties = new ControlProperties();
		setParentFrom(gridHelper);
		mGridHelper = gridHelper;
		mControlPropertiesTrackingEnabled = true;
	}

	private void setParentFrom(GridHelper gridHelper)
	{
		// See if the grid is hosted in a data view. In that case the data view will be the parent.
		IDataViewHosted hosted = ViewHierarchyVisitor.getParent(IDataViewHosted.class, gridHelper.getGridView());

		if (hosted != null && hosted.getHost() != null)
			setParent(hosted.getHost().getUIContext());
	}

	void setGridItem(GridItemViewInfo gridItem)
	{
		// Update the reference to the view. Needed because of reuse.
		setRootView(gridItem.getView());
	}

	@Override
	public IGxControl findControl(String name)
	{
		IGxControl control = super.findControl(name);

		// Whenever we gain access to a particular grid item control, we need to
		// track its changes to properties, to apply them if the control is redrawn.
		if (control != null)
			return new GridControlWrapper(control);
		else
			return null;
	}

	public ControlProperties getAssignedControlProperties()
	{
		return mControlProperties;
	}

	private class GridControlWrapper implements IGxControl
	{
		private final IGxControl mControl;

		public GridControlWrapper(IGxControl control)
		{
			mControl = control;
		}

		@Override
		public String getName()
		{
			return mControl.getName();
		}

		@Override
		public LayoutItemDefinition getDefinition()
		{
			return mControl.getDefinition();
		}

		@Override
		public void requestLayout()
		{
			mControl.requestLayout();
		}

		@Override
		public void setEnabled(boolean enabled)
		{
			if (mControlPropertiesTrackingEnabled)
				mControlProperties.putProperty(getName(), ControlHelper.PROPERTY_ENABLED, Boolean.toString(enabled));

			mControl.setEnabled(enabled);
		}

		@Override
		public void setFocus(boolean showKeyboard)
		{
			mControl.setFocus(showKeyboard);
		}

		@Override
		public void setThemeClass(ThemeClassDefinition themeClass)
		{
			if (themeClass != null)
			{
				if (mControlPropertiesTrackingEnabled)
					mControlProperties.putProperty(getName(), ControlHelper.PROPERTY_CLASS, themeClass.getName());

				mControl.setThemeClass(themeClass);
			}
		}

		@Override
		public void setVisible(boolean visible)
		{
			if (mControlPropertiesTrackingEnabled)
				mControlProperties.putProperty(getName(), ControlHelper.PROPERTY_VISIBLE, Boolean.toString(visible));

			mControl.setVisible(visible);
		}

		@Override
		public void setCaption(String caption)
		{
			if (mControlPropertiesTrackingEnabled)
				mControlProperties.putProperty(getName(), ControlHelper.PROPERTY_CAPTION, caption);

			mControl.setCaption(caption);
		}

		@Override
		public boolean isEnabled()
		{
			return mControl.isEnabled();
		}

		@Override
		public ThemeClassDefinition getThemeClass()
		{
			return mControl.getThemeClass();
		}

		@Override
		public boolean isVisible()
		{
			return mControl.isVisible();
		}

		@Override
		public String getCaption()
		{
			return mControl.getCaption();
		}

		@Override
		public void setExecutionContext(ExecutionContext context)
		{
			mControl.setExecutionContext(context);
		}

		@Override
		public Object getProperty(String name)
		{
			return mControl.getProperty(name);
		}

		@Override
		public void setProperty(String name, Object value)
		{
			if (mControlPropertiesTrackingEnabled)
				mControlProperties.putProperty(getName(), name, value);

			mControl.setProperty(name, value);
		}

		@Override
		public void runMethod(String name, List<Object> parameters)
		{
			if (mControlPropertiesTrackingEnabled)
				mGridHelper.disableViewReuse();

			// TODO: Should these be preserved too?
			mControl.runMethod(name, parameters);
		}
	}

	public void setControlPropertiesTrackingEnabled(boolean value)
	{
		mControlPropertiesTrackingEnabled = value;
	}
}
