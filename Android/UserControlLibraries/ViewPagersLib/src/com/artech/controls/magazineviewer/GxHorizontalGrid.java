package com.artech.controls.magazineviewer;

import java.util.ArrayList;

import android.content.Context;
import android.util.AttributeSet;

import com.artech.base.metadata.enums.Orientation;
import com.artech.base.metadata.layout.ControlInfo;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.services.Services;
import com.artech.base.utils.PlatformHelper;
import com.artech.ui.Coordinator;
import com.artech.utils.ThemeUtils;
import com.artech.controls.magazineviewer.FlipperOptions.FlipperLayoutType;

public class GxHorizontalGrid extends GxMagazineViewer {

	public GxHorizontalGrid(Context context, Coordinator coordinator, LayoutItemDefinition def) {
		super(context, coordinator, def);
	}

	public GxHorizontalGrid(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void setControlInfo(ControlInfo info) {
		mFlipperOptions = new FlipperOptions();

		ArrayList<Integer> layout = new ArrayList<Integer>();
		int columns = 1;
		int rows = 1;
		if (Services.Device.getScreenOrientation() == Orientation.PORTRAIT)
		{
			columns = info.optIntProperty("@SDHorizontalGridColumnsPerPagePortrait"); //$NON-NLS-1$
			rows = info.optIntProperty("@SDHorizontalGridRowsPerPagePortrait"); //$NON-NLS-1$
		}
		else
		{
			columns = info.optIntProperty("@SDHorizontalGridColumnsPerPageLandscape"); //$NON-NLS-1$
			rows = info.optIntProperty("@SDHorizontalGridRowsPerPageLandscape"); //$NON-NLS-1$
		}
		for (int i = 0; i < columns; i++)
			layout.add(rows);

		mFlipperOptions.setRowsPerColumn(rows);
		mFlipperOptions.setLayout(layout);

		mFlipperOptions.setItemsPerPage(columns * rows);
		mFlipperOptions.setShowFooter(info.optBooleanProperty("@SDHorizontalGridShowPageController")); //$NON-NLS-1$
		mFlipperOptions.setLayoutType(FlipperLayoutType.Specific);

		ThemeClassDefinition indicatorClass = PlatformHelper.getThemeClass(info.optStringProperty("@SDHorizontalGridPageControllerClass"));
		if (indicatorClass != null)
			mFlipperOptions.setFooterThemeClass(indicatorClass);
		else
			mFlipperOptions.setFooterBackgroundColor(ThemeUtils.getColorId(info.optStringProperty("@SDHorizontalGridPageControllerBackColor")));
	}
}
