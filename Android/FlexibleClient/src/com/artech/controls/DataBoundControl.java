package com.artech.controls;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.artech.android.layout.GxTheme;
import com.artech.android.layout.LayoutTag;
import com.artech.base.controls.IGxControlRuntime;
import com.artech.base.controls.IGxControlRuntimeContext;
import com.artech.base.metadata.Properties;
import com.artech.base.metadata.enums.Alignment;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.model.Entity;
import com.artech.common.ExecutionContext;
import com.artech.controls.utils.TextViewUtils;
import com.artech.extendedcontrols.gauge.RangeControl;
import com.artech.extendedcontrols.image.GxAdvancedImage;
import com.artech.ui.Coordinator;
import com.artech.utils.BackgroundOptions;
import com.artech.utils.Cast;
import com.artech.utils.ThemeUtils;

public class DataBoundControl extends GxLinearLayout implements IGxEdit, IGxEditControl, IGxEditWithDependencies, IGxControlRuntime, IGxControlRuntimeContext, IGxLocalizable
{
	private final GxTextView mLabel;
	private IGxEdit mEdit;

	private Coordinator mCoordinator;
	private LayoutItemDefinition mFormItemDefinition;
	private String mLabelPosition;

	private Entity mEntity; // Optional, only for "in grid" domain actions.

	public DataBoundControl(Context context, Coordinator coordinator, LayoutItemDefinition def)
	{
		super(context);
		mCoordinator = coordinator;

		mLabel = new GxTextView(context);
		mFormItemDefinition = def;
		setCaption(def.getCaption());
		setLabelPosition(def.getLabelPosition());

		// matiash: For testing, not enabled yet.
		// setFocusableInTouchMode(true);
	}

	public void setCaption(String caption) {
		TextViewUtils.setText(mLabel, caption, mFormItemDefinition);
	}

	public DataBoundControl(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		mLabel = new GxTextView(context, (LayoutItemDefinition) null);
	}

	public void addEdit(View view)
	{
		mEdit = (IGxEdit) view;
		addEditInView(this);
	}

	private void addEditInView(GxLinearLayout layoutData)
	{
		if (mLabelPosition.equals(Properties.LabelPositionType.Right)
				|| 	mLabelPosition.equals(Properties.LabelPositionType.Bottom)) {
			layoutData.addView((View)mEdit, 0);
		}
		else {
			layoutData.addView((View)mEdit);
		}
		if (mLabelPosition.equals(Properties.LabelPositionType.None))
		{
			//GxWebView need to be wrapcontext to autogrow works
			if (mEdit!=null &&
				( !(mEdit instanceof GxWebView)
						|| !mFormItemDefinition.hasAutoGrow() ) )
			{
				//set fill parent to unique data field.
				LayoutParams parameters = (LayoutParams)((View)mEdit).getLayoutParams();
				parameters.height = LayoutParams.MATCH_PARENT;
				parameters.width = LayoutParams.MATCH_PARENT;
				((View)mEdit).setLayoutParams(parameters);
			}
		}

	}

	public void addDomainActionImage(ImageView view)
	{
		addActionImageToRight(view);
	}

	private void addActionImageToRight(ImageView view)
	{
		// Image is centered vertically and takes space horizontally as needed.
		LayoutParams lpImage = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);

