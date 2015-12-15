package com.artech.controls;

import java.text.NumberFormat;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.artech.base.controls.IGxControlRuntime;
import com.artech.base.metadata.layout.ControlInfo;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.services.Services;
import com.artech.ui.Coordinator;
import com.artech.utils.ThemeUtils;

public class SeekBarControl extends LinearLayout implements IGxEdit, IGxThemeable, IGxControlRuntime
{
	private final static String PROPERTY_MIN_VALUE = "MinValue";
	private final static String PROPERTY_MAX_VALUE = "MaxValue";
	private final static String PROPERTY_STEP = "Step";

	private LayoutItemDefinition mDefinition;
	private Coordinator mCoordinator;

	private TextView mTextCurrent;
	private TextView mTextMin;
	private TextView mTextMax;
	private SeekBar mSeekBar;
	private Drawable mDefaultProgressDrawable;
	private String mLastValue;

	private int mMaxValue;
	private double seekBarMinValue;
	private double seekBarMaxValue;
	private double seekBarStep;
	private ThemeClassDefinition mThemeClass;

	public SeekBarControl(Context context, Coordinator coordinator, LayoutItemDefinition def) {
		super(context);
		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    if (inflater != null)
	       inflater.inflate(com.artech.R.layout.seekbarcontrol, this, true);
	    mCoordinator = coordinator;
	    setLayoutDefinition(def);
	    init();
	}

	public SeekBarControl(Context context, AttributeSet attrs) {
		super(context, attrs);
		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    if(inflater != null)
	       inflater.inflate(com.artech.R.layout.seekbarcontrol, this, true);

	    init();
	}

	@Override
	public String getGx_Value() {
		return getCurrentValue();
	}

	@Override
	public void setGx_Value(String value) {
		try {
			setCurrentValue(value);
			mLastValue = getCurrentValue();
		} catch (Exception e) {
			Services.Log.Error("Unable to set value to slider " + value, e); //$NON-NLS-1$
		}
	}

	@Override
	public String getGx_Tag() {
		return getTag().toString();
	}

	@Override
	public void setGx_Tag(String tag) {
		setTag(tag);
	}

	@Override
	public void setThemeClass(ThemeClassDefinition themeClass)
	{
		if (themeClass == null)
			return;

		mThemeClass = themeClass;
		applyClass(themeClass);
	}

	@Override
	public ThemeClassDefinition getThemeClass() {
		return mThemeClass;
	}

