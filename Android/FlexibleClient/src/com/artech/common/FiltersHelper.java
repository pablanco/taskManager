package com.artech.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import android.content.Context;
import android.widget.LinearLayout;

import com.artech.R;
import com.artech.base.metadata.DataItem;
import com.artech.base.metadata.DataItemHelper;
import com.artech.base.metadata.DomainDefinition;
import com.artech.base.metadata.EnumValuesDefinition;
import com.artech.base.metadata.IDataSourceDefinition;
import com.artech.base.metadata.RelationDefinition;
import com.artech.base.metadata.StructureDefinition;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.services.Services;
import com.artech.base.utils.PlatformHelper;
import com.artech.base.utils.Strings;
import com.artech.controls.GxButton;
import com.artech.controls.GxCheckBox;
import com.artech.controls.GxEnumComboSpinner;
import com.artech.controls.GxLinearLayout;
import com.artech.controls.GxRadioGroupThemeable;
import com.artech.controls.GxTextBlockTextView;

public class FiltersHelper
{
	private final static String ThemeTable = "Table"; //$NON-NLS-1$
	private final static String ThemeAttr = "Attribute"; //$NON-NLS-1$
	public final static String ThemeLabel = "TextBlock"; //$NON-NLS-1$
	private final static String ThemeButton = "Button"; //$NON-NLS-1$

	public final static String prefixFrom = "From"; //$NON-NLS-1$
	public final static String prefixTo = "To"; //$NON-NLS-1$

	public static final int SELECT_FK = 3;

	public static DataItem getFilterDataItem(IDataSourceDefinition dataSource, String filterAttName)
	{
		return DataItemHelper.find(dataSource, filterAttName, true);
	}

    public static List<CharSequence> ObtainAttributeDefinitionEnumCombo(LayoutItemDefinition formAttDef, Context context)
    {
    	List<CharSequence> enumStrings = new ArrayList<CharSequence>();
		GxEnumComboSpinner enumCombo1 = new GxEnumComboSpinner(context, null, formAttDef);
        enumCombo1.setTag(formAttDef.getDataId());

        enumStrings.add(context.getResources().getText( R.string.GX_AllItems));

		if (formAttDef.getDataItem().getBaseType().getIsEnumeration())
		{
			DomainDefinition defEnum = Services.Application.getDomain(formAttDef.getDataTypeName().GetDataType());
			if (defEnum!=null)
			{
				Vector<EnumValuesDefinition> items = defEnum.getEnumValues();
				for (EnumValuesDefinition itemDef : items) {
					enumStrings.add(itemDef.getDescription());
				}
			}
		}
    	return enumStrings;
    }

    public static List<CharSequence> ObtainAttributeDefinitionCheckBox(LayoutItemDefinition formAttDef, Context context)
    {
    	List<CharSequence> checkBoxStrings = new ArrayList<CharSequence>();
    	GxCheckBox checkBox = new GxCheckBox(context, null, formAttDef);
        checkBox.setTag(formAttDef.getDataId());
        checkBox.setHint(formAttDef.getInviteMessage());

        checkBoxStrings.add(context.getResources().getText(R.string.GX_AllItems));
        checkBoxStrings.add(context.getResources().getText(R.string.GXM_True));
        checkBoxStrings.add(context.getResources().getText(R.string.GXM_False));
    	return checkBoxStrings;
    }

    public static LayoutItemDefinition getFormAttDef(DataItem attDef, StructureDefinition structure)
	{
		LayoutItemDefinition formitem = null;
		if (attDef != null)
		{
			formitem = new LayoutItemDefinition(attDef);

			//set the caption for Hint
			formitem.setCaption(attDef.getCaption());

			if (structure == null) return formitem;

			//set if is FK
			for(int i = 0; i < structure.ManyToOneRelations.size(); i++)
			{
				RelationDefinition relation = structure.ManyToOneRelations.get(i);
				StructureDefinition def = Services.Application.getBusinessComponent(relation.getBCRelated());
				//BC could not be defined in SD WW
				if (def!=null)
				{
					int keySize = relation.getKeys().size();
					// Add the FK relation in the last attribute of the key
					for(int j = 0; j < keySize; j++)
					{
						String attRef = relation.getKeys().get(j);
						if (formitem.getDataId().equalsIgnoreCase(attRef))
						{
							formitem.setFK(relation);
						}
					}
				}
			}
		}
		return formitem;
	}

    public static String calculateAttName(String att, RelationDefinition relation)
    {
    	return PromptHelper.calculateAttName(att, relation);
	}

    public static String MakeGetFilterWithValue(Vector<String> attsAllToSend, Vector<String> values)
	{
		StringBuffer sb = new StringBuffer();
		if (attsAllToSend != null && values != null)
    	{
			for (int i = 0; i < attsAllToSend.size(); i++)
			{
				StringBuffer sbAtt = MakeGetFilterWithValue(attsAllToSend.get(i), values.get(i));
				sb = sb.append(sbAtt);
			}
			if (sb.length()>0)
				sb = sb.deleteCharAt(sb.length() - 1);
    	}
		return sb.toString();
	}

    private static StringBuffer MakeGetFilterWithValue(String attAllToSend, String value)
	{
		StringBuffer sb = new StringBuffer();
		sb.append(attAllToSend);
		sb.append(Strings.EQUAL);
		sb.append(value);
		sb.append(Strings.AND);
		return sb;
	}

    public static void setButtonAttributes(GxButton viewButtonOne, GxButton viewButtonSecond, int idButtonOne, int idButtonSecond) {
		viewButtonOne.setAttributes(idButtonOne, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
		viewButtonSecond.setAttributes(idButtonSecond, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
	}

    public static void setThemeFilters(GxLinearLayout viewTable, GxLinearLayout viewAttr, GxTextBlockTextView viewAttValue, GxTextBlockTextView viewAttLabel, GxRadioGroupThemeable radioGroupOrder, GxButton viewButtonFirst, GxButton viewButtonSecond) {

    	//Set the table Theme
		ThemeClassDefinition theme = PlatformHelper.getThemeClass(FiltersHelper.ThemeTable);
		if ((theme != null) && (viewTable != null))
			viewTable.setThemeClass(theme);

		//Set the attribute Theme
		theme = PlatformHelper.getThemeClass(FiltersHelper.ThemeAttr);
		if ((theme != null) && (viewAttr != null))
			viewAttr.setThemeClass(theme);

		//Set the attribute Theme
		theme = PlatformHelper.getThemeClass(FiltersHelper.ThemeAttr);
		if ((theme != null) && (viewAttValue != null))
			viewAttValue.setThemeClass(theme);

		//Set the attribute Label Theme
		theme = PlatformHelper.getThemeClass(FiltersHelper.ThemeLabel);
		if ((theme != null) && (viewAttLabel != null))
			viewAttLabel.setThemeClass(theme);

		//Set the attribute Label Theme
		theme = PlatformHelper.getThemeClass(FiltersHelper.ThemeLabel);
		if ((theme != null) && (radioGroupOrder != null))
			radioGroupOrder.setThemeClass(theme);

		//Set the button Theme
		theme = PlatformHelper.getThemeClass(FiltersHelper.ThemeButton);
		if ((theme != null) && (viewButtonFirst != null) && (viewButtonSecond != null)) {
			viewButtonFirst.setThemeClass(theme);
			viewButtonSecond.setThemeClass(theme);
		}
	}

}
