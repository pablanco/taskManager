package com.artech.adapters;

import java.lang.reflect.Method;
import java.util.List;

import android.app.Activity;
import android.content.res.TypedArray;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.CheckBox;

import com.artech.actions.UIContext;
import com.artech.activities.ActivityLauncher;
import com.artech.android.layout.GxTheme;
import com.artech.base.metadata.DataItem;
import com.artech.base.metadata.DataTypeName;
import com.artech.base.metadata.IDataSourceDefinition;
import com.artech.base.metadata.ILayoutDefinition;
import com.artech.base.metadata.RelationDefinition;
import com.artech.base.metadata.enums.DisplayModes;
import com.artech.base.metadata.enums.LayoutModes;
import com.artech.base.metadata.layout.LayoutDefinition;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.metadata.layout.Size;
import com.artech.base.metadata.layout.TableDefinition;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.model.Entity;
import com.artech.base.model.EntityHelper;
import com.artech.base.model.EntityList;
import com.artech.base.services.Services;
import com.artech.common.FormatHelper;
import com.artech.common.PhoneHelper;
import com.artech.compatibility.CompatibilityHelper;
import com.artech.controllers.IDataSourceBoundView;
import com.artech.controllers.ViewData;
import com.artech.controls.DataBoundControl;
import com.artech.controls.IGxActionControl;
import com.artech.controls.IGxEdit;
import com.artech.controls.IGxEditControl;
import com.artech.controls.grids.GridItemViewInfo;
import com.artech.controls.grids.IGridAdapter;
import com.artech.utils.Cast;

public class AdaptersHelper
{
	public static void setBounds(LayoutDefinition layout, Size size)
	{
		setBounds(layout.getTable(), size.getWidth(), size.getHeight());
	}

	public static void setBounds(TableDefinition table, int width, int height)
	{
		if (table == null)
		{
			Services.Log.Error("setBounds", "No layout in set bounds"); //$NON-NLS-1$ //$NON-NLS-2$
			return;
		}
		table.calculateBounds(width, height);
	}

	public static Size getWindowSize(Activity activity, ILayoutDefinition definition)
	{
		return new Size(getWindowWidth(activity), getWindowHeight(activity, definition));
	}

	/**
	 * Tries to get the available width from the (previously measured) view of the activity.
	 * If if fails then calls getDisplayWidth().
	 */
	private static int getWindowWidth(Activity activity)
	{
		int width = 0;

		//Doesn't work on create method, should call it from another place?
		View content = activity.getWindow().findViewById(Window.ID_ANDROID_CONTENT);
		if (content!=null)
			width = content.getWidth();

		if (width == 0)
			width = getDisplayWidth(activity);

    	return width;
	}

	/**
	 * Gets the available width using only the DisplayMetrics class and removing known decorations.
	 */
	@SuppressWarnings("deprecation")
	public static int getDisplayWidth(Activity activity)
	{
		Display display = activity.getWindowManager().getDefaultDisplay();
		return display.getWidth();
	}

	private static final int STATUS_BAR_HEIGHTInDip = 25;
	private static final int STATUS_BAR_HEIGHT_HONEYCOMBInDip = 48;

	/**
	 * Tries to get the available height from the root view of the activity.
	 * If if fails then calls getDisplayHeight().
	 */
	private static int getWindowHeight(Activity activity, ILayoutDefinition layout)
	{
		int height = 0;

		//Doesn't work on create method, should call it from another place?
		View content = activity.getWindow().findViewById(Window.ID_ANDROID_CONTENT);
		if (content!=null)
			height = content.getHeight();

		if (height == 0)
			height = getDisplayHeight(activity, layout);

    	return height;
	}

	/**
	 * Gets the available height using only the DisplayMetrics class and removing known decorations.
	 */
	@SuppressWarnings("deprecation")
	public static int getDisplayHeight(Activity activity, ILayoutDefinition layout)
	{
		DisplayMetrics displayMetrics = new DisplayMetrics();
		Display display = activity.getWindowManager().getDefaultDisplay();
		display.getMetrics(displayMetrics);
		int height = display.getHeight();

		// Remove decorations.
		int statusBarHeight;
		if (CompatibilityHelper.isIceCreamSandwich())
		{
			// Supposedly this should work for any Android version (?)
			// Tested in Nexus 4/7/10, Galaxy S4 / Tab 10.1 / Tab 2 10.1
			statusBarHeight = getStatusBarHeight(activity);
		}
		else if (CompatibilityHelper.isHoneycomb())
		{
			statusBarHeight = Services.Device.dipsToPixels(STATUS_BAR_HEIGHT_HONEYCOMBInDip);
		}
		else
		{
			// 2.x, Gingerbread and below.
			statusBarHeight = Services.Device.dipsToPixels(STATUS_BAR_HEIGHTInDip);
		}
		// StatusBar EnableHeaderRowPattern
		if (layout!=null && layout.getEnableHeaderRowPattern() && CompatibilityHelper.isStatusBarOverlayingAvailable())
			statusBarHeight = 0;

		height = height - statusBarHeight;

		//title bar
		int titleBarHeight = getTitleBarHeight(activity, layout);
		// ActionBar EnableHeaderRowPattern
		if (layout!=null && layout.getEnableHeaderRowPattern())
			titleBarHeight = 0;

		height = height - titleBarHeight;

		// Services.Log.info("WindowDisplay Height", String.valueOf(height)); //$NON-NLS-1$
		return height;
	}