	private static Drawable createProgressDrawable(ThemeClassDefinition themeClass, Drawable defaultDrawable)
	{
		Integer progressColor = ThemeUtils.getColorId(themeClass.getColor());
		if (progressColor != null && defaultDrawable instanceof LayerDrawable)
		{
			LayerDrawable previous = (LayerDrawable)defaultDrawable;
			Drawable[] themedLayers = new Drawable[previous.getNumberOfLayers()];

			// We only want to change the foreground (progress) layer, leaving everything else as it is.
			for (int i = 0; i < previous.getNumberOfLayers(); i++)
			{
				Drawable previousLayer = previous.getDrawable(i);
				Drawable themedLayer;

				if (previous.getId(i) == android.R.id.progress)
				{
					// Create a gradient by "blackening" the color (60% value).
					float[] progressHsv = new float[3];
					Color.colorToHSV(progressColor, progressHsv);
					progressHsv[2] = (float)(progressHsv[2] * 0.6);
					int progressGradientColor = Color.HSVToColor(Color.alpha(progressColor), progressHsv);

					// Adapted from http://stackoverflow.com/a/12997264/82788
					GradientDrawable color = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[] { progressColor, progressGradientColor });
					color.setCornerRadius(8);

					themedLayer = new ClipDrawable(color, Gravity.LEFT, ClipDrawable.HORIZONTAL);
				}
				else
					themedLayer = previousLayer;

				themedLayers[i] = themedLayer;
			}

			// Build up the new LayerDrawable.
			LayerDrawable themed = new LayerDrawable(themedLayers);
			for (int i = 0; i < previous.getNumberOfLayers(); i++)
				themed.setId(i, previous.getId(i));

			return themed;
		}
		else
			return defaultDrawable;
	}


	private void init()
	{
		mSeekBar = (SeekBar) findViewById(com.artech.R.id.seekBar);
		mTextCurrent = (TextView) findViewById(com.artech.R.id.textCurrent);
		mTextMin = (TextView) findViewById(com.artech.R.id.textMin);
		mTextMax = (TextView) findViewById(com.artech.R.id.textMax);
		mDefaultProgressDrawable = mSeekBar.getProgressDrawable();

		// Do not show texts as specified
		mTextCurrent.setVisibility(GONE);
		mTextMin.setVisibility(GONE);
		mTextMax.setVisibility(GONE);

		mSeekBar.setMax(mMaxValue);
		mSeekBar.incrementProgressBy(1);
		mTextMin.setText(String.valueOf(seekBarMinValue));
		mTextMax.setText(String.valueOf(seekBarMaxValue));

		mLastValue = "";

		final View control = this;
		mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
		{
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
			{
				if (mTextCurrent.isShown())
					mTextCurrent.setText(getCurrentValue());
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) { }

			@Override
			public void onStopTrackingTouch(SeekBar seekBar)
			{
				if (mCoordinator!=null)
					mCoordinator.runControlEvent(control, "ValueChanged");

				if (!getCurrentValue().equals(mLastValue) && mCoordinator != null) {
					mCoordinator.onValueChanged(SeekBarControl.this, true);
				}

				mLastValue = getCurrentValue();
			}
		});
	}

	@Override
	public void setValueFromIntent(Intent data) { }

	@Override
	public void setEnabled(boolean enabled)
	{
		mSeekBar.setEnabled(enabled);
		mTextMin.setEnabled(enabled);
		mTextMax.setEnabled(enabled);
		mTextCurrent.setEnabled(enabled);
	}

	@Override
	public IGxEdit getViewControl()
	{
		setEnabled(false);
		return this;
	}

	@Override
	public IGxEdit getEditControl()
	{
		return this;
	}

	private double tryParse(final String strValue, final double defaultValue)  {
		try {
			return Double.parseDouble(strValue);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	private void setCurrentValue(String value)
	{
		int numberOfDecimals = mDefinition.getDataItem().getDecimals();
		double val = Double.parseDouble(value);

		int progress;
		if (seekBarStep == 0)
			progress = (int) ((val - seekBarMinValue) * Math.pow(10, numberOfDecimals));
		else
			progress = (int) ((val - seekBarMinValue) / seekBarStep);

		mSeekBar.setProgress(progress);
	}

	private String getCurrentValue()
	{
		int numberOfDecimals = mDefinition.getDataItem().getDecimals();
		NumberFormat nf = NumberFormat.getInstance();
		nf.setGroupingUsed(false);
        nf.setMaximumFractionDigits(numberOfDecimals);
		if (seekBarStep == 0) {
			return nf.format((seekBarMinValue + (mSeekBar.getProgress() / Math.pow(10, numberOfDecimals))));
		}
		else {
			return nf.format((seekBarMinValue + (mSeekBar.getProgress() * seekBarStep)));
		}
	}

	private void calculateSettings(LayoutItemDefinition layoutItemDefinition) {
		int numberOfDecimals = layoutItemDefinition.getDataItem().getDecimals();
		if (seekBarStep == 0) {
				int maxValue = (int) (seekBarMaxValue *  Math.pow(10, numberOfDecimals));
				int minValue = (int) (seekBarMinValue * Math.pow(10, numberOfDecimals));
				mMaxValue = maxValue - minValue;
		} else
			if (seekBarStep > 0)
				mMaxValue = (int) ((seekBarMaxValue - seekBarMinValue) / seekBarStep);
	}

	private void updateSeekbar()
	{
		calculateSettings(mDefinition);
		mSeekBar.setMax(mMaxValue);
	}

	private void setLayoutDefinition(LayoutItemDefinition layoutItemDefinition)
	{
		mDefinition = layoutItemDefinition;
		ControlInfo info = layoutItemDefinition.getControlInfo();
		if (info == null)
			return;

		try
		{
			seekBarMinValue = tryParse(info.optStringProperty("@SDSliderMinValue"), 0); //$NON-NLS-1$
			seekBarMaxValue = tryParse(info.optStringProperty("@SDSliderMaxValue"), 10);	 //$NON-NLS-1$
			seekBarStep = tryParse(info.optStringProperty("@SDSliderStep"), 0);		 //$NON-NLS-1$
			if (seekBarMinValue > seekBarMaxValue) {
				resetValues();
			}
			calculateSettings(layoutItemDefinition);
		} catch (Exception e) {
			Services.Log.Error("Unable to set slider properties", e); //$NON-NLS-1$
		}
	}

	private void resetValues() {
		seekBarMinValue = 0;
		seekBarMaxValue = 10;
		seekBarStep = 1;
	}

	@Override
	public void setProperty(String name, Object value)
	{
		boolean isChanged = false;
		String currentValue = getGx_Value();

		if (PROPERTY_MIN_VALUE.equalsIgnoreCase(name) && value != null)
		{
			seekBarMinValue = tryParse(value.toString(), 0);
			isChanged = true;
		}
		else if (PROPERTY_MAX_VALUE.equalsIgnoreCase(name) && value != null)
		{
			seekBarMaxValue = tryParse(value.toString(), 10);
			isChanged = true;
		}
		else if (PROPERTY_STEP.equalsIgnoreCase(name) && value != null)
		{
			seekBarStep = tryParse(value.toString(), 0);
			isChanged = true;
		}

		if (isChanged)
		{
			// Update calculations and visual position in seekbar.
			updateSeekbar();
			setCurrentValue(currentValue);
		}
	}

	@Override
	public Object getProperty(String name)
	{
		if (PROPERTY_MIN_VALUE.equalsIgnoreCase(name))
			return Double.toString(seekBarMinValue);
		else if (PROPERTY_MAX_VALUE.equalsIgnoreCase(name))
			return Double.toString(seekBarMaxValue);
		else if (PROPERTY_STEP.equalsIgnoreCase(name))
			return Double.toString(seekBarStep);
		else
			return null;
	}

	@Override
	public void runMethod(String methodName, List<Object> parameters)
	{
		// No methods.
	}

	@Override
	public void applyClass(ThemeClassDefinition themeClass) {
		Drawable progressDrawable = createProgressDrawable(themeClass, mDefaultProgressDrawable);
		mSeekBar.setProgressDrawable(progressDrawable);
	}

	@Override
	public boolean isEditable()
	{
		return isEnabled(); // Editable when enabled.
	}
}
