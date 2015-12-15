package com.artech.common;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.artech.R;
import com.artech.base.metadata.DomainDefinition;
import com.artech.base.metadata.Properties;
import com.artech.base.metadata.RelationDefinition;
import com.artech.base.metadata.enums.ControlTypes;
import com.artech.base.metadata.layout.ControlInfo;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.model.Entity;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.artech.controls.DataBoundControl;
import com.artech.controls.FKPickerControl;
import com.artech.controls.GxDateTimeEdit;
import com.artech.controls.GxEditText;
import com.artech.controls.GxEditTextMail;
import com.artech.controls.GxEditTextNumeric;
import com.artech.controls.GxEditTextPhone;
import com.artech.controls.GxEnumComboSpinner;
import com.artech.controls.GxImageViewData;
import com.artech.controls.GxLocationEdit;
import com.artech.controls.GxMediaEditControl;
import com.artech.controls.IGxComboEdit;
import com.artech.controls.IGxEdit;
import com.artech.controls.common.EditInputDescriptions;
import com.artech.ui.Coordinator;
import com.artech.usercontrols.IGxUserControl;
import com.artech.usercontrols.UcFactory;
import com.artech.utils.Cast;
import com.fedorvlasov.lazylist.ImageLoader;

public class TrnHelper
{
	private static final int rightMargin = 5;

	//create edit controls