	public static int getStatusAndActionBarHeight(Activity activity, ILayoutDefinition layout)
	{
		int statusActionBarHeight = 0;
		statusActionBarHeight += AdaptersHelper.getStatusBarHeight(activity);
		statusActionBarHeight += getTitleBarHeight(activity, layout);
		return statusActionBarHeight;

	}

	public static int getStatusBarHeight(Activity activity)
	{
	    int statusBarHeight = 0;

	    if (!hasOnScreenSystemBar(activity))
	    {
	        int resourceId = activity.getResources().getIdentifier("status_bar_height", "dimen", "android");
	        if (resourceId > 0)
	            statusBarHeight = activity.getResources().getDimensionPixelSize(resourceId);
	    }

	    return statusBarHeight;
	}

	@SuppressWarnings("deprecation")
	private static boolean hasOnScreenSystemBar(Activity activity)
	{
	    Display display = activity.getWindowManager().getDefaultDisplay();
	    int rawDisplayHeight = 0;
	    try
	    {
	        Method getRawHeight = Display.class.getMethod("getRawHeight");
	        rawDisplayHeight = (Integer) getRawHeight.invoke(display);
	    }
	    catch (Exception ex) { }

	    int uiRequestedHeight = display.getHeight();
	    return rawDisplayHeight - uiRequestedHeight > 0;
	}

	private static final int TITLE_BAR_HEIGHTInDip = 48;

	private static int getTitleBarHeight(Activity activity, ILayoutDefinition layout)
	{
		if (layout != null && layout.getShowApplicationBar())
		{
			final TypedArray styledAttributes = activity.getTheme().obtainStyledAttributes(new int[] { com.artech.R.attr.actionBarSize });
			int size = (int) styledAttributes.getDimension(0, 0);
			styledAttributes.recycle();			
			
			if (size == 0)
				return Services.Device.dipsToPixels(TITLE_BAR_HEIGHTInDip);
			else
				return size;
		}
		else
			return 0; // No title bar.
	}

	public static boolean hasOnClickAction(DataBoundControl control, short layoutMode, short trnMode)
	{
	    LayoutItemDefinition controlDef = control.getFormItemDefinition();
	    if (controlDef != null)
	    {
	    	if (!controlDef.getReadOnly(layoutMode, trnMode))
	    		return false;
	    	
	    	DataTypeName domainDefinition = controlDef.getDataTypeName();
	    	if (domainDefinition != null && domainDefinition.GetActions().size() != 0)
	    	{
	    		// Semantic domain action.
	    		// In grids, setting autolink to false disables it. Everywhere else it does not.
	    		if (layoutMode == LayoutModes.LIST)
	    		{
	    			if (controlDef.getAutoLink())
	    				return true;
	    		}
	    		else
	    			return true;
	    	}

			RelationDefinition relDef = controlDef.getFK();
			if (relDef != null && controlDef.getAutoLink())
				return true; // FK link.
	    }

	    return false;
	}

	public static void launchDomainAction(UIContext context, View v, Entity entity)
	{
		if (!(v instanceof DataBoundControl))
			return;

		DataBoundControl actionDomainControl = (DataBoundControl)v;

	    LayoutItemDefinition formAttDef = actionDomainControl.getFormItemDefinition();
	    if (formAttDef != null)
	    {
	    	DataTypeName domainDefinition = formAttDef.getDataTypeName();
	    	//Actions, first FK
			RelationDefinition relDef = formAttDef.getFK();

			if (relDef != null && formAttDef.getAutoLink())
			{
				ActivityLauncher.callRelated(context, entity, relDef);
			}
			else if (domainDefinition != null && domainDefinition.GetActions().size()>0)
	    	{
	    		String type = domainDefinition.GetActions().get(0);
	    		String value = actionDomainControl.getGx_Value();
	    		PhoneHelper.launchDomainAction(context, type, value);
	    	}
	    }
	}

