package com.extensions.controls.sparkline;

import android.content.Context;
import android.graphics.Color;

import com.artech.base.metadata.enums.LayoutItemsTypes;
import com.artech.base.metadata.layout.ControlInfo;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.model.Entity;
import com.artech.base.model.EntityList;
import com.artech.base.services.Services;
import com.artech.controllers.ViewData;
import com.artech.controls.IGridView;
import com.artech.controls.IGxThemeable;
import com.artech.utils.ThemeUtils;

public class GxSparkLine extends SparkLineView implements IGridView, IGxThemeable
{
	private LayoutItemDefinition mItemDef;
	private ThemeClassDefinition mThemeClass;

	public GxSparkLine(Context context, LayoutItemDefinition def) {
		super(context);
		setLayoutDefinition(def);

		mItemDef = getPropertyItemDef(def);
	}

	private LayoutItemDefinition getPropertyItemDef(LayoutItemDefinition definition) {
		if (definition.getType().equalsIgnoreCase(LayoutItemsTypes.Data)) {
			return definition;
		} else {
			LayoutItemDefinition itemDef = null;
			for (LayoutItemDefinition child : definition.getChildItems()) {
				itemDef = getPropertyItemDef(child);
				if (itemDef != null) {
					break;
				}
			}
			return itemDef;
		}
	}

	public void setControlInfo(ControlInfo info) {
		SparkLineOptions options = new SparkLineOptions();
		options.setLabelText(info.optStringProperty("@SDSparkLineLabelText")); //$NON-NLS-1$
		options.setShowCurrentValue(info.optBooleanProperty("@SDSparkLineShowCurrentValue")); //$NON-NLS-1$
		options.setCurrentValueColor(ThemeUtils.getColorId("@SDSparkLineCurrentValueColor", options.getCurrentValueColor())); //$NON-NLS-1$
		setOptions(options);
	}


	private void setLayoutDefinition(LayoutItemDefinition layoutItemDefinition) {
		if (layoutItemDefinition != null)
			setControlInfo(layoutItemDefinition.getControlInfo());
	}

	@Override
	public void addListener(GridEventsListener listener) {
	}

	@Override
	public void update(ViewData data) {
		SparkLineData values = new SparkLineData();

		if (mItemDef != null) {
			EntityList entities = data.getEntities();

			for (int i = 0; i < entities.size(); i++) {
				Entity entity = entities.get(i);

				String propertyName = mItemDef.getDataId(i);

				// if it's a control
				int startPos = propertyName.lastIndexOf('.');
				if (startPos != -1) {
					propertyName = propertyName.substring(startPos + 1);
				}

				try {
					Float value = Float.valueOf((String) entity.getProperty(propertyName));
					values.add(value);
				} catch (NumberFormatException ex) {
					Services.Log.Error("Invalid Values for SparkLine"); //$NON-NLS-1$
					values = null;
					break;
				}
			}
		}

		if (values != null) {
			setDataValues(values);
		}
	}

	@Override
	public void setThemeClass(ThemeClassDefinition themeClass) {
		mThemeClass = themeClass;
		applyClass(themeClass);
	}

	@Override
	public ThemeClassDefinition getThemeClass() {
		return mThemeClass;
	}

	@Override
	public void applyClass(ThemeClassDefinition themeClass)
	{
		SparkLineOptions options = getOptions();

		options.setPenColor(ThemeUtils.getColorId(themeClass.getBorderColor(), options.getPenColor()));
		if (themeClass.getBorderWidth() != 0)
			options.setPenWidth(themeClass.getBorderWidth());

		setBackgroundColor(ThemeUtils.getColorId(themeClass.getBackgroundColor(), Color.WHITE));
	}
}
