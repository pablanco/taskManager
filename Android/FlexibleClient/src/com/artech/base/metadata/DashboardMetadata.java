package com.artech.base.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import android.annotation.SuppressLint;

import com.artech.application.MyApplication;
import com.artech.base.metadata.enums.Connectivity;
import com.artech.base.metadata.theme.TabControlThemeClassDefinition;
import com.artech.base.metadata.theme.ThemeApplicationBarClassDefinition;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.services.Services;
import com.artech.base.utils.PlatformHelper;

public class DashboardMetadata implements IPatternMetadata, IViewDefinition, ILayoutDefinition
{
	private static final long serialVersionUID = 1L;

	public static final String CONTROL_TABS = "Tabs"; //$NON-NLS-1$
	public static final String CONTROL_LIST = "List"; //$NON-NLS-1$
	public static final String CONTROL_GRID = "Grid"; //$NON-NLS-1$

	private final ArrayList<DashboardItem> mItems;
	private final ArrayList<ActionDefinition> mEvents;
	private final List<VariableDefinition> mVariables;
	private final HashMap<String, DashboardItem> mNotificationActions;

	private String mBackgroundImage;
	private String mHeaderImage;
	private String mTitle;
	private String mName;
	private String mControl;
	private String mUserControl;
	private String mThemeClass;

	private final InstanceProperties mInstanceProperties = new InstanceProperties();
	private boolean mShowAds;
	private String mAdsPosition;

	private boolean mShowApplicationBar;
	private String mApplicationBarClass;

	public DashboardMetadata()
	{
		mItems = new ArrayList<DashboardItem>();
		mEvents = new ArrayList<ActionDefinition>();
		mVariables = new ArrayList<VariableDefinition>();
		mNotificationActions = new HashMap<String, DashboardItem>();
	}

	public String getBackgroundImage() { return mBackgroundImage; }
	public void setBackgroundImage(String image) { mBackgroundImage = image; }

	public String getHeaderImage() { return mHeaderImage; }
	public void setHeaderImage(String image) { mHeaderImage = image; }

	public List<DashboardItem> getItems() { return mItems; }
	public List<ActionDefinition> getEvents() { return mEvents; }

	@Override
	public List<ObjectParameterDefinition> getParameters()
	{
		// Dashboards, for now, do not have parameters.
		return Collections.emptyList();
	}

	public HashMap<String, DashboardItem> getNotificationActions() { return mNotificationActions; }

	@Override
	public String getCaption() { return Services.Resources.getTranslation(mTitle); }
	public void setCaption(String value) { mTitle = value; }

	@Override
	public String getObjectName() { return mName; }

	@Override
	public String getName() { return mName; }

	@Override
	public void setName(String name) { mName = name; }

	public String getControl() { return mControl; }
	public void setControl(String value) { mControl = value; }

	public String getUserControl() { return mUserControl; }
	public void setUserControl(String value) { mUserControl = value; }

	public void setThemeClass(String value) { mThemeClass = value; }

	public ThemeClassDefinition getThemeClassForGrid()
	{
		ThemeClassDefinition dashboardClass = PlatformHelper.getThemeClass(mThemeClass);
		if (dashboardClass != null)
			return PlatformHelper.getThemeClass(dashboardClass.getThemeGrid());
		else
			return null;
	}

	public TabControlThemeClassDefinition getThemeClassForTabs()
	{
		ThemeClassDefinition dashboardClass = PlatformHelper.getThemeClass(mThemeClass);
		if (dashboardClass != null)
			return PlatformHelper.getThemeClass(TabControlThemeClassDefinition.class, dashboardClass.getThemeTab());
		else
			return null;
	}

	@Override
	public InstanceProperties getInstanceProperties() { return mInstanceProperties; }

	//Ads
	public void setShowAds(boolean value) { mShowAds = value; }
	public boolean getShowAds() { return mShowAds; }


	public void setAdsPosition(String value) { mAdsPosition = value; }
	public String getAdsPosition() { return mAdsPosition; }

	public boolean getShowLogout()
	{
		return mInstanceProperties.getShowLogoutButton();
	}

	@Override
	public boolean isSecure()
	{
		return (MyApplication.getApp().isSecure() && !getInstanceProperties().notSecureInstance());
	}

	@Override
	public boolean getShowApplicationBar() { return mShowApplicationBar; }
	public void setShowApplicationBar(boolean value) { mShowApplicationBar = value; }

	//Dashboard do not have a Layout, neither a main table.
	@Override
	public boolean getEnableHeaderRowPattern() { return false; }

	@Override
	public ThemeApplicationBarClassDefinition getHeaderRowApplicationBarClass()
	{
		return null;
	}

	@Override
	public ThemeApplicationBarClassDefinition getApplicationBarClass()
	{
		return PlatformHelper.getThemeClass(ThemeApplicationBarClassDefinition.class, mApplicationBarClass);
	}

	public void setApplicationBarClass(String value) { mApplicationBarClass = value; }

	@Override
	public List<VariableDefinition> getVariables()
	{
		return mVariables;
	}

	// Connectivity Support
	@Override
	public Connectivity getConnectivitySupport() {
		return mInstanceProperties.getConnectivitySupport();
	}

	@SuppressLint("DefaultLocale")
	@Override
	public VariableDefinition getVariable(String name) {
		String variableName = name.replace("&", "");
		for (VariableDefinition var : getVariables()) {
			if (var.getName().equalsIgnoreCase(variableName)) {
				return var;
			}
		}
		return null;
	}

	@Override
	public ActionDefinition getEvent(String name)
	{
		return Events.find(mEvents, name);
	}

	@Override
	public ActionDefinition getClientStart()
	{
		return getEvent(Events.CLIENT_START);
	}
}
