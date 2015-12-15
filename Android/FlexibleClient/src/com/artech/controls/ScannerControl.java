package com.artech.controls;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.artech.activities.ActivityLauncher;
import com.artech.activities.GxBaseActivity;
import com.artech.base.metadata.enums.RequestCodes;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.ui.Coordinator;
import com.artech.utils.ThemeUtils;

public class ScannerControl extends LinearLayout implements IGxEdit, IGxThemeable 
{
	private Button mAction;
	private TextView mEdit;
	private IGxEdit mControl;
	private Coordinator mCoordinator;
	private LayoutItemDefinition mDefinition;

	private ThemeClassDefinition mThemeClass;

	public ScannerControl(Context context, Coordinator coordinator, LayoutItemDefinition def) {
		super(context);
		mControl = this;
		mCoordinator = coordinator;
		mDefinition = def;

		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    if(inflater != null) {
	       inflater.inflate(com.artech.R.layout.scannercontrol, this, true);
	    }

		LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1);
		lp.setMargins(1, 1, 5 ,1);
		setLayoutParams(lp);

		mAction = (Button) findViewById(com.artech.R.id.scannerButton);
		mEdit = (TextView) findViewById(com.artech.R.id.scannerEdit);

		LayoutParams lpEdit = (LayoutParams) mEdit.getLayoutParams();
		if (lpEdit!=null) {
			lpEdit.weight = 1;
		}

		mAction.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// GxActivity activity = (GxActivity) getContext();
				GxBaseActivity.PickingElementId = mControl.getGx_Tag();
				Intent intent = new Intent("com.google.zxing.client.android.SCAN"); //$NON-NLS-1$
				ActivityLauncher.setIntentFlagsNewDocument(intent);

				try {
					((Activity)getContext()).startActivityForResult(intent, RequestCodes.PICKER);
	     	    }
				catch (Exception ex)
				{
					ex.printStackTrace();
					// should use ScannerAPI.callDownloadScanner to check if scanner is installed?.
				}
			}
		});
	}

	public ScannerControl(Context context, AttributeSet attrs) {
		super(context);
		mControl = this;
	}

	@Override
	public String getGx_Value() {
		return  mEdit.getText().toString();
	}

	@Override
	public void setGx_Value(String value) {
		CharSequence previousValue = mEdit.getText();

		mEdit.setText(value);

		boolean valueChanged = !TextUtils.equals(previousValue, value);

		if (mCoordinator != null && valueChanged) {
			mCoordinator.onValueChanged(ScannerControl.this, true);
		}
	}

	@Override
	public String getGx_Tag() {
		return (String) mEdit.getTag();
	}

	@Override
	public void setGx_Tag(String tag) {
		mEdit.setTag(tag);
	}

	@Override
	public void setValueFromIntent(Intent data) {
		String contents = data.getStringExtra("SCAN_RESULT"); //$NON-NLS-1$
		//String format = data.getStringExtra("SCAN_RESULT_FORMAT");
		setGx_Value(contents);
	}

	@Override
	public void setEnabled(boolean enabled) {
		mEdit.setEnabled(enabled);
		mAction.setEnabled(enabled);
		super.setEnabled(enabled);
	}

	@Override
	public IGxEdit getViewControl() {
		return new GxTextView(getContext(), mDefinition);
	}

	@Override
	public IGxEdit getEditControl() {
		return this;
	}

	@Override
	public boolean isEditable()
	{
		return isEnabled(); // Editable when enabled.
	}
	
	@Override
	public void setThemeClass(ThemeClassDefinition themeClass)
	{
		mThemeClass = themeClass;
		applyClass(themeClass);
	}

	@Override
	public ThemeClassDefinition getThemeClass()
	{
		return mThemeClass;
	}

	@Override
	public void applyClass(ThemeClassDefinition themeClass)
	{
		// Only apply font properties to edit control. Background, border, etc are already set by DataBoundControl.
		if (mEdit != null)
		{
			ThemeUtils.setFontProperties(mEdit, themeClass);
		}
	}
	
}