		if (mLabelPosition.equals(Properties.LabelPositionType.Top) || mLabelPosition.equals(Properties.LabelPositionType.Bottom))
		{
			//Only the last one image added is getting draw

			/*
			 TODO should be done different, not work in some cases.
			for (int i=0;  i< this.getChildCount(); i++)
			{
				View viewChild = this.getChildAt(i);
				if (viewChild instanceof ViewGroup)
				{
					ViewGroup viewGroupChild = (ViewGroup)viewChild;
					viewGroupChild.removeAllViews();
				}
			}
			*/
			//remove all view in childs
			removeAllViews();

			//add in a new layout.
			//Data
			GxLinearLayout layoutData = new GxLinearLayout(getContext());
			layoutData.setOrientation(LinearLayout.VERTICAL);
			layoutData.addView(mLabel);
			addEditInView(layoutData);

			// Add the data part and the image to main view
			setOrientation(LinearLayout.HORIZONTAL);
			addView(layoutData);
			addView(view, lpImage);

			//set gravity to lbl/att field
			LayoutParams parameters = (LayoutParams) layoutData.getLayoutParams();
			parameters.weight = 1;
			layoutData.setLayoutParams(parameters);
		}
		else
		{
			// Just add the image.
			addView(view, lpImage);
		}
	}

	public void addFKActionImage(ImageView view) {
		addActionImageToRight(view);
	}

	public Coordinator getCoordinator() { return mCoordinator; }
	public LayoutItemDefinition getFormItemDefinition() { return mFormItemDefinition; }

	@Override
	public String getGx_Value() {
		return mEdit.getGx_Value();
	}

	@Override
	public void setGx_Value(String value) {
		mEdit.setGx_Value(value);

	}

	@Override
	public String getGx_Tag() {
		return mEdit.getGx_Tag();
	}

	@Override
	public void setGx_Tag(String tag) {
		mEdit.setGx_Tag(tag);
	}

	@Override
	public void setEnabled(boolean enabled)
	{
		super.setEnabled(enabled);
		if (mEdit != null)
			mEdit.setEnabled(enabled);
	}

	@Override
	public void setValueFromIntent(Intent data) 
	{
		if (mEdit != null) 
		{
			mEdit.setValueFromIntent(data);
		}
	}

	public void setLabelText(String text) {
		TextViewUtils.setText(mLabel, text, mFormItemDefinition);
	}

	public GxTextView getLabel() {
		return mLabel;
	}

	public IGxEdit getEdit() {
		return mEdit;
	}

	public void setLabelPosition(String labelPosition) {
		mLabelPosition = labelPosition;

		if (labelPosition.equals(Properties.LabelPositionType.Top) || labelPosition.equals(Properties.LabelPositionType.Bottom)) {
			setOrientation(LinearLayout.VERTICAL);
	        addView(mLabel);
		}
		if (labelPosition.equals(Properties.LabelPositionType.Left) || labelPosition.equals(Properties.LabelPositionType.Right)  ) {
			setOrientation(LinearLayout.HORIZONTAL);
			mLabel.setPadding(0, 0, 4, 0);
	        addView(mLabel);
		}
		if (labelPosition.equals(Properties.LabelPositionType.None)){
			setOrientation(LinearLayout.HORIZONTAL);

		}

	}

	public String getLabelPosition()
	{
		return mLabelPosition;
	}

	public boolean hasLabelPositionNone()
	{
		return (mLabelPosition.equals(Properties.LabelPositionType.None));
	}

	@Override
	public IGxEdit getViewControl() {
		return this;
	}

	@Override
	public IGxEdit getEditControl() {
		return this;
	}


	private void setLabelAndFieldLayoutThemeProperties(ThemeClassDefinition themeClass)
	{
		if (mFormItemDefinition == null)
			return;

		if (themeClass == null)
			themeClass = mFormItemDefinition.getThemeClass();

		GxTextView labelTextView = getLabel();
		IGxEdit attView = getEdit();
		boolean setLabelAlignment = false;
		if (themeClass != null)
		{
			// Apply Theme classes for the DataBoundControl the container of the Label + Attribute
			super.setThemeClass(themeClass);

			// Apply Theme for the Label
			if (labelTextView != null && themeClass.getLabelClass()!=null)
				GxTheme.applyStyle(labelTextView, themeClass.getLabelClass());

			// Apply Alignment for the Label
			if (labelTextView != null)
			{
				//set gravity of label
				labelTextView.setGravity(themeClass.getHorizontalLabelAlignment() | themeClass.getVerticalLabelAlignment());
				LayoutParams params = (LayoutParams)labelTextView.getLayoutParams();
				if (params!=null)
				{
					params.gravity = themeClass.getHorizontalLabelAlignment() | themeClass.getVerticalLabelAlignment();
					labelTextView.setLayoutParams(params);
				}

				if (labelTextView.getGravity() != android.view.Gravity.NO_GRAVITY)
					setLabelAlignment = true;
				if (themeClass.getLabelWidth()!=null)
				{
					ViewGroup.LayoutParams p = labelTextView.getLayoutParams();
					p.width = themeClass.getLabelWidth();
					labelTextView.setLayoutParams(p);
				}
			}

			// Apply properties for the attribute.
			// TODO: This should be done via an interface or something.
			if (attView instanceof TextView)
			{
				ThemeUtils.setFontProperties((TextView)attView, themeClass);
			}
			else if (attView instanceof GxImageViewData)
			{
				((GxImageViewData)attView).setThemeClass(themeClass);
				((GxImageViewData)attView).setAlignment(mFormItemDefinition.CellGravity);
			}
			else if (attView instanceof SpinnerControl || attView instanceof DynamicSpinnerControl || attView instanceof RadioGroupControl
					|| attView instanceof SeekBarControl || attView instanceof GxAdvancedImage || attView instanceof GxWebView
					|| attView instanceof ScannerControl || attView instanceof RangeControl)
			{
				// TODO: Should this be done to all IGxThemeable controls?
				// this only be done for controls that use the class but not use border, background like databound control do
				// a class in theme for the attView control itself (only) is needed.
				((IGxThemeable)attView).setThemeClass(themeClass);
			}
		}

		if (setLabelAlignment && attView!=null && !mLabelPosition.equals(Properties.LabelPositionType.None) )
		{
			//set gravity to att field
			LayoutParams parameters = (LayoutParams)((View)attView).getLayoutParams();
			parameters.gravity = mFormItemDefinition.CellGravity;
			parameters.weight = 1;
			((View)attView).setLayoutParams(parameters);
			if (attView instanceof TextView && //or EditText
					mFormItemDefinition.CellGravity!=Alignment.CENTER_VERTICAL &&
					mFormItemDefinition.CellGravity!=Alignment.NONE  )
			{
				((TextView)attView).setGravity(mFormItemDefinition.CellGravity);
			}
		}
		else
		{
			/*
			//set gravity to att field
			LayoutParams parameters = (LayoutParams)((View)attView).getLayoutParams();
			parameters.gravity = mFormItemDefinition.CellGravity;
			parameters.weight = 1;
			((View)attView).setLayoutParams(parameters);
			*/
			setGravity(mFormItemDefinition.CellGravity);
			if (attView instanceof TextView && //or EditText
					mFormItemDefinition.CellGravity!=Alignment.CENTER_VERTICAL &&
					mFormItemDefinition.CellGravity!=Alignment.NONE )
			{
				((TextView)attView).setGravity(mFormItemDefinition.CellGravity);
			}
		}

		if (attView != null && attView instanceof View)
			((View)attView).setTag(LayoutTag.CONTROL_THEME_CLASS, themeClass);
	}

	@Override
	public void setThemeClass(ThemeClassDefinition themeClass) {
		setLabelAndFieldLayoutThemeProperties(themeClass);
	}

	public void setLabelAndFieldLayoutNoTheme()
	{
		ThemeClassDefinition themeClass = null;
		if (mFormItemDefinition != null)
			themeClass = mFormItemDefinition.getThemeClass();

		if (themeClass==null && mFormItemDefinition != null)
			setGravity(mFormItemDefinition.CellGravity);

	}

	@Override
	protected void setBackgroundBorderProperties(ThemeClassDefinition themeClass)
	{
		// Override to pass layout item definition (GxLinearLayout does not always correspond to a layout item).
		ThemeUtils.setBackgroundBorderProperties(this, themeClass, BackgroundOptions.defaultFor(mFormItemDefinition));
	}

	// Optional, only for "in grid" domain actions.
	public Entity getEntity()
	{
		return mEntity;
	}

	public void setEntity(Entity entity)
	{
		mEntity = entity;
	}

	@Override
	public void setValue(Object value)
	{
		if (value != null)
		{
			IGxEditControl editNew = Cast.as(IGxEditControl.class, mEdit);
			if (editNew != null)
				editNew.setValue(value);
			else
				mEdit.setGx_Value(value.toString());
		}
	}

	@Override
	public Object getValue() {
		return getGx_Value();
	}

	@Override
	public List<String> getDependencies()
	{
		if (mEdit instanceof IGxEditWithDependencies)
			return ((IGxEditWithDependencies)mEdit).getDependencies();
		else
			return new ArrayList<String>();
	}

	@Override
	public void onDependencyValueChanged(String name, Object value)
	{
		if (mEdit instanceof IGxEditWithDependencies)
			((IGxEditWithDependencies)mEdit).onDependencyValueChanged(name, value);
	}

	@Override
	public void setExecutionContext(ExecutionContext context)
	{
		if (mEdit instanceof IGxControlRuntimeContext)
			((IGxControlRuntimeContext)mEdit).setExecutionContext(context);
	}

	@Override
	public Object getProperty(String name)
	{
		// Retrieve properties from custom control if it supports them.
		if (mEdit instanceof IGxControlRuntime)
			return ((IGxControlRuntime)mEdit).getProperty(name);
		else
			return null;
	}

	@Override
	public void setProperty(String name, Object value)
	{
		if (mEdit instanceof IGxControlRuntime)
			((IGxControlRuntime)mEdit).setProperty(name, value);
	}

	@Override
	public void runMethod(String name, List<Object> parameters)
	{
		if (mEdit instanceof IGxControlRuntime)
			((IGxControlRuntime)mEdit).runMethod(name, parameters);
	}

	@Override
	public boolean isEditable()
	{
		return (mEdit != null && mEdit.isEditable());
	}

	@Override
	public void onTranslationChanged() {
		TextViewUtils.setText(mLabel, mFormItemDefinition.getCaption(), mFormItemDefinition);
	}
}
