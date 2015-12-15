package com.artech.controls;

import java.util.Calendar;
import java.util.Date;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.support.v7.widget.AppCompatButton;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.TimePicker;

import com.artech.R;
import com.artech.base.metadata.enums.DataTypes;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.artech.common.FormatHelper;
import com.artech.ui.Coordinator;

public class GxDateTimeEdit extends LinearLayout implements IGxEdit
{
	private final Coordinator mCoordinator;
	private final LayoutItemDefinition mDefinition;

	private Button mDateButton;
	private Button mTimeButton;

	private String mDataType; // Date, Time, DateTime
	private String mPicture;
	private boolean mShowDate;
	private boolean mShowTime;

	private Date mValue;

	public GxDateTimeEdit(Context context, Coordinator coordinator, LayoutItemDefinition definition)
	{
		super(context);
		mCoordinator = coordinator;
		mDataType = DataTypes.date;
		createButtons();

		mDefinition = definition;
		String dataType = mDefinition.getDataTypeName().GetDataType();
		String picture = mDefinition.getDataItem().getInputPicture();
		setDataType(dataType, picture);

		updateText();
	}

	private void createButtons()
	{
		mDateButton = new AppCompatButton(getContext());
		mTimeButton = new AppCompatButton(getContext());

		setOrientation(HORIZONTAL);
		addView(mDateButton, new LayoutParams(0, LayoutParams.MATCH_PARENT, 1f));
		addView(mTimeButton, new LayoutParams(0, LayoutParams.MATCH_PARENT, 1f));

		mDateButton.setTransformationMethod(null); // Remove all-caps in Android 5.0
		mTimeButton.setTransformationMethod(null); // Remove all-caps in Android 5.0

		mDateButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v) { showDateDialog(null); }
		});

		mTimeButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v) { showTimeDialog(null); }
		});
	}

	public void setDataType(String dataType, String picture)
	{
		mDataType = dataType;
		mPicture = picture;

		if (mDataType.equals(DataTypes.date))
		{
			mShowDate = true;
			mShowTime = false;
		}
		else if (mDataType.equals(DataTypes.dtime) || mDataType.equals(DataTypes.datetime))
		{
			mShowDate = true;
			mShowTime = true;
			if (Strings.hasValue(mPicture) && mPicture.length() <= 5) // Special case, datetime with time picture.
				mShowDate = false;
		}
		else if (mDataType.equals(DataTypes.time))
		{
			mShowDate = false;
			mShowTime = true;
		}
		else
		{
			// Default is date
			Services.Log.warning("Unexpected datatype: " + dataType);
			mShowDate = true;
			mShowTime = false;
		}

		mDateButton.setVisibility(mShowDate ? VISIBLE : GONE);
		mTimeButton.setVisibility(mShowTime ? VISIBLE : GONE);
	}

	private void showDateDialog(DatePickerDialog.OnDateSetListener customListener)
	{
		Calendar c = getCalendar();

		DatePickerDialog.OnDateSetListener listener = (customListener != null ? customListener : mOnDateSetListener);
		DatePickerDialog dialog = new DatePickerDialog(getContext(), listener, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
		dialog.show();
	}

	private final DatePickerDialog.OnDateSetListener mOnDateSetListener = new DatePickerDialog.OnDateSetListener()
	{
		@Override
		public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth)
		{
			// OK pressed in DatePicker.
			Calendar c = getCalendar();
			c.set(Calendar.YEAR, year);
			c.set(Calendar.MONTH, monthOfYear);
			c.set(Calendar.DAY_OF_MONTH, dayOfMonth);

			updateValueFromCalendar(c);
		}
	};

	private void showTimeDialog(TimePickerDialog.OnTimeSetListener customListener)
	{
		Calendar c = getCalendar();
		final String timeS = Settings.System.getString(getContext().getContentResolver(), Settings.System.TIME_12_24);
		final boolean is24HourView = timeS == null || !timeS.equals("12");

		TimePickerDialog.OnTimeSetListener listener = (customListener != null ? customListener : mOnTimeSetListener);
		TimePickerDialog dialog = new TimePickerDialog(getContext(), listener, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), is24HourView);
		dialog.show();
	}

	private final TimePickerDialog.OnTimeSetListener mOnTimeSetListener = new TimePickerDialog.OnTimeSetListener()
	{
		@Override
		public void onTimeSet(TimePicker view, int hourOfDay, int minute)
		{
			// OK pressed in TimePicker.
			Calendar c = getCalendar();
			c.set(Calendar.HOUR_OF_DAY, hourOfDay);
			c.set(Calendar.MINUTE, minute);
			c.set(Calendar.SECOND, 0);

			updateValueFromCalendar(c);
		}
	};

	private Calendar getCalendar()
	{
		Calendar c = Calendar.getInstance();
		if (mValue != null)
			c.setTime(mValue);

		return c;
	}

	private void updateValueFromCalendar(Calendar c)
	{
		Date newValue = c.getTime();
		if (!newValue.equals(mValue))
		{
			mValue = newValue;
			updateText();

			// Fire ControlValueChanged.
			if (mCoordinator != null)
				mCoordinator.onValueChanged(GxDateTimeEdit.this, true);
		}
	}

	@Override
	public String getGx_Value()
	{
		if (mDataType.equals(DataTypes.date))
			return Services.Strings.getDateStringForServer(mValue);
		else if (mDataType.equals(DataTypes.time))
			return Services.Strings.getDateTimeStringForServer(mValue, true);
		else
			return Services.Strings.getDateTimeStringForServer(mValue);
	}

	private String getText()
	{
		return FormatHelper.formatDate(mValue, mDataType, mPicture);
	}

	@Override
	public void setGx_Value(String value)
	{
		if (mDataType.equals(DataTypes.date))
			mValue = Services.Strings.getDate(value);
		else if (mDataType.equals(DataTypes.time))
			mValue = Services.Strings.getDateTime(value, true);
		else
			mValue = Services.Strings.getDateTime(value);

		updateText();
	}

	private void updateText()
	{
		if (mValue != null)
		{
			if (mShowDate && mShowTime)
			{
				String dateText = Services.Strings.getDateString(mValue, mPicture);
				String timeText = Services.Strings.getTimeString(mValue, mPicture);

				mDateButton.setText(dateText);
				mTimeButton.setText(timeText);
			}
			else
			{
				String text = getText();

				if (mShowDate)
					mDateButton.setText(text);
				else if (mShowTime)
					mTimeButton.setText(text);
			}
		}
		else
		{
			mDateButton.setText(R.string.GXM_Date);
			mTimeButton.setText(R.string.GXM_Time);
		}
	}

	@Override
	public String getGx_Tag()
	{
		return (String)getTag();
	}

	@Override
	public void setGx_Tag(String tag)
	{
		setTag(tag);
	}

	@Override
	public void setValueFromIntent(Intent data) { }

	@Override
	public boolean isEditable()
	{
		return isEnabled(); // Editable when enabled.
	}

	@Override
	public IGxEdit getEditControl()
	{
		return this;
	}

	@Override
	public IGxEdit getViewControl()
	{
		return new GxTextView(getContext(), mDefinition);
	}

	public void showDateTimeDialog(final OnDateTimeChangedListener listener)
	{
		if (listener == null)
			throw new IllegalArgumentException("Listener cannot be null.");

		if (mShowDate)
		{
			showDateDialog(new DatePickerDialog.OnDateSetListener()
			{
				@Override
				public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth)
				{
					mOnDateSetListener.onDateSet(view, year, monthOfYear, dayOfMonth);

					// Either continue to time dialog or finish here.
					if (mShowTime)
					{
						showTimeDialog(new TimePickerDialog.OnTimeSetListener()
						{
							@Override
							public void onTimeSet(TimePicker view, int hourOfDay, int minute)
							{
								mOnTimeSetListener.onTimeSet(view, hourOfDay, minute);
								listener.onDateTimeChanged(getGx_Value(), getText());
							}
						});
					}
					else
						listener.onDateTimeChanged(getGx_Value(), getText());

				}
			});
		}
		else if (mShowTime)
		{
			showTimeDialog(new TimePickerDialog.OnTimeSetListener()
			{
				@Override
				public void onTimeSet(TimePicker view, int hourOfDay, int minute)
				{
					mOnTimeSetListener.onTimeSet(view, hourOfDay, minute);
					listener.onDateTimeChanged(getGx_Value(), getText());
				}
			});
		}
	}

	public interface OnDateTimeChangedListener
	{
		void onDateTimeChanged(String value, String text);
	}
}