	public static boolean drawListItem(IGridAdapter adapter, GridItemViewInfo itemView, int itemPosition, Entity itemEntity, ThemeClassDefinition itemClass,
			OnClickListener actionHandler, OnClickListener domainActionHandler, IDataSourceDefinition dataSource, boolean notReuseViews, boolean inSelectionMode)
	{
		// set theme
		if (itemClass != null)
			GxTheme.applyStyle(itemView.getHolder(), itemClass);

		// Get the entity from which properties can be "getted" (may not be the same as the collection item).
		Entity dataEntity = EntityHelper.forEvaluation(itemEntity);

		// Bind the data with the holder.
		for (View view : itemView.getBoundViews())
		{
			if (view instanceof IGxEdit)
			{
				IGxEdit edit = (IGxEdit)view;
				setEditValue(edit, dataEntity, itemPosition);

				// Don't reuse views if there are any editable controls.
				// Otherwise, when an edit with focus is reused, the ListView "jumps" as the item comes into view.
				// Also, it messes "tab order" since the controls in successive rows have the same id.
				if (edit.isEditable())
					notReuseViews = true;
			}
			else if (view instanceof IGxActionControl)
			{
				IGxActionControl action = (IGxActionControl) view;
				action.setEntity(itemEntity);
				action.setOnClickListener(actionHandler);
			}
			else if (view instanceof IDataSourceBoundView)
			{
				IDataSourceBoundView grid = (IDataSourceBoundView)view;
				if (Services.Strings.hasValue(grid.getDataSourceMember()))
				{
					EntityList gridData = Cast.as(EntityList.class, itemEntity.getProperty(grid.getDataSourceMember()));
					if (gridData != null)
						grid.update(ViewData.memberData(adapter.getData(), gridData));
				}
			}

			DataBoundControl dataControl = Cast.as(DataBoundControl.class, view);
			if (dataControl != null && AdaptersHelper.hasOnClickAction(dataControl, LayoutModes.LIST, DisplayModes.VIEW))
			{
				dataControl.setOnClickListener(domainActionHandler);
				dataControl.setEntity(itemEntity);
			}
		}

		CheckBox itemCheckbox = itemView.getCheckbox();
		if (inSelectionMode)
		{
			itemCheckbox.setVisibility(View.VISIBLE);
			itemCheckbox.setChecked(itemEntity.isSelected());
			itemCheckbox.setTag(itemEntity);
		}
		else
		{
			if (itemCheckbox != null)
				itemCheckbox.setVisibility(View.GONE);
		}

		return notReuseViews;
	}

	public static void setEditValue(IGxEdit edit, Entity entity)
	{
		// -1 in itemPosition means "do not recalculate dataId".
		setEditValue(edit, entity, -1);
	}

	private static void setEditValue(IGxEdit edit, Entity entity, int itemPosition)
	{
		String dataId = edit.getGx_Tag();

		DataBoundControl control = getControlFromEdit(edit);
		if (control != null && control.getFormItemDefinition() != null)
		{
			if (itemPosition >= 0)
			{
				// Resolve data item expression if necessary. Why is done here instead of
				// when the control is created? Because list item views are reused under some
				// conditions, and in those cases the expression would keep the old assigned
				// position. So we recalculate it to force use of the current one.
				// Note: increase itemPosition because GX indexes are 1-based.
				dataId = control.getFormItemDefinition().getDataId(itemPosition + 1);
				edit.setGx_Tag(dataId);
			}
		}

		if (dataId == null)
			return;

		IGxEditControl editNew = Cast.as(IGxEditControl.class, edit);
		if (editNew == null)
		{
			String value = entity.optStringProperty(dataId);
			edit.setGx_Value(value);
		}
		else
		{
			editNew.setValue(entity.getProperty(dataId));
		}
	}

	/**
	 * Posts the values of all editable controls in the bound views list into
	 * their corresponding properties in the supplied Entity.
	 */
	public static void saveEditValues(List<View> boundViews, Entity data)
	{
		for (View view : boundViews)
		{
			IGxEdit edit = Cast.as(IGxEdit.class, view);
			if (edit != null)
				AdaptersHelper.saveEditValue(edit, data);
		}
	}

	/**
	 * If the edit control is a view that can be edited (i.e. it's not read-only) then save its
	 * value to the property it is bound to in the supplied Entity.
	 */
	public static boolean saveEditValue(IGxEdit edit, Entity data)
	{
		if (edit.isEditable())
		{
			String name = edit.getGx_Tag();
			String value = edit.getGx_Value();
			if (name != null && value != null)
				return data.setProperty(edit.getGx_Tag(), value);
		}

		return false;
	}

	public static CharSequence getFormattedText(Entity entity, String dataId, DataItem dataItem)
	{
		String value = entity.optStringProperty(dataId);
		if (dataItem != null)
		{
			CharSequence cs = FormatHelper.formatValue(value, dataItem);
			if (cs != null)
				return cs;
		}

		return value;
	}

	private static DataBoundControl getControlFromEdit(IGxEdit edit)
	{
		if (edit instanceof DataBoundControl)
			return (DataBoundControl)edit;

		if (edit instanceof View)
			return Cast.as(DataBoundControl.class, ((View)edit).getParent());

		return null;
	}
}
