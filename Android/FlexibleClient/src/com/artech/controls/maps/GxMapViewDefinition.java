package com.artech.controls.maps;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.artech.R;
import com.artech.base.metadata.DataItem;
import com.artech.base.metadata.enums.DataTypes;
import com.artech.base.metadata.enums.ImageScaleType;
import com.artech.base.metadata.layout.ControlInfo;
import com.artech.base.metadata.layout.GridDefinition;
import com.artech.base.metadata.loader.MetadataLoader;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.services.Services;
import com.artech.base.utils.PlatformHelper;
import com.artech.base.utils.Strings;
import com.artech.controls.grids.CustomGridDefinition;

public class GxMapViewDefinition extends CustomGridDefinition
{
	private String mGeoLocationExpression;
	private String mPinImageExpression;
	private String mPinImage;
	private String mPinImageClass;
	private boolean mShowMyLocation;
	private String mMyLocationImage;
	private String mMapType;
	private boolean mCanChooseMapType;

	private int mInitialCenter;
	private String mCustomCenterExpression;

	private int mInitialZoom;
	private String mZoomRadiusExpression;

	public static final int INITIAL_CENTER_DEFAULT = 0;
	public static final int INITIAL_CENTER_MY_LOCATION = 1;
	public static final int INITIAL_CENTER_CUSTOM = 2;

	public static final int INITIAL_ZOOM_DEFAULT = 0;
	public static final int INITIAL_ZOOM_NEAREST_POINT = 1;
	public static final int INITIAL_ZOOM_RADIUS = 2;

	public static final String MAP_TYPE_STANDARD = "Standard";
	public static final String MAP_TYPE_HYBRID = "Hybrid";
	public static final String MAP_TYPE_SATELLITE = "Satellite";
	public static final String MAP_TYPE_TERRAIN = "Terrain";

	/**
	 * Gets the Android Maps API Key for the currently running application.
	 * Can be overridden in resources.
	 */
	public static String getApiKey()
	{
		return Services.Strings.getResource(R.string.MapsApiKey);
	}

	public GxMapViewDefinition(Context context, GridDefinition grid)
	{
		super(context, grid);
	}

	@Override
	protected void init(GridDefinition grid, ControlInfo controlInfo)
	{
		mGeoLocationExpression = readGeoLocationExpression(grid, controlInfo);
		mPinImageExpression = readDataExpression("@SDMapsPinImageAtt", "@SDMapsPinImageField"); //$NON-NLS-1$ //$NON-NLS-2$ $NON-NLS-2$

		mMapType = controlInfo.optStringProperty("@SDMapsMapType"); //$NON-NLS-1$
		mCanChooseMapType = controlInfo.optBooleanProperty("@SDMapsCanChooseType"); //$NON-NLS-1$
		mPinImage = MetadataLoader.getObjectName(controlInfo.optStringProperty("@SDMapsPinImage")); //$NON-NLS-1$
		mPinImageClass = controlInfo.optStringProperty("@SDMapsPinImageClass");
		mShowMyLocation = controlInfo.optBooleanProperty("@SDMapsShowMyLocation"); //$NON-NLS-1$
		mMyLocationImage = MetadataLoader.getObjectName(controlInfo.optStringProperty("@SDMapsPinImageMyLocation")); //$NON-NLS-1$

		mInitialCenter = Services.Strings.parseEnum(controlInfo.optStringProperty("@SDMapsCenter"), "Default", "MyLocation", "Custom"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		mCustomCenterExpression = readDataExpression("@SDMapsCenterAtt", "@SDMapsCenterField"); //$NON-NLS-1$ //$NON-NLS-2$

		mInitialZoom = Services.Strings.parseEnum(controlInfo.optStringProperty("@SDMapsInitialZoomBehavior"), "ShowAll", "NearestPoint", "Radius"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$  //$NON-NLS-4$
		mZoomRadiusExpression = readDataExpression("@SDMapsZoomRadiusAtt", "@SDMapsZoomRadiusField"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private String readGeoLocationExpression(GridDefinition grid, ControlInfo controlInfo)
	{
		String locationAttribute = controlInfo.optStringProperty("@SDMapsLocationAtt"); //$NON-NLS-1$
		String locationField = controlInfo.optStringProperty("@SDMapsLocationField"); //$NON-NLS-1$

		if (Services.Strings.hasValue(locationAttribute))
			return getDataExpression(locationAttribute, locationField);
		else
			return getDefaultGeoLocationAttribute(grid);
	}

	private static String getDefaultGeoLocationAttribute(GridDefinition grid)
	{
		for (DataItem item : grid.getDataSourceItems())
		{
			if (item.getDataTypeName() != null && item.getDataTypeName().GetDataType().equals(DataTypes.geolocation))
				return item.getName();
		}

		return null;
	}

	// Properties
	public String getGeoLocationExpression() { return mGeoLocationExpression; }
	public String getPinImageExpression() { return mPinImageExpression; }
	public Drawable getPinImage() { return getDrawable(mPinImage, R.drawable.red_markers); }
	public boolean getShowMyLocation() { return mShowMyLocation; }
	public boolean getCanChooseMapType() { return mCanChooseMapType; }
	public String getMapType() { return mMapType; }
	public Drawable getMyLocationImage() { return getDrawable(mMyLocationImage, R.drawable.pin_here_arrow); }

	public int getInitialCenter() { return (mInitialCenter >= 0 ? mInitialCenter : INITIAL_CENTER_DEFAULT); }
	public String getCustomCenterExpression() { return mCustomCenterExpression; }

	public int getInitialZoom() { return (mInitialZoom >= 0 ? mInitialZoom : INITIAL_ZOOM_DEFAULT); }
	public String getZoomRadiusExpression() { return mZoomRadiusExpression; }

	public int getPinImageResourceId()
	{
		if (Strings.hasValue(mPinImage))
			return Services.Resources.getImageResourceId(mPinImage);
		else
			return 0;
	}

	public boolean needsUserLocation()
	{
		return (getShowMyLocation() ||
				getInitialCenter() == INITIAL_CENTER_MY_LOCATION ||
				getInitialZoom() == INITIAL_ZOOM_NEAREST_POINT);
	}

	public static class PinImageProperties
	{
		public ImageScaleType scaleType = ImageScaleType.FIT;
		public int width = 0;
		public int height = 0;
	}

	private PinImageProperties mPinImageProperties;

	public PinImageProperties getPinImageProperties()
	{
		if (mPinImageProperties == null)
		{
			PinImageProperties properties = new PinImageProperties();
			ThemeClassDefinition pinImageClass = PlatformHelper.getThemeClass(mPinImageClass);
			if (pinImageClass != null)
			{
				properties.width = Services.Device.dipsToPixels(Services.Strings.tryParseInt(pinImageClass.optStringProperty("PinWidth"), 0));
				properties.height = Services.Device.dipsToPixels(Services.Strings.tryParseInt(pinImageClass.optStringProperty("PinHeight"), 0));
				properties.scaleType = ImageScaleType.parse(pinImageClass.optStringProperty("PinScaleType"));
			}

			mPinImageProperties = properties;
		}

		return mPinImageProperties;
	}
}
