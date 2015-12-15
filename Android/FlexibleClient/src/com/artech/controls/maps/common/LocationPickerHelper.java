package com.artech.controls.maps.common;

import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.artech.R;

public class LocationPickerHelper
{
	private final Activity mActivity;
	private final TextView mSelectedLocation;
	private boolean mIsSelected;
	
	public static final String EXTRA_LOCATION = "location";
	public static final String EXTRA_MAP_TYPE = "mapType";

	public LocationPickerHelper(Activity activity, boolean showButtons)
	{
		mActivity = activity;
        activity.setTitle(R.string.GXM_SelectLocation);

		mSelectedLocation = (TextView) activity.findViewById(R.id.selectedLocation);

		Button buttonOk = (Button) activity.findViewById(R.id.OkButton);
		Button buttonCancel = (Button) activity.findViewById(R.id.CancelButton);
		buttonOk.setOnClickListener(mOkClickListener);
		buttonCancel.setOnClickListener(mCancelClickListener);
		
		if (!showButtons)
		{
			buttonOk.setVisibility(View.GONE);
			buttonCancel.setVisibility(View.GONE);
			
		}
	}

	public void setPickedLocation(IMapLocation location)
	{
		mIsSelected = true;
		String locationString = String.format(Locale.US, "%.5f, %.5f", location.getLatitude(), location.getLongitude());
		mSelectedLocation.setText(locationString);
	}

	private OnClickListener mOkClickListener = new OnClickListener()
	{
		@Override
		public void onClick(View v)
		{
			selectLocation();
		}
	};

	private OnClickListener mCancelClickListener = new OnClickListener()
	{
		@Override
		public void onClick(View v)
		{
			cancelSelect();
		}
	};
	
	public void selectLocation() 
	{
		if (mIsSelected)
		{
			Intent data = new Intent();
			data.putExtra(EXTRA_LOCATION, mSelectedLocation.getText().toString()); //$NON-NLS-1$
			mActivity.setResult(Activity.RESULT_OK, data);
			mActivity.finish();
		}
	}
	
	public void cancelSelect() 
	{
		mActivity.setResult(Activity.RESULT_CANCELED);
		mActivity.finish();
	}

}
