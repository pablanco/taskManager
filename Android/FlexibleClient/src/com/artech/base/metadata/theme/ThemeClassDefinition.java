package com.artech.base.metadata.theme;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.artech.base.metadata.DimensionValue;
import com.artech.base.metadata.DimensionValue.ValueType;
import com.artech.base.metadata.Properties;
import com.artech.base.metadata.enums.Alignment;
import com.artech.base.metadata.enums.ImageScaleType;
import com.artech.base.metadata.enums.MeasureUnit;
import com.artech.base.metadata.loader.MetadataLoader;
import com.artech.base.model.PropertiesObject;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;

public class ThemeClassDefinition extends PropertiesObject implements Serializable
{
	private static final long serialVersionUID = 1L;

	private String mName;

	protected final ThemeDefinition mTheme;
	private final ThemeClassDefinition mParentClass;
	private final ArrayList<ThemeClassDefinition> mChildItems = new ArrayList<>();

	// Cached properties (most accessed, or slowest).
	private LayoutBoxMeasures mMargins;
	private LayoutBoxMeasures mPadding;
	private String mBackgroundColor;
	private String mHighlightedBackgroundColor;
	private String mBackgroundImage;
	private String mHighlightedBackgroundImage;
	private String mBorderStyle;
	private String mBorderColor;
	private Integer mBorderWidth;
	private Integer mCornerRadius;
	private String mForegroundColor;
	private String mHighlightedForegroundColor;
	private ThemeClassDefinition mLabelClass;
	private boolean mLabelClassResolved;
	private Integer mElevation;
	private boolean mElevationResolved;

	private ThemeFontDefinition mFontDefinition;

	private Integer mLabelWidth;
	private boolean mLabelWidthResolved;

	private ImageScaleType mScaleType;
	private Integer mImageWidth;
	private Integer mImageHeight;

	private Boolean mIsAnimated;

	public ThemeClassDefinition(ThemeDefinition theme, ThemeClassDefinition parentClass)
	{
		mTheme = theme;
		mParentClass = parentClass;
	}

	@Override
	public String toString()
	{
		return mName;
	}

	public void setName(String name) {
		mName = name;
	}

	public String getName() {
		return mName;
	}

	public ThemeDefinition getTheme() {
		return mTheme;
	}

	public List<ThemeClassDefinition> getChildItems()
	{
		return mChildItems;
	}

	public String getRootName()
	{
		if (mParentClass != null)
			return mParentClass.getRootName();
		else
			return mName;
	}

	@Override
	public Object getProperty(String name)
	{
		// Overriding this method should be enough for inheritance, since other versions
		// (optStringProperty, getBooleanProperty) end up calling this one.
		Object result = super.getProperty(name);
		if (result == null && mParentClass != null)
			result = mParentClass.getProperty(name);

		return result;
	}

	public ThemeClassDefinition getParentClass() {
		return mParentClass;
	}

	/**
	 * Gets the transformation associated to this theme class. May be null.
	 */
	public TransformationDefinition getTransformation()
	{
		return mTheme.getTransformation(optStringProperty("ThemeTransformationReference")); //$NON-NLS-1$
	}

	public boolean isAnimated()
	{
		if (mIsAnimated == null)
			mIsAnimated = Services.Strings.parseBoolean(optStringProperty("ThemeAnimated"));

		return mIsAnimated;
	}

	//gets for knows properties.
	public String getColor()
	{
		if (mForegroundColor == null)
			mForegroundColor = optStringProperty("color"); //$NON-NLS-1$

		return mForegroundColor;
	}

	public String getHighlightedColor()
	{
		if (mHighlightedForegroundColor == null)
			mHighlightedForegroundColor = optStringProperty("highlighted_color"); //$NON-NLS-1$

		return mHighlightedForegroundColor;
	}

	public ThemeFontDefinition getFont()
	{
		if (mFontDefinition == null)
			mFontDefinition = new ThemeFontDefinition(this);

		return mFontDefinition;
	}

	public boolean hasBorder()
	{
		String borderStyle = getBorderStyle();
		return (Strings.hasValue(getBorderColor()) && Strings.hasValue(borderStyle) && !borderStyle.equalsIgnoreCase("none")); //$NON-NLS-1$
	}

	public String getBorderStyle()
	{
		if (mBorderStyle == null)
			mBorderStyle = optStringProperty("border_style"); //$NON-NLS-1$

		return mBorderStyle;
	}

	public int getBorderWidth()
	{
		if (mBorderWidth == null)
		{
			String str = optStringProperty("border_width"); //$NON-NLS-1$
			Integer dipValue = Services.Strings.parseMeasureValue(str, MeasureUnit.DIP);
			if (dipValue != null)
				mBorderWidth = Services.Device.dipsToPixels(dipValue);

			if (mBorderWidth == null)
				mBorderWidth = 0;
		}

		return mBorderWidth;
	}

	public String getBorderColor()
	{
		if (mBorderColor == null)
			mBorderColor = optStringProperty("border_color"); //$NON-NLS-1$

		return mBorderColor;
	}

	public boolean hasBackgroundColor(boolean highlighted)
	{
		return (highlighted ? Strings.hasValue(getHighlightedBackgroundColor()) : Strings.hasValue(getBackgroundColor()));
	}

	public String getBackgroundColor()
	{
		if (mBackgroundColor == null)
			mBackgroundColor = optStringProperty("background_color"); //$NON-NLS-1$

		return mBackgroundColor;
	}

	public String getHighlightedBackgroundColor()
	{
		if (mHighlightedBackgroundColor == null)
			mHighlightedBackgroundColor = optStringProperty("highlighted_background_color"); //$NON-NLS-1$

		return mHighlightedBackgroundColor;
	}

	public String getBackgroundImage()
	{
		if (mBackgroundImage == null)
			mBackgroundImage = getImage("background_image"); //$NON-NLS-1$

		return mBackgroundImage;
	}

	private BackgroundImageMode mBackgroundImageMode = null;
	public BackgroundImageMode getBackgroundImageMode()
	{
		if (mBackgroundImageMode != null)
			return mBackgroundImageMode;
		String strMode = optStringProperty("background_image_mode"); //$NON-NLS-1$
		mBackgroundImageMode = BackgroundImageMode.parse(strMode);
		return mBackgroundImageMode;
	}

	public String getHighlightedBackgroundImage()
	{
		if (mHighlightedBackgroundImage == null)
			mHighlightedBackgroundImage = getImage("highlighted_image"); //$NON-NLS-1$

		return mHighlightedBackgroundImage;
	}

	public boolean hasHighlightedBackground()
	{
		return Strings.hasValue(getHighlightedBackgroundColor()) || Strings.hasValue(getHighlightedBackgroundImage());
	}

	public int getCornerRadius()
	{
		if (mCornerRadius == null)
		{
			String cornerRadius = optStringProperty("border_radius"); //$NON-NLS-1$
			Integer radiusInDips = Services.Strings.parseMeasureValue(cornerRadius, MeasureUnit.DIP);
            if (radiusInDips != null)
				mCornerRadius = Services.Device.dipsToPixels(radiusInDips);
			else
				mCornerRadius = 0;
		}

		return mCornerRadius;
	}

	public LayoutBoxMeasures getMargins()
	{
		if (mMargins == null)
			mMargins = LayoutBoxMeasures.from(this, "margin"); //$NON-NLS-1$

		return mMargins;
	}


	/**
	 * Gets the padding for the theme class.
	 * This value INCLUDES border width, if set, so it may not match the json value.
	 */
	public LayoutBoxMeasures getPadding()
	{
		if (mPadding == null)
			mPadding = LayoutBoxMeasures.from(this, "padding", getBorderWidth()); //$NON-NLS-1$

		return mPadding;
	}

	public ImageScaleType getImageScaleType()
	{
		if (mScaleType == null)
			mScaleType = ImageScaleType.parse(optStringProperty("content_mode")); //$NON-NLS-1$

		return mScaleType;
	}

	public Integer getImageWidth()
	{
		if (mImageWidth == null)
		{
			DimensionValue value = DimensionValue.parse(optStringProperty("width"));
			if (value != null && value.Type == ValueType.PIXELS)
				mImageWidth = (int)value.Value;
			else
				mImageWidth = -1; // To mark as calculated, but return null.
		}

		return (mImageWidth != -1 ? mImageWidth : null);
	}

	public Integer getImageHeight()
	{
		if (mImageHeight == null)
		{
			DimensionValue value = DimensionValue.parse(optStringProperty("height"));
			if (value != null && value.Type == ValueType.PIXELS)
				mImageHeight = (int)value.Value;
			else
				mImageHeight = -1; // To mark as calculated, but return null.
		}

		return (mImageHeight != -1 ? mImageHeight : null);
	}

	public ThemeClassDefinition getStartDragClass() {
		return mTheme.getClass(optStringProperty("start_dragging_class")); //$NON-NLS-1$
	}

	public ThemeClassDefinition getAcceptDragClass() {
		return mTheme.getClass(optStringProperty("accept_drag_class")); //$NON-NLS-1$
	}

	public ThemeClassDefinition getNoAcceptDragClass() {
		return mTheme.getClass(optStringProperty("no_accept_drag_class")); //$NON-NLS-1$
	}