	public static void createEditRow(Context context, Coordinator coordinator, LinearLayout parent, Entity entity, ImageLoader loader, LayoutItemDefinition att, ArrayList<IGxEdit> editables, OnClickListener listener)
	{
		if (!hideLabel(att))
		{
			TextView textView = new TextView(context);
			textView.setText(att.getCaption());
			parent.addView(textView);
		}

		//Check if is FK
		RelationDefinition relDef = att.getFK();

		//Check if user control is Dynamic Combo Box
		ControlInfo info = att.getControlInfo();
		String controlInfo = Strings.EMPTY;
		if (info!=null)
			controlInfo = info.getControl();

		if (relDef!=null && !controlInfo.equalsIgnoreCase(ControlTypes.DynamicComboBox))
		{
			LayoutInflater inflater = LayoutInflater.from(context);
			LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.fkeditor, parent, false);
			layout.setTag(att.getDataId());
			//Add listener if not read only
			layout.setOnClickListener(listener);

			LinearLayout editor = (LinearLayout) layout.findViewById(R.id.itemEditor);
			View control = addEditField(context, coordinator, loader, att, editables);
			editor.addView(control);

			LinearLayout fkButton = (LinearLayout) layout.findViewById(R.id.fkButton);
			//	Add Button For FK
			FKPickerControl btn = new FKPickerControl(context);
			btn.setTag(att.getDataId());
			//Add listener if not read only
			btn.setOnClickListener(listener);
			fkButton.addView(btn);

			parent.addView(layout);
		}
		else
			parent.addView(addEditField(context, coordinator, loader, att, editables));
	}


	public static void createEditRowRange(Context context, Coordinator coordinator, LinearLayout parent, short trnMode, Entity entity, ImageLoader loader, LayoutItemDefinition att, ArrayList<IGxEdit> editables, OnClickListener listener, String labelCaption)
	{
		TextView textView = new TextView(context);
		textView.setText(labelCaption);
		if (!hideLabel(att))
			parent.addView(textView);
		//Check if is FK
		RelationDefinition relDef = att.getFK();
		if (relDef != null)
		{
			LayoutInflater inflater = LayoutInflater.from(context);
			LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.fkeditor, parent, false);
			layout.setTag(att.getDataId());
			//Add listener if not read only
			layout.setOnClickListener(listener);
			LinearLayout editor = (LinearLayout) layout.findViewById(R.id.itemEditor);
			View control = addEditField(context, coordinator, loader, att, editables);
			editor.addView(control);
			LinearLayout fkButton = (LinearLayout) layout.findViewById(R.id.fkButton);
			//	Add Button For FK
			FKPickerControl btn = new FKPickerControl(context);
			btn.setTag(att.getDataId());
			//Add listener if not read only
			btn.setOnClickListener(listener);
			fkButton.addView(btn);
			parent.addView(layout);
		}
		else
			parent.addView(addEditField(context, coordinator, loader, att, editables));
	}

	private static boolean hideLabel(LayoutItemDefinition att)
	{
		return att.getLabelPosition().equals(Properties.LabelPositionType.None);

	}

	private static final Hashtable<String, Class<? extends IGxEdit>> mEditControls;

	static
	{
		mEditControls = new Hashtable<String, Class<? extends IGxEdit>>();
		mEditControls.put(ControlTypes.DateBox, GxDateTimeEdit.class);
		mEditControls.put(ControlTypes.LocationControl, GxLocationEdit.class);
		mEditControls.put(ControlTypes.EmailTextBox, GxEditTextMail.class);
		mEditControls.put(ControlTypes.PhoneNumericTextBox, GxEditTextPhone.class);
		mEditControls.put(ControlTypes.EnumCombo, GxEnumComboSpinner.class);
		mEditControls.put(ControlTypes.NumericTextBox, GxEditTextNumeric.class);
		mEditControls.put(ControlTypes.PhotoEditor, GxMediaEditControl.class);
		mEditControls.put(ControlTypes.VideoView, GxMediaEditControl.class);
		mEditControls.put(ControlTypes.AudioView, GxMediaEditControl.class);
	}

	public static View addEditField(Context context, Coordinator coordinator, ImageLoader loader, LayoutItemDefinition layoutItem, ArrayList<IGxEdit> editables)
	{
		View item =	getEditUserControl(context, coordinator, layoutItem, editables);
		if (item != null)
			return item;

		String dataId = layoutItem.getDataId();
		IGxEdit fieldControl = null;

		// For input type descriptions, use GxEditText regardless of custom editor.
		if (!EditInputDescriptions.isInputTypeDescriptions(layoutItem))
		{
			String controlType = layoutItem.getControlType();
			if (mEditControls.containsKey(controlType))
				fieldControl = createControlEdit(context, coordinator, layoutItem, controlType);
		}

		if (fieldControl == null)
			fieldControl = new GxEditText(context, coordinator, layoutItem);

		item = (View)fieldControl;
		item.setTag(dataId);
		setLayoutParams(item);

		editables.add(fieldControl);
		return item;
	}

	private static IGxEdit createControlEdit(Context context, Coordinator coordinator, LayoutItemDefinition layoutItemDefinition, String controlType)
	{
		Class<?> controlClass = mEditControls.get(controlType);
		try
		{
			return (IGxEdit) UcFactory.getUcConstructor(controlClass, controlType, context, coordinator, layoutItemDefinition);
		}
		catch (Exception e)
		{
			Services.Log.Error("Error calling constructor of " + controlType); //$NON-NLS-1$
			return null;
		}
	}

	@SuppressLint("InlinedApi")
	private static void setLayoutParams(View edit)
	{
		LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1);
		lp.setMargins(1, 1, rightMargin, 1);
		edit.setLayoutParams(lp);
	}

	private static View getEditUserControl(Context context, Coordinator coordinator, LayoutItemDefinition att, ArrayList<IGxEdit> editables)
	{
		IGxEdit item =	getEditUserControl(context, coordinator, att);
		if (item != null)
		{
			editables.add(item);
			return (View) item;
		}
		return null;
	}

	private static IGxEdit getEditUserControl(Context context, Coordinator coordinator, LayoutItemDefinition layoutItem)
	{
		if (layoutItem != null)
		{
			IGxEdit edit = getUserControlFromLayoutItem(context, coordinator, layoutItem);

			if (edit != null)
			{
				try
				{
					edit.setGx_Tag(layoutItem.getDataId());
					return edit;
				}
				catch (Exception e)
				{
					Services.Log.Error("Creating User Control", layoutItem.getControlInfo().getControl()); //$NON-NLS-1$
					Services.Exceptions.printStackTrace(e);
				}
			}
		}

		return null;
	}

	public static IGxEdit getUserControlFromLayoutItem(Context context, Coordinator coordinator, LayoutItemDefinition layoutItem)
	{
		if (layoutItem.getControlInfo() != null)
		{
			IGxUserControl edit = UcFactory.createUserControl(context, coordinator, layoutItem);
			return Cast.as(IGxEdit.class, edit);
		}

		return null;
	}

	//Enums Handler

	public static void setEnumCombosData(List<IGxEdit> items)
	{
		for (IGxEdit item : items)
		{
			if (item instanceof IGxComboEdit)
				setEnumComboData((IGxComboEdit)item);
		}
	}

	private static void setEnumComboData(IGxComboEdit combo)
	{
		LayoutItemDefinition def = combo.getDefinition();
		if (def != null)
		{
			if (def.getDataItem().getBaseType().getIsEnumeration())
			{
				DomainDefinition defEnum = Services.Application.getDomain(def.getDataTypeName().GetDataType());
				if (defEnum != null)
					combo.setComboValues(defEnum.getEnumValues());
			}
		}
	}
	public static GxImageViewData getGxImage(View v)
	{
		if(v instanceof GxImageViewData)
			return (GxImageViewData)v;
		if (v instanceof DataBoundControl)
		{
			DataBoundControl boundControl = (DataBoundControl)v;
			if (boundControl.getEdit()!=null && boundControl.getEdit() instanceof GxImageViewData)
				return (GxImageViewData) boundControl.getEdit();
		}
		return null;
	}
}
