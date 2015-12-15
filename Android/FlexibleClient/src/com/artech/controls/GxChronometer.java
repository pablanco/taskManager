package com.artech.controls;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.widget.Chronometer;

import com.artech.base.controls.IGxControlRuntime;
import com.artech.base.metadata.layout.ControlInfo;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.services.IValuesFormatter;
import com.artech.base.services.Services;
import com.artech.ui.Coordinator;
import com.artech.utils.BackgroundOptions;
import com.artech.utils.ThemeUtils;

@SuppressLint("DefaultLocale")
public class GxChronometer extends android.widget.Chronometer implements IGxEdit, IGxThemeable, IGxControlRuntime,
	android.widget.Chronometer.OnChronometerTickListener
{
	private static final int DEFAULT_INTERVAL = 1; // second

	private LayoutItemDefinition mDefinition;
	private ThemeClassDefinition mClassDefinition;
	private Coordinator mCoordinator;
	private long mMilliseconds = 0;
	private long mInterval = DEFAULT_INTERVAL;
	private int mTicks = -1;

	public GxChronometer(Context context, Coordinator coordinator, LayoutItemDefinition definition)
	{
		this(context, coordinator, definition, null);
	}

	public GxChronometer(Context context, Coordinator coordinator, LayoutItemDefinition definition, IValuesFormatter formatter)
	{
		this(context);
		mDefinition = definition;
		mCoordinator = coordinator;

		ControlInfo info = definition.getControlInfo();
		if (info != null)
			mInterval = Services.Strings.tryParseLong(info.optStringProperty("@SDChronometerTickInterval"), DEFAULT_INTERVAL); //$NON-NLS-1$
		else
			mInterval = DEFAULT_INTERVAL;
	}


	public GxChronometer(Context context)
	{
		super(context);
	}

	public GxChronometer(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	@Override
	public String getGx_Value() {
		// Value in seconds
		return Long.toString((mMilliseconds) / 1000);
	}

	@Override
	public void setGx_Value(String value)
	{
		mMilliseconds = Long.parseLong(value) * 1000;
	}

	@Override
	public String getGx_Tag() {
		return (String)this.getTag();
	}

	@Override
	public void setGx_Tag(String data) {
		this.setTag(data);
	}

	@Override
	public void setValueFromIntent(Intent data) { }


	@Override
	public IGxEdit getViewControl() {
		return this;
	}

	@Override
	public IGxEdit getEditControl() {
		return this;
	}

	@Override
	public void setThemeClass(ThemeClassDefinition themeClass)
	{
		mClassDefinition = themeClass;
		applyClass(themeClass);
	}

	@Override
	public ThemeClassDefinition getThemeClass() {
		return mClassDefinition;
	}

	@Override
	public void applyClass(ThemeClassDefinition themeClass) {
		//set font properties
		ThemeUtils.setFontProperties(this, themeClass);

		//set background and border properties
		ThemeUtils.setBackgroundBorderProperties(this, themeClass, BackgroundOptions.defaultFor(mDefinition));
	}

	@Override
	public void setProperty(String name, Object value) {
	}

	@Override
	public Object getProperty(String name) {
		return null;
	}

	public void reset() {
		mMilliseconds = 0;
		mTicks = -1;
		setBase(SystemClock.elapsedRealtime());
	}

	@Override
	public void start() {
		setBase(SystemClock.elapsedRealtime() - mMilliseconds);
		setOnChronometerTickListener(this);
		super.start();
	}

	@Override
	public void stop() {
		mTicks = -1;
		super.stop();
	}

	@Override
	public void runMethod(String name, List<Object> parameters) {

			Method method = null;
			try {
				method = this.getClass().getDeclaredMethod(name.toLowerCase());
			} catch (NoSuchMethodException e) {
	}
			try {
				if (method != null)
					method.invoke(this);
			} catch (IllegalArgumentException e) {
			} catch (IllegalAccessException e) {
			} catch (InvocationTargetException e) {
			}

	}

	@Override
	public void onChronometerTick(Chronometer chronometer) {
		mMilliseconds = SystemClock.elapsedRealtime() - getBase();
		mTicks++;
		if (mMilliseconds > 0 && mCoordinator != null && mTicks == mInterval) {
			mCoordinator.runControlEvent(this, "Tick"); //$NON-NLS-1$
			mTicks = 0;
		}
	}

	@Override
	public boolean isEditable()
	{
		return true; // Although not editable, its value changes, so it needs to be read before executing actions.
	}
}