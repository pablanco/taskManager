package com.artech.common;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;

import com.artech.android.json.NodeObject;
import com.artech.android.layout.SectionsLayoutVisitor;
import com.artech.android.layout.SectionsLayoutVisitor.LayoutSection;
import com.artech.application.MyApplication;
import com.artech.base.metadata.DetailDefinition;
import com.artech.base.metadata.enums.DisplayModes;
import com.artech.base.metadata.enums.LayoutItemsTypes;
import com.artech.base.metadata.layout.ContentDefinition;
import com.artech.base.metadata.layout.LayoutDefinition;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.metadata.layout.LayoutItemDefinitionFactory;
import com.artech.base.metadata.layout.TabControlDefinition;
import com.artech.base.metadata.layout.TabItemDefinition;
import com.artech.base.metadata.layout.TableDefinition;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;


public class LayoutHelper
{
	//Ads
	public static int AdsSizeDpi = 52;//52;

	public static AdView getAdView(Activity activity)
	{
		String publisherId = MyApplication.getApp().getAdMobPublisherId();
		AdSize adsSize = AdSize.BANNER;
		AdView adView = new AdView(activity);
		adView.setAdSize(adsSize);
		adView.setAdUnitId(publisherId);

		return adView;
	}

	//Create dynamic layout helpers.

	//create row with one cell
	public static LayoutItemDefinition getRowWithCell(LayoutDefinition layout, LayoutItemDefinition parent, String cellContent, String rowHeight,
			String cellX, String cellY, String cellXRelative, String cellYRelative,
			String cellWidth, String cellHeight, String cellWidthRelative, String cellHeightRelative)
	{
		String attName = "row"; //$NON-NLS-1$

		// \"@hAlign\": \"Center\", \"@vAlign\": \"Middle\", "+ //$NON-NLS-1$

		String jsonRow = "{ \"@rowHeight\": \"" + rowHeight + "\", "+ //$NON-NLS-1$ //$NON-NLS-2$
	    "\"cell\": { \"@rowSpan\": \"1\", \"@colSpan\": \"1\", \"@hAlign\": \"Default\", \"@vAlign\": \"Default\", "+ //$NON-NLS-1$
	    cellContent + ", "+ //$NON-NLS-1$
        " \"CellBounds\": { \"@x\": \"" + cellX + "\", \"@y\": \"" + cellY +"\", " +  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        "\"@xRelative\": \"" + cellXRelative + "\", \"@yRelative\": \"" + cellYRelative + "\", " +  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        "\"@width\": \""+ cellWidth + "\", " + " \"@height\": \"" + cellHeight +  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        "\", \"@widthRelative\": \"" + cellWidthRelative + "\", \"@heightRelative\": \"" + cellHeightRelative + "\"  } } } "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		NodeObject rowNode = LayoutHelper.getNodeObjectFromJsonObjectString(jsonRow);
		if (rowNode!=null)
		{
			LayoutItemDefinition layoutItemDef = LayoutItemDefinitionFactory.createDefinition(layout, parent, attName);
			if (layoutItemDef != null) 	{
				layoutItemDef.setType(attName);
				layoutItemDef.readData(rowNode);
				LayoutDefinition.readLayoutItems(rowNode, layout, layoutItemDef, 0);
				return layoutItemDef;
			}
		}
		return null;
	}

	//create tab with sections in it.
	public static TabControlDefinition getTabControlDefinition(LayoutDefinition layout, LayoutItemDefinition parent,
			List<LayoutSection> tabsSections)
	{
		TabControlDefinition tabControl = new TabControlDefinition(layout, parent);
		tabControl.setType(LayoutItemsTypes.Tab);
		String dataJson = "{ \"@class\": \"Tab\", \"@visible\": \"True\" } "; //$NON-NLS-1$
		NodeObject dataNode = LayoutHelper.getNodeObjectFromJsonObjectString(dataJson);
		if (dataNode!=null)
			tabControl.readData(dataNode);

		//Add tab Items childs.
		int position = 1;
		for (LayoutSection section : tabsSections)
		{
			JSONObject dataItemJson = new JSONObject();
			try
			{
				dataItemJson.put("@itemControlName", "Tab" + position);
				dataItemJson.put("@visible", true);
				dataItemJson.put("@caption", section.getCaption());
				dataItemJson.put("@image", section.getImage());
				dataItemJson.put("@unselectedImage", section.getImageUnSelected());
				dataItemJson.put("@class", "TabPage.UnselectedTabPage");
				dataItemJson.put("@selClass", "TabPage.SelectedTabPage");
			}
			catch (JSONException e)
			{
				Services.Log.warning("Error creating tab item JSON", e);
			}
			
			// Create tabItem
			TabItemDefinition tabItem = new TabItemDefinition(layout, tabControl);
			tabItem.setType(LayoutItemsTypes.TabPage);
			tabItem.readData(new NodeObject(dataItemJson));

			// create child table
			TableDefinition tableItemDef = getTableForSection(layout, tabItem ,section, position);
			tabItem.getChildItems().add(tableItemDef);

			//Add tabItem to tab childs
			tabControl.getChildItems().add(tabItem);
			position++;
		}

		return tabControl;
	}

