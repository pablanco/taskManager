package com.artech.controls;

import java.util.Calendar;
import java.util.Date;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;

import com.artech.base.metadata.enums.DataTypes;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.services.Services;
import com.artech.ui.Coordinator;

public class GxInPlaceDatePicker extends DateTimePicker implements IGxEdit {

	private boolean mShowDate= true;
	private boolean mShowTime= false;
	private Context mContext;
	private LayoutItemDefinition mDefinition;
	private Calendar currentValue;
	private String mInputType;
	private String mAttType;

	private boolean useMinDate = false;
	private long mMinDate;
	private boolean useMaxDate = false;
	private long mMaxDate;

	public GxInPlaceDatePicker(Context context, Coordinator coordinator, LayoutItemDefinition def) {
		super(context);
		initialize(context);
		setLayoutDefinition(def);
		setCoordinator(coordinator, this);
	}

	public GxInPlaceDatePicker(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialize(context);
	}
	public GxInPlaceDatePicker(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initialize(context);
	}

	private void initialize(Context context)
	{
		mContext = context;
		// Check is system is set to use 24h time (this doesn't seem to work as expected though)
		final String timeS = android.provider.Settings.System.getString(mContext.getContentResolver(), android.provider.Settings.System.TIME_12_24);
		final boolean is24h = !(timeS == null || timeS.equals("12")); //$NON-NLS-1$
		
		// Setup TimePicker
		setIs24HourView(is24h);

		setShowDate(mShowDate);
		setShowTime(mShowTime);

		// If set min and/or max date
		if (useMinDate || useMaxDate)
			setDateTimeValueRangeHelper();
		if (useMinDate)
			setMinDate(mMinDate);
		if (useMaxDate)
			setMaxDate(mMaxDate);

		if (currentValue!=null)
			setCurrentValue(currentValue);
	}

	private static Date calendarToDate(Calendar calendar)
	{
		calendar.set(Calendar.SECOND, 0);
		return calendar.getTime();
	}

	@Override
	public String getGx_Value() {
		Calendar calendar = getCalendar();
		Date date = calendarToDate(calendar);
		return Services.Strings.getDateTimeStringForServer(date);
	}

	@Override
	public void setGx_Value(String value) {
		if (value!=null && value.length()>0)
		{
			Date date ;
			if (mAttType.equals(DataTypes.date))
			{
				date = Services.Strings.getDate(value);
			} else if (mAttType.equals(DataTypes.dtime) || mAttType.equals(DataTypes.datetime))
			{
				date = Services.Strings.getDateTime(value);
			}else if (mAttType.equals(DataTypes.time))
			{
				date = Services.Strings.getDateTime(value, true);
			}
			else{
				date = Services.Strings.getDate(value);
			}
			if (date!=null)
			{
				Calendar calendarInstance = Calendar.getInstance();
				calendarInstance.setTime(date);
				currentValue = calendarInstance;
				setCurrentValue(currentValue);
			}
		}
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
	public void setValueFromIntent(Intent data) {
	}

	@Override
	public IGxEdit getViewControl()
	{
		return new GxTextView(getContext(), mDefinition);
	}

	@Override
	public IGxEdit getEditControl() {
		return this;
	}

	private void setLayoutDefinition(LayoutItemDefinition layoutItemDefinition) {
		mDefinition = layoutItemDefinition;
		String attributeType = mDefinition.getDataTypeName().GetDataType();
		String attributeInputType = mDefinition.getDataItem().getInputPicture();
		setInputType(attributeInputType);
		//Define if you a date, date/time or time
		setAttType(attributeType);
	}
	
	private void setInputType(String inputType)
	{
		mInputType = inputType;
	}

	private void setAttType(String attType)
	{
		mAttType = attType;
		if (mAttType.equals(DataTypes.date)){
			mShowDate = true;
			mShowTime = false;
		} else if (mAttType.equals(DataTypes.dtime) || mAttType.equals(DataTypes.datetime)){
			mShowTime = true;
			mShowDate = !(mInputType != null && mInputType.length() <= 5);
		}else if (mAttType.equals(DataTypes.time))	{
			mShowDate = false;
			mShowTime = true;
		}
	}


	@Override
	public void setMinDate(long minDate) {
		useMinDate = true;
		mMinDate = minDate;
	}

	@Override
	public void setMaxDate(long maxDate) {
		useMaxDate = true;
		mMaxDate = maxDate;
	}

	@Override
	public boolean isEditable()
	{
		return isEnabled(); // Editable when enabled.
	}
}

