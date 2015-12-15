package com.artech.extendedcontrols.gauge;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.services.Services;
import com.artech.base.utils.PlatformHelper;
import com.artech.base.utils.Strings;
import com.artech.utils.ThemeUtils;

public class ChartSurface extends View
{
	private GaugeSpecification mSpec;
	private int mWidth;
	private int mMargin = Services.Device.dipsToPixels(5);  
	private int mMarkSize = Services.Device.dipsToPixels(3);  
	
	protected ThemeClassDefinition mThemeClass;

	public ChartSurface(Context context) {
		super(context);
		init();
	}


	public ChartSurface(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public void setSpec(GaugeSpecification spec) {
		mSpec = spec;
	}

	private void init() {
		setSpec(null);
	}

	@Override
	public void onDraw(Canvas canvas) {
		doDraw(canvas);
	}


	private void doDraw(Canvas canvas) {
		if (mSpec == null)
			return;

		Integer defaultTextColor = ThemeUtils.getAndroidThemeColorId(this.getContext(), android.R.attr.textColorPrimary);
		// if null, should not, use a default color visible on light and dark background.
		if (defaultTextColor==null)
			defaultTextColor = Color.argb(255, 192, 92, 0);
		
		mWidth = getWidth() - mMargin;
		// use Height definition as in dips
		int height = Services.Device.dipsToPixels(mSpec.Height);
		if (height == 0)
			height = getHeight() / 2;
		if (mWidth == 0)
			return;
		Paint p = new Paint() { { setStyle(Paint.Style.STROKE);
		setAntiAlias(true);
		}};
		p.setColor(defaultTextColor);

		ThemeClassDefinition themeClass = mThemeClass; // get the att/var class
		
		//Keep just for compatibility, could be removed...
		if (themeClass==null)
			themeClass = PlatformHelper.getThemeClass("Attribute.SDLinearGaugeText");

		if(themeClass != null)
		{
			//Text Size
			Integer textSizef = themeClass.getFont().getFontSize();
			if (textSizef != null && textSizef > 0)
			{
				p.setTextSize(textSizef);
			}
			else
			{
				p.setTextSize(Services.Device.dipsToPixels(12));
			}
			// Text Color
			Integer colorId = ThemeUtils.getColorId(themeClass.getColor());
			//Integer highlightedColorId = ThemeUtils.getColorId(themeClass.getHighlightedColor());
			if (colorId != null /*|| highlightedColorId != null*/)
			{
				p.setColor(colorId);
			}
		}
		else
		{
			p.setTextSize(Services.Device.dipsToPixels(12));
		}

		float fontHeight = p.getTextSize();

		float ShowMinMaxYSize = p.getTextSize() + (mMargin);
		// Min/ Max Value
		if (mSpec.ShowMinMax) 
		{
			Paint pLegends = new Paint() { { setStyle(Paint.Style.STROKE);  } };
			pLegends.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
			// Draw Text of Min
			canvas.drawText(String.valueOf(mSpec.MinValue), 0, ShowMinMaxYSize, p);
			// Draw Maximum
			String maximumLabel = String.valueOf(mSpec.MaxValue);
			int labelWidth = (int) p.measureText(maximumLabel);
			canvas.drawText(maximumLabel, mWidth - labelWidth, ShowMinMaxYSize, p);
		}

		int currentX = 0;
		boolean hasRangeNames = false;
		for (int i = 0; i < mSpec.Ranges.size() ; i++) {
			RangeSpec range = mSpec.Ranges.get(i);
			if (Strings.hasValue(range.Name))
				hasRangeNames = true;
		}
		// use a default color visible on light and dark background.
		// Override with range color if possible.
		int currentValueColor = Color.argb(255, 192, 92, 0);
		float rangeStartValue = mSpec.MinValue;
		
		for (int i = 0; i < mSpec.Ranges.size() ; i++) {
			RangeSpec range = mSpec.Ranges.get(i);
			int widthRange = getWidth(range.Length);
			int elementY = height;
			if (mSpec.ShowMinMax || hasRangeNames) 
				elementY += ShowMinMaxYSize;
			elementY += 5;
			ColumnElement element = new ColumnElement(currentX, elementY, height, widthRange, range.Color );
			element.drawElement(canvas);
			int textWidth = (int) p.measureText(range.Name.trim());
			int xText = (currentX + (widthRange / 2)) - (textWidth / 2);
			canvas.drawText(range.Name.trim(), xText, ShowMinMaxYSize, p);
			currentX += widthRange;
			if (rangeStartValue <= mSpec.CurrentValue && mSpec.CurrentValue <= (rangeStartValue+range.Length))
				currentValueColor = range.Color;
			rangeStartValue = rangeStartValue + range.Length;
		}
		p.setStyle(Paint.Style.FILL_AND_STROKE);
		p.setStrokeWidth(2);
		p.setColor(Color.YELLOW);
		Path path = new Path();
		path.moveTo(0, -10);
		path.lineTo(5, 0);
		path.lineTo(-5, 0);
		path.close();
		int x = getWidth(mSpec.CurrentValue - mSpec.MinValue);
		int y = (int) (height + fontHeight);
		if (mSpec.ShowMinMax || hasRangeNames) 
			y += ShowMinMaxYSize;
		Rect rect = new Rect(x, y, x + mMarkSize, y + mMarkSize);
		p.setShader(ColumnElement.getColumnShader(currentValueColor, rect));
		path.offset(x, y);
		canvas.drawPath(path, p);
		p.setStrokeWidth(1);

		String valueText = String.valueOf(mSpec.CurrentValue);
		float valueLength = p.measureText(valueText);
		if (x + valueLength > mWidth)
			x = (int) (mWidth - valueLength);

		Paint pText = new Paint() {
			{ 	setStyle(Paint.Style.STROKE);
				setAntiAlias(true);
		}};
		pText.setColor(defaultTextColor);
		pText.setTextSize(fontHeight);

		// Current Value color
		if(themeClass != null)
		{
			// Text Color
			Integer colorId = ThemeUtils.getColorId(themeClass.getColor());
			//	Integer highlightedColorId = ThemeUtils.getColorId(themeClass.getHighlightedColor());
			if (colorId != null /*|| highlightedColorId != null*/)
			{
				pText.setColor(colorId);
			}
		}

		canvas.drawText(valueText, Math.max( x - (valueLength/2), 0), y + mMarkSize + fontHeight, pText);
	}

	private int getWidth(float value) {
		if (mSpec.MaxValue > mSpec.MinValue)
			return (int) ((value * mWidth) / (mSpec.MaxValue - mSpec.MinValue));
		return (int) ((value * mWidth) / (mSpec.MaxValue));
	}
}