	public ThemeClassDefinition getDragOverClass() {
		return mTheme.getClass(optStringProperty("drag_over_class")); //$NON-NLS-1$
	}

	public ThemeClassDefinition getLabelClass()
	{
		if (!mLabelClassResolved || Services.Application.isLiveEditingEnabled())
		{
			String labelClassName = optStringProperty("ThemeLabelClassReference"); //$NON-NLS-1$
			if (Strings.hasValue(labelClassName))
				mLabelClass = mTheme.getClass(labelClassName);

			mLabelClassResolved = true;
		}

		return mLabelClass;
	}

	public ThemeClassDefinition getThemeGridOddRowClass()
	{
		String className = optStringProperty("ThemeGridOddRowClassReference");
		if (Strings.hasValue(className))
			return mTheme.getClass(className);
		else
			return null;
	}

	public ThemeClassDefinition getThemeGridEvenRowClass()
	{
		String className = optStringProperty("ThemeGridEvenRowClassReference");
		if (Strings.hasValue(className))
			return mTheme.getClass(className);
		else
			return null;

	}

	private String getThemeGridGroupSeparatorClassReference()
	{
		return optStringProperty("ThemeGroupSeparatorClassReference"); //$NON-NLS-1$
	}

	public ThemeClassDefinition getThemeGridGroupSeparatorClass()
	{
		if (getThemeGridGroupSeparatorClassReference().length()>0)
		{
			return mTheme.getClass(getThemeGridGroupSeparatorClassReference());
		}
		return null;
	}

	public int getVerticalLabelAlignment()
	{
		String LabelAlign =  optStringProperty("label_vertical_alignment"); //$NON-NLS-1$
		if (LabelAlign.equalsIgnoreCase(Properties.VerticalAlignType.Bottom))
			return Alignment.BOTTOM;
		else if (LabelAlign.equalsIgnoreCase(Properties.VerticalAlignType.Middle))
			return Alignment.CENTER_VERTICAL;
		else if (LabelAlign.equalsIgnoreCase(Properties.VerticalAlignType.Top))
			return Alignment.TOP;

		return Alignment.NONE;
	}

	public int getHorizontalLabelAlignment()
	{
		String LabelAlign =  optStringProperty("label_horizontal_alignment"); //$NON-NLS-1$

		if (LabelAlign.equalsIgnoreCase(Properties.HorizontalAlignType.Left))
			return Alignment.LEFT;
		else if (LabelAlign.equalsIgnoreCase(Properties.HorizontalAlignType.Center))
			return Alignment.CENTER_HORIZONTAL;
		else if (LabelAlign.equalsIgnoreCase(Properties.HorizontalAlignType.Right))
			return Alignment.RIGHT;
		return Alignment.NONE;

	}

	public Integer getLabelWidth()
	{
		if (!mLabelWidthResolved)
		{
			Integer dipValue = Services.Strings.parseMeasureValue(optStringProperty("label_width"), MeasureUnit.DIP);
			if (dipValue != null)
				mLabelWidth = Services.Device.dipsToPixels(dipValue);

			mLabelWidthResolved = true;
		}

		return mLabelWidth;
	}

	public String getThemeImageClass() {
		return optStringProperty("ThemeImageClassReference"); //$NON-NLS-1$
	}

	public String getThemeGrid() {
		return optStringProperty("ThemeGridClassReference"); //$NON-NLS-1$
	}

	public String getThemeTab() {
		return optStringProperty("ThemeTabClassReference"); //$NON-NLS-1$
	}

	public boolean hasMarginSet()
	{
		LayoutBoxMeasures margin = getMargins();
		return !margin.isEmpty();
	}

	public boolean hasPaddingSet()
	{
		LayoutBoxMeasures padding = getPadding();
		return !padding.isEmpty();
	}

	public String getImage(String key)
	{
		return MetadataLoader.getObjectName(optStringProperty(key));
	}

	protected ThemeClassDefinition getRelatedClass(String property)
	{
		String relatedClassName = optStringProperty(property);
		if (Strings.hasValue(relatedClassName))
			return mTheme.getClass(relatedClassName);
		else
			return null;
	}

	/**
	 * Gets the elevation (in pixels) set in this class.
	 * If null, use the control's default elevation instead.
	 */
	public Integer getElevation()
	{
		if (!mElevationResolved)
		{
			Integer elevation = Services.Strings.tryParseInt(optStringProperty("elevation"));
			if (elevation != null)
				elevation = Services.Device.dipsToPixels(elevation);

			mElevation = elevation;
			mElevationResolved = true;
		}

		return mElevation;
	}
}
