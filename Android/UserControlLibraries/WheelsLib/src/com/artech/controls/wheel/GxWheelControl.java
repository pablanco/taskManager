package com.artech.controls.wheel;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.artech.base.metadata.layout.ControlInfo;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.utils.Strings;
import com.artech.controls.GxTextView;
import com.artech.controls.IGxEdit;
import com.artech.ui.Coordinator;

import antistatic.spinnerwheel.AbstractWheel;
import antistatic.spinnerwheel.OnWheelScrollListener;

public class GxWheelControl extends LinearLayout implements IGxEdit
{
	private static enum GxWheelType {
		Numeric,
		Enum
	}

	private LayoutItemDefinition mDefinition = null;
	private Coordinator mCoordinator = null;
	private String mTag = null;

	private IGxWheelControl mWheelControlDefinition;
	private GxWheelType mWheelControlDefinitionType;

	private GxWheelPicker mWheelControlNumeric = null;
	private GxWheelPicker mWheelControlDecimal = null;

	// Show wheel in-line or as a picker dialog.
	private boolean mShowInline = false;

	// Wheel properties
	private String mCurrentValue = null;
	private boolean mIsCyclic = false;
	private boolean mOnlyNumericWheel = false;

	// Wheel dialog
	private AlertDialog mWheelControlDialog = null;
	private Button mAction = null;
	private TextView mText = null;

	public GxWheelControl(Context context, Coordinator coordinator, LayoutItemDefinition layoutItemDefinition) {
		super(context);
		mCoordinator = coordinator;
		mDefinition = layoutItemDefinition;

		if (layoutItemDefinition.getControlInfo() != null) {
			setControlInfo(layoutItemDefinition.getControlInfo());
		}

		mWheelControlNumeric = new GxWheelPicker(getContext());
		mWheelControlDecimal = new GxWheelPicker(getContext());
		mWheelControlNumeric.setCyclic(mIsCyclic);
		mWheelControlDecimal.setCyclic(mIsCyclic);
		mWheelControlDefinition.setViewAdapter(mCurrentValue, mWheelControlNumeric, mWheelControlDecimal);

		if (mShowInline) {
			setupWheelsContainer(this);
			mWheelControlNumeric.addScrollingListener(onWheelScrollListener);
			if (!mOnlyNumericWheel) {
				mWheelControlDecimal.addScrollingListener(onWheelScrollListener);
			}
		} else {
			mText = new TextView(getContext());
			mText.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
			mText.setGravity(Gravity.CENTER);
			mText.setText(mCurrentValue);
			addView(mText);
			mAction = new AppCompatButton(getContext());
			mAction.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
			mAction.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (mWheelControlDialog == null) {
						mWheelControlDialog = createDialog();
					}
					mWheelControlDialog.show();
				}
			});
			mAction.setText(mCurrentValue);
			addView(mAction);
		}

		setEnabled(true); // Complete setup (see setEnabled() implementation).
	}

    private OnWheelScrollListener onWheelScrollListener = new OnWheelScrollListener() {

		@Override
		public void onScrollingStarted(AbstractWheel wheel) {
			// Nothing to do.
		}

		@Override
		public void onScrollingFinished(AbstractWheel wheel) {
			onWheelValueChanged();
		}

    };

    private void onWheelValueChanged() {
    	String previousValue = mCurrentValue;
    	mCurrentValue = mWheelControlDefinition.getCurrentStringValue(mWheelControlNumeric, mWheelControlDecimal);
    	mWheelControlDefinition.setViewAdapter(mCurrentValue, mWheelControlNumeric, mWheelControlDecimal);
    	if (mCoordinator != null && !mCurrentValue.equals(previousValue)) {
    		mCoordinator.onValueChanged(GxWheelControl.this, true);
    	}
    }

    private void setupWheelsContainer(LinearLayout linearLayout) {
    	linearLayout.setOrientation(LinearLayout.HORIZONTAL);
    	linearLayout.setGravity(Gravity.CENTER);
    	linearLayout.addView(mWheelControlNumeric, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1));
		if (!mOnlyNumericWheel) {
			linearLayout.addView(mWheelControlDecimal, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1));
		}
    }

	private AlertDialog createDialog() {
		LinearLayout dialogContent = new LinearLayout(getContext());
		setupWheelsContainer(dialogContent);

		AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
		builder.setView(dialogContent)
			// Action buttons
			.setPositiveButton(R.string.GXM_button_ok, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					onWheelValueChanged();
					mAction.setText(mCurrentValue);
					mText.setText(mCurrentValue);
				}
			})
			.setNegativeButton(R.string.GXM_cancel, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					mWheelControlDefinition.setViewAdapter(mCurrentValue, mWheelControlNumeric, mWheelControlDecimal);
					dialog.cancel();
				}
			});

		return builder.create();
	}

	private void setControlInfo(ControlInfo info) {
		mIsCyclic = info.optBooleanProperty("@SDWheelCyclic"); //$NON-NLS-1$
		mShowInline = info.optStringProperty("@SDWheelDisplayStyle").equalsIgnoreCase("inline"); //$NON-NLS-1$ $NON-NLS-2$
		String controlValues = info.optStringProperty("@ControlValues"); //$NON-NLS-1$

		// If the control is numeric, controlValues comes empty. Otherwise, it contains the enum values.
		if (controlValues.length() == 0) {
			mWheelControlDefinition = new GxWheelNumericControl(info);
			mWheelControlDefinitionType = GxWheelType.Numeric;
			mOnlyNumericWheel = ((GxWheelNumericControl) mWheelControlDefinition).isOnlyWheelControlNumeric();
		} else {
			mWheelControlDefinition = new GxWheelEnumControl(info);
			mWheelControlDefinitionType = GxWheelType.Enum;
			mOnlyNumericWheel = true;
		}

		mCurrentValue = mWheelControlDefinition.getDisplayInitialValue();
	}

	@Override
	public String getGx_Value() {
		String res = Strings.EMPTY;

		switch (mWheelControlDefinitionType) {
			case Numeric:
				res = mCurrentValue;
				break;
			case Enum:
				res = ((GxWheelEnumControl) mWheelControlDefinition).getGx_Value(mCurrentValue);
				break;
		}

		return res;
	}

	@Override
	public void setGx_Value(String value) {
		mCurrentValue = mWheelControlDefinition.getGx_DisplayValue(value);
		mWheelControlDefinition.setViewAdapter(mCurrentValue, mWheelControlNumeric, mWheelControlDecimal);
		if (!mShowInline) {
			mAction.setText(mCurrentValue);
			mText.setText(mCurrentValue);
		}
	}

	@Override
	public String getGx_Tag() {
		return mTag;
	}

	@Override
	public void setGx_Tag(String tag) {
		mTag = tag;
	}

	@Override
	public void setValueFromIntent(Intent data) {
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		if (mShowInline) {
			mWheelControlNumeric.setEnabled(enabled);
			mWheelControlDecimal.setEnabled(enabled);
		} else {
			mText.setVisibility(enabled ? View.GONE : View.VISIBLE);
			mAction.setVisibility(enabled ? View.VISIBLE : View.GONE);
		}
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
}