	private static TableDefinition getTableForSection(LayoutDefinition layout, LayoutItemDefinition parent,
			LayoutSection section, int position) {
		TableDefinition tableDef = new TableDefinition(layout, parent);
		tableDef.setType(LayoutItemsTypes.Table);
		String tableItemJson = " { \"@controlName\": \"Tab" + position + "Table1\", " + //$NON-NLS-1$ //$NON-NLS-2$
                " \"@width\": \"100%\", \"@height\": \"100%\", \"@AutoGrow\": \"True\", " + //$NON-NLS-1$
                " \"@class\": \"Table\", \"@visible\": \"True\", \"@FixedHeightSum\": \"0\", \"@FixedWidthSum\": \"0\" } "; //$NON-NLS-1$
	    NodeObject tableItemNode = LayoutHelper.getNodeObjectFromJsonObjectString(tableItemJson);
		if (tableItemNode!=null)
			tableDef.readData(tableItemNode);

		//Add child row with a section
		String cellContent = "\"oneContent\": { \"@controlName\": \"Section" + position + "\", \"@content\": \"Section:" + section.getSection().getCode() +"\", \"@display\": \"Inline\", "+ //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        "  \"@visible\": \"True\", \"@showSectionTitle\": \"False\"  }" ; //$NON-NLS-1$
		LayoutItemDefinition layoutRowItemDef = LayoutHelper.getRowWithCell(layout, tableDef, cellContent, "100%",  //$NON-NLS-1$
				Strings.ZERO, Strings.ZERO, Strings.ZERO, Strings.ZERO,
				Strings.ZERO, Strings.ZERO, "100", "100"); //$NON-NLS-1$ //$NON-NLS-2$

		if (layoutRowItemDef!=null)
		{
			tableDef.getChildItems().add(layoutRowItemDef);
		}

		return tableDef;
	}

	//get section tab contentdefinition
	public static ContentDefinition getContentDefinition(LayoutDefinition layout, LayoutItemDefinition parent,
			List<LayoutSection> tabsSections)
	{
		ContentDefinition itemDef = new ContentDefinition(layout, parent);
		itemDef.setType(LayoutItemsTypes.OneContent);
		String dataJson = "{ \"@content\": \"Section:"+ tabsSections.get(0).getSection().getCode() + "\"," + //$NON-NLS-1$ //$NON-NLS-2$
		        "\"@display\": \"Platform Default\", \"@showSectionTitle\": \"False\", " + //$NON-NLS-1$
		        "\"@visible\": \"True\" } "; //$NON-NLS-1$

		NodeObject dataNode = LayoutHelper.getNodeObjectFromJsonObjectString(dataJson);
		if (dataNode!=null)
			itemDef.readData(dataNode);

		return itemDef;
	}

	private static NodeObject getNodeObjectFromJsonObjectString(String jsonObject)
	{
		JSONObject rowJsonObject = null;
		try {
			rowJsonObject = new JSONObject(jsonObject);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		if (rowJsonObject!=null)
		{
			return new NodeObject(rowJsonObject);
		}
		return null;
	}

	//get sections of a Detail metadata
	public static List<LayoutSection> getDetailSections(DetailDefinition formMetadata, short displayMode) {
		//For "All Sections" show a tab view with the section that correspond to this mode
		//For "Section" return the sections that are in the layout inline.
        List<LayoutSection> sections = LayoutSection.all(formMetadata.getSections());

        LayoutDefinition layout = formMetadata.getLayoutForMode(displayMode);
        if (layout != null)
        {
        	SectionsLayoutVisitor visitor = new SectionsLayoutVisitor();
        	layout.getTable().accept(visitor);
    		if (visitor.hasSections())
    			sections = visitor.getInlineSections();
        }

        List<LayoutSection> tabsSections = new ArrayList<LayoutSection>();

    	for (LayoutSection layoutSection : sections )
        {
        	if (layoutSection.getSection().getLayout(LayoutDefinition.TYPE_ANY) == null)
        		continue; // Don't show sections that have layouts defined for other platforms only (not Android and not Any).

        	if (displayMode != DisplayModes.VIEW && layoutSection.getSection().getLayout(LayoutDefinition.TYPE_EDIT) == null)
        		continue; // Don't show sections that don't have edit layouts defined when called in edit mode.

        	tabsSections.add(layoutSection);
        }

		return tabsSections;
	}
}
