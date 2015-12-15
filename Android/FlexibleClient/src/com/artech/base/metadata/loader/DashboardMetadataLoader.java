package com.artech.base.metadata.loader;

import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.DashboardItem;
import com.artech.base.metadata.DashboardMetadata;
import com.artech.base.metadata.IPatternMetadata;
import com.artech.base.metadata.enums.GxObjectTypes;
import com.artech.base.serialization.INodeCollection;
import com.artech.base.serialization.INodeObject;
import com.artech.base.services.IContext;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;

public class DashboardMetadataLoader extends MetadataLoader
{
	public final static short COMPONENT_KIND = 100;

	@Override
	public IPatternMetadata load(IContext context, String name)
	{
		INodeObject jsonData = getDefinition(context, Strings.toLowerCase(name));
		if (jsonData == null)
		{
			Services.Log.Error("Could not connect to metadata for " + name); //$NON-NLS-1$
			return null;
		}

		return loadJSON(jsonData);
	}

	private DashboardMetadata loadJSON(INodeObject jsonData)
	{
		if (jsonData == null)
			return null;

		DashboardMetadata dashboardMetadata = new DashboardMetadata();
		INodeObject dashboardNode = jsonData.getNode("Dashboard"); //$NON-NLS-1$

		//Read Dashboard Data
		String background = MetadataLoader.getAttributeName(dashboardNode.getString("@Background")); //$NON-NLS-1$
		String header = MetadataLoader.getAttributeName(dashboardNode.getString("@Header")); //$NON-NLS-1$
		String title = MetadataLoader.getAttributeName(dashboardNode.getString("@Title"));  //$NON-NLS-1$
		String themeClass = dashboardNode.optString("@Class"); //$NON-NLS-1$
		dashboardMetadata.setCaption(title);
		dashboardMetadata.setThemeClass(themeClass);

		String control = dashboardNode.optString("@Control"); //$NON-NLS-1$
		dashboardMetadata.setControl(control);
		String userControl = dashboardNode.optString("@UserControl"); //$NON-NLS-1$
		dashboardMetadata.setUserControl(userControl);

		boolean showAds = dashboardNode.optBoolean("@showAds"); //$NON-NLS-1$
		dashboardMetadata.setShowAds(showAds);
		String adsPosition = dashboardNode.optString("@adsPosition"); //$NON-NLS-1$
		dashboardMetadata.setAdsPosition(adsPosition);

		dashboardMetadata.setBackgroundImage(background);
		dashboardMetadata.setHeaderImage(header);

		dashboardMetadata.setShowApplicationBar(dashboardNode.optBoolean("@showApplicationBars"));
		dashboardMetadata.setApplicationBarClass(dashboardNode.optString("@applicationBarsClass"));

		//Read Instance Properties
		INodeObject instancePropNode = dashboardNode.optNode("instanceProperties"); //$NON-NLS-1$
		dashboardMetadata.getInstanceProperties().deserialize(instancePropNode);

		//read variables (before items and events)
		WorkWithMetadataLoader.readVariables(dashboardMetadata, dashboardNode);
		
		INodeCollection itemsNode = dashboardNode.optCollection("Item"); //$NON-NLS-1$
		for (int i = 0; i < itemsNode.length() ; i++)
		{
			INodeObject itemNode = itemsNode.getNode(i);
			DashboardItem item = loadDashboardItem(dashboardMetadata, itemNode);
			if (item != null)
				dashboardMetadata.getItems().add(item);
		}

		// Read events.
		INodeObject eventsNode = dashboardNode.optNode("events");
		if (eventsNode != null)
		{
			for (INodeObject eventNode : eventsNode.optCollection("Item"))
			{
				DashboardItem item = loadDashboardItem(dashboardMetadata, eventNode);
				if (item != null)
				{
					// Unlike normal items, events can have parameters too.
					ActionDefinition event = item.getActionDefinition();
					WorkWithMetadataLoader.readEventParameters(dashboardMetadata, event.getEventParameters(), eventNode);
					dashboardMetadata.getEvents().add(event);
				}
			}
		}

		//read notifications
		INodeObject jsonNotifications = dashboardNode.optNode("notifications"); //$NON-NLS-1$
		if (jsonNotifications!=null)
		{
			INodeCollection actionsNode = jsonNotifications.optCollection("Action"); //$NON-NLS-1$
			for (int i = 0; i < actionsNode.length() ; i++)
			{
				INodeObject actionNode = actionsNode.getNode(i);
				DashboardItem item = loadDashboardItem(dashboardMetadata, actionNode);
				if (item != null)
					dashboardMetadata.getNotificationActions().put(item.getName(), item);
			}
		}

		return dashboardMetadata;
	}

	/*
	 * Load a dashboaritem from a JSON representation, if it is not possible it returns null
	 */
	private DashboardItem loadDashboardItem(DashboardMetadata dashboard, INodeObject itemNode)
	{
		DashboardItem item = new DashboardItem();

		String image = MetadataLoader.getAttributeName(itemNode.optString("@Image")); //$NON-NLS-1$

		String data = itemNode.optString("@Data", Strings.EMPTY); //$NON-NLS-1$
		String name = itemNode.optString("@Name", Strings.EMPTY); //$NON-NLS-1$
		String cls = itemNode.optString("@Class", Strings.EMPTY); //$NON-NLS-1$
		String description = itemNode.optString("@Description", Strings.EMPTY); //$NON-NLS-1$
		String link = itemNode.optString("@Link", Strings.EMPTY); //$NON-NLS-1$

	    item.deserialize(itemNode);

	    item.setThemeClass(cls);

		// Read Sub Items
		INodeCollection parArray = itemNode.optCollection("Item"); //$NON-NLS-1$
		for (int k = 0; k < parArray.length() ; k++) {
			DashboardItem childItem = loadDashboardItem(dashboard, parArray.getNode(k));
			item.getItems().add(childItem);
		}

		WorkWithMetadataLoader.readActionParameters(dashboard, item.getParameters(), itemNode);

		item.setName(name);
		item.setTitle(description);

		if (link.length() > 0)
		{
			item.setKind(COMPONENT_KIND);
			item.setObjectName(link);
		}
		else
		{
			item.setKind( GxObjectTypes.getGxObjectTypeFromName(data));
			item.setObjectName(MetadataLoader.getAttributeName(data));
		}

		item.setImage(image);

		return item;
	}
}