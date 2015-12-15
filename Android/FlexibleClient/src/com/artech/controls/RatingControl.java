package com.artech.controls;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.RatingBar;

import com.artech.base.metadata.layout.ControlInfo;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.services.Services;

public class RatingControl extends LinearLayout implements IGxEdit
{
	private Context mContext;

	private RatingBar mRatingBar;
	private RatingBar mSmallRatingBar;
	private GxRatingView mGxRatingView;
	private LinearLayout mPreLinearLayout;
	private LinearLayout mPostLinearLayout;

	private String mStep;
	private String mMaxValue;

	private float mStepGx;

	final class RatingBarType {
		public static final int AndroidRatingBar = 0; //mRatingBar
		public static final int GxRatingBar = 1; //mGxRatingView
	}
	private int mUseRatingBar;

	public RatingControl(Context context, LayoutItemDefinition def)
	{
		super(context);
		initialize(context);
		setLayoutDefinition(def);
		setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
	}

	public RatingControl(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		initialize(context);
		setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
	}

	public RatingControl(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs);
		initialize(context);
		setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
	}

	private void initialize(Context context) {
		mContext = context;

		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if(inflater != null){
			inflater.inflate(com.artech.R.layout.ratingcontrol, this, true);
		}

		mRatingBar = (RatingBar) findViewById(com.artech.R.id.ratingbar);
		mGxRatingView = (GxRatingView) findViewById(com.artech.R.id.gxRatingbarView);
		mSmallRatingBar = (RatingBar) findViewById(com.artech.R.id.smallratingbar);

		mPreLinearLayout = (LinearLayout) findViewById(com.artech.R.id.layoutPreRatingBar);
		mPostLinearLayout = (LinearLayout) findViewById(com.artech.R.id.layoutPostRatingBar);

	}

	@Override
	public String getGx_Value() {
		switch (mUseRatingBar) {
		case RatingBarType.AndroidRatingBar :
			return Integer.toString(Math.round(mRatingBar.getRating() * mStepGx));
		case RatingBarType.GxRatingBar :
			return Integer.toString(Math.round(mGxRatingView.getRating() * mStepGx));
		}
		return Integer.toString(Math.round(mRatingBar.getRating() * mStepGx));
	}

	@Override
	public void setGx_Value(String value) {
		if (Services.Strings.hasValue(value)) {
			mRatingBar.setRating(Float.parseFloat(value) / mStepGx);
			mGxRatingView.setRating(Float.parseFloat(value) / mStepGx);
			mSmallRatingBar.setRating(Float.parseFloat(value) / mStepGx);
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
	public void setValueFromIntent(Intent data) {

	}

	@Override
	public void setEnabled(boolean enabled) {

		switch (mUseRatingBar) {
		case RatingBarType.AndroidRatingBar :
			setEnabled(mRatingBar, enabled);
			break;
		case RatingBarType.GxRatingBar :
			setEnabled(mGxRatingView, enabled);
			setEnabled(mPreLinearLayout, enabled);
			setEnabled(mPostLinearLayout, enabled);
			break;
		}

	}

	private void setEnabled(View view, boolean enabled) {
		view.setEnabled(enabled);
		view.setFocusable(enabled);
	}

	@Override
	public IGxEdit getViewControl() {
		switch (mUseRatingBar) {
		case RatingBarType.AndroidRatingBar :
			setEnabled(mRatingBar, false);
			break;
		case RatingBarType.GxRatingBar :
			setEnabled(mGxRatingView, false);
			setEnabled(mPreLinearLayout, false);
			setEnabled(mPostLinearLayout, false);
			break;
		}
		return this;
	}

	@Override
	public IGxEdit getEditControl() {
		return this;
	}

	public void prepareForList()
	{
		float step = getStepValue();
		int maxValue = getMaxValue();

		mSmallRatingBar.setStepSize(1);
		mSmallRatingBar.setNumStars((int) (maxValue / step));
		mSmallRatingBar.setMax((int) (maxValue / step));
		LayoutParams param = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		mSmallRatingBar.setLayoutParams(param);
		setEnabled(mSmallRatingBar, false);

		mRatingBar.setVisibility(GONE);
		mSmallRatingBar.setVisibility(VISIBLE);
		mGxRatingView.setVisibility(GONE);
		mPreLinearLayout.setVisibility(GONE);
		mPostLinearLayout.setVisibility(GONE);
	}

	private float getStepValue() {
		float step = 1;
		if (mStep.length() > 0) {
			try {
				step = Float.parseFloat(mStep);
			}
			catch (NumberFormatException ex) {
				Services.Log.Error("formatStep", ex); //$NON-NLS-1$
			}
		}
		return step;
	}

	private int getMaxValue() {
		int maxValue = 5;
		if (mMaxValue.length() > 0) {
			try {
				maxValue = Integer.parseInt(mMaxValue);
			}
			catch (NumberFormatException ex) {
				Services.Log.Error("value", ex); //$NON-NLS-1$
			}
		}
		return maxValue;
	}


	private void setLayoutDefinition(LayoutItemDefinition layoutItemDefinition) {
		ControlInfo info = layoutItemDefinition.getControlInfo();
		if (info == null)
			return;
		mStepGx = 1;
		mStep = info.optStringProperty("@RatingStep");		 //$NON-NLS-1$
		mStepGx = getStepValue();

		mMaxValue = info.optStringProperty("@RatingMaxValue"); //$NON-NLS-1$
		int maxValue = getMaxValue();

		int totalStar = (int) (maxValue / mStepGx);

		selectStar(totalStar);

		switch (mUseRatingBar) {
		case RatingBarType.AndroidRatingBar :
			mRatingBar.setStepSize(1);
			mRatingBar.setNumStars(totalStar);
			mRatingBar.setMax(totalStar);
			break;
		case RatingBarType.GxRatingBar :
			mGxRatingView.setStepSize(1);
			mGxRatingView.setNumStars(totalStar);
			mGxRatingView.setMax(totalStar);
			break;
		}
	}

	private void selectStar(int totalStar) {
		//Use the Android Rating
		mUseRatingBar = RatingBarType.AndroidRatingBar; //mRatingBar

		if (totalStar > 5)
		{
			DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
			float displayWidth = displayMetrics.widthPixels;
			Services.Log.info("Display Width", String.valueOf(displayWidth)); //$NON-NLS-1$
			float sizeStar = (displayWidth / totalStar) - 15;

			if (sizeStar < 66)
			{
				//Use the Genexus Rating
				mGxRatingView.generateGxRating(sizeStar);
				mUseRatingBar = RatingBarType.GxRatingBar; //mGxRatingView
				setOnItemClickListener();

				//Set the width and height to PreLayoutParams
				mPreLinearLayout.setLayoutParams(new LayoutParams(15, (int)sizeStar));
			}
		}
		setVisibility();
	}

	private void setVisibility() {
		switch (mUseRatingBar) {
		case RatingBarType.AndroidRatingBar :
			mRatingBar.setVisibility(VISIBLE);
			mGxRatingView.setVisibility(GONE);
			break;
		case RatingBarType.GxRatingBar :
			mRatingBar.setVisibility(GONE);
			mGxRatingView.setVisibility(VISIBLE);
			mPreLinearLayout.setVisibility(VISIBLE);
			mPostLinearLayout.setVisibility(VISIBLE);
			break;
		}
	}

	private void setOnItemClickListener()
	{
		mGxRatingView.setOnItemClickListener(new OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position, long id)
			{
				mGxRatingView.setRating(position + 1);
			}
		});

		mPreLinearLayout.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				mGxRatingView.setRating(0);
			}
		});
	}

	@Override
	public boolean isEditable()
	{
		return isEnabled(); // Never editable.
	}
}
