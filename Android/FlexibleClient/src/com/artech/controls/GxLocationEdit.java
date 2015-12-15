package com.artech.controls;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.support.annotation.IdRes;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.artech.R;
import com.artech.activities.ActivityLauncher;
import com.artech.activities.GxBaseActivity;
import com.artech.android.api.LocationHelper;
import com.artech.base.controls.IGxControlActivityLauncher;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.artech.controls.maps.common.LocationPickerHelper;
import com.artech.controls.maps.googlev2.GooglePlacePickerHelper;
import com.artech.ui.Coordinator;

public class GxLocationEdit extends RelativeLayout implements IGxEdit, IGxControlActivityLauncher
{
    @IdRes
    private static final int VIEW_ID = 75;  //just a valid integer
    private final GxEditText mEditText;
	private final Button mSelectButton;
	private final Activity mContext;
	private final IGxEdit mEdit;
	private final LayoutItemDefinition mDefinition;
	private final String mMapType;

	private boolean mShowMap = false;
	private GxSDGeoLocation mGeoLocationEdit;
	private Coordinator mCoordinator;

	private final static int PLACE_PICKER_REQUEST_CODE = 151;

	public GxLocationEdit(Context context, Coordinator coordinator, LayoutItemDefinition layoutItemDef)
	{
		super(context);
		mDefinition = layoutItemDef;
		mEdit = this;
		mContext = (Activity) context;
		mCoordinator = coordinator;
		mMapType = layoutItemDef.getControlInfo().optStringProperty("@SDGeoLocationMapType");

		setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

		mEditText = new GxEditText(context, coordinator, layoutItemDef);
		mSelectButton = new AppCompatButton(context);
		mSelectButton.setText(R.string.GX_BtnSelect);

		addView(mEditText, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		addView(mSelectButton, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));

		updateMapControl();

		mSelectButton.setOnClickListener(mOnClickListener);
	}

	private final OnClickListener mOnClickListener = new OnClickListener()
	{
		@Override
		public void onClick(View v)
		{
			if (mShowMap)
			{
				callBestLocationPicker();
			}
			else
			{
				AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
				String[] items = { Services.Strings.getResource(R.string.GXM_MyLocation), Services.Strings.getResource(R.string.GXM_OtherLocation) };

				builder.setTitle(R.string.GXM_SelectLocation);
				builder.setItems(items, ondialogclick);
				AlertDialog alert = builder.create();
				alert.show();
			}
		}

		private DialogInterface.OnClickListener ondialogclick = new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int option)
			{
				dialog.dismiss();

				if (option == 0)
				{
					Location location = LocationHelper.getLastKnownLocation();
					if (location != null)
						setEditValue(String.valueOf(location.getLatitude()) + Strings.COMMA + String.valueOf(location.getLongitude()));
					else
						Toast.makeText(getContext(), R.string.GXM_CouldNotGetLocationInformation, Toast.LENGTH_SHORT).show();
				}
				else if (option == 1)
				{
					callBestLocationPicker();
				}
				else
					throw new IllegalArgumentException("Option should only be 0 or 1");
			}
		};
	};

	private void callBestLocationPicker()
	{
		if (GooglePlacePickerHelper.isAvailable(getContext()))
			callGooglePlacePicker();
		else
			callBasicGeolocationPicker();
	}

	private void callGooglePlacePicker()
	{
		Intent placePickerIntent = GooglePlacePickerHelper.buildIntent(mContext, getGx_Value());
		if (placePickerIntent != null)
		{
			if (mCoordinator.getUIContext().getActivityController() != null)
				mCoordinator.getUIContext().getActivityController().setCurrentActivityLauncher(this);

			mContext.startActivityForResult(placePickerIntent, PLACE_PICKER_REQUEST_CODE);
		}
	}

	@Override
	public boolean handleOnActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (requestCode == PLACE_PICKER_REQUEST_CODE)
		{
			String value = GooglePlacePickerHelper.getLocationValueFromResult(getContext(), resultCode, data);
			if (Strings.hasValue(value))
				setEditValue(value);

			return true; // Handled by us, even if unsuccessfully.
		}
		else
			return false;
	}

	private void callBasicGeolocationPicker()
	{
		GxBaseActivity.PickingElementId = mEdit.getGx_Tag(); //hack because onActivityResult should be handled on activities
		ActivityLauncher.CallLocationPicker(mContext, mMapType, getGx_Value());
	}

	@Override
	public void setValueFromIntent(Intent data)
	{
		String contents = data.getStringExtra(LocationPickerHelper.EXTRA_LOCATION);
		setEditValue(contents);
	}

	private void updateMapControl()
	{
		if (mShowMap)
		{
			removeView(mEditText);
			removeView(mSelectButton);

			// use a relative layout for better layout with map.
			mGeoLocationEdit = new GxSDGeoLocation(getContext(), mCoordinator, mDefinition);
			mGeoLocationEdit.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
			mGeoLocationEdit.setOnClickListener(mOnClickListener);

			addView(mGeoLocationEdit);
			addView(mSelectButton);

			RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			// align select top right inside the map
			params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
			mSelectButton.setLayoutParams(params);

		}
		else
		{
			RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			// align right the button
			params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
			mSelectButton.setLayoutParams(params);
			mSelectButton.setId(VIEW_ID);

			RelativeLayout.LayoutParams params2 = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
			// edit text fill remain space
			params2.addRule(RelativeLayout.LEFT_OF, VIEW_ID);
			mEditText.setLayoutParams(params2);

			setHint(mDefinition.getInviteMessage());
		}
	}


	@Override
	public String getGx_Value() {
		return (mGeoLocationEdit != null) ? mGeoLocationEdit.getGx_Value() : mEditText.getGx_Value();
	}

	@Override
	public void setGx_Value(String value) {
		if (mGeoLocationEdit != null) {
			mGeoLocationEdit.setGx_Value(value);
		} else {
			mEditText.setGx_Value(value);
		}
	}

	private void setEditValue(String value) {
		String previousValue;
		String currentValue;

		if (mGeoLocationEdit != null) {
			previousValue = mGeoLocationEdit.getGx_Value();
			mGeoLocationEdit.setGx_Value(value);
			currentValue = mGeoLocationEdit.getGx_Value();
		} else {
			previousValue = mEditText.getGx_Value();
			mEditText.setGx_Value(value);
			mEditText.setTextAsEdited(value);
			currentValue = mEditText.getGx_Value();
		}

		boolean valueChanged = !TextUtils.equals(currentValue, previousValue);

		if (mCoordinator != null && valueChanged) {
			mCoordinator.onValueChanged(this, true);
		}
	}

	@Override
	public String getGx_Tag() {
		return (String) this.getTag();
	}

	@Override
	public void setGx_Tag(String data) {
		this.setTag(data);
	}

	private void setHint(String label)
	{
		if (!mShowMap)
			mEditText.setHint(label);
	}

	@Override
	public IGxEdit getViewControl() {
		return new GxTextView(getContext(), mDefinition);
	}

	@Override
	public IGxEdit getEditControl() {
		return this;
	}

	public void setShowMap(boolean b)
	{
		mShowMap = b;
		updateMapControl();
	}

	@Override
	public boolean isEditable()
	{
		return isEnabled(); // Editable when enabled.
	}
}
