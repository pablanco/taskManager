package com.artech.base.metadata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.artech.application.MyApplication;
import com.artech.base.metadata.enums.Connectivity;
import com.artech.base.metadata.enums.DisplayModes;
import com.artech.base.metadata.layout.LayoutDefinition;
import com.artech.base.metadata.layout.LayoutsPerOrientation;
import com.artech.base.metadata.rules.RulesDefinition;
import com.artech.base.model.PropertiesObject;
import com.artech.base.serialization.INodeObject;
import com.artech.base.services.Services;
import com.artech.base.utils.NameMap;
import com.artech.base.utils.PlatformHelper;

public abstract class DataViewDefinition
	extends PropertiesObject
	implements IDataViewDefinition, Serializable
{
	private static final long serialVersionUID = 1L;

	private IDataSourceDefinition mMainDataSource;

	private final DataSourceDefinitionList mDataSources;
	private final List<ObjectParameterDefinition> mParameters;
	private final RulesDefinition mRules;
	private final List<LayoutDefinition> mLayouts;
	private final List<VariableDefinition> mVariables;
	private final List<ActionDefinition> mActions;

	private final NameMap<LayoutsPerOrientation> mChosenLayouts; // in a dictionary, by type (view/edit).

	public DataViewDefinition()
	{
		mDataSources = new DataSourceDefinitionList();
		mParameters = new ArrayList<ObjectParameterDefinition>();
		mRules = new RulesDefinition(this);
		mLayouts = new ArrayList<LayoutDefinition>();
		mVariables = new ArrayList<VariableDefinition>();
		mActions = new ArrayList<ActionDefinition>();
		mChosenLayouts = new NameMap<LayoutsPerOrientation>();
	}

	@Override
	public String getObjectName()
	{
		return getPattern().getName();
	}

	@Override
	public String toString()
	{
		return getName();
	}

	@Override
	public void deserialize(INodeObject obj)
	{
		super.deserialize(obj);

		// Force deserialization of JSON immediatly. It contains a lot of information, including e.g. layouts
		// and actions which are deserialized independently.
		internalDeserialize();
	}

	@Override
	public DataSourceDefinitionList getDataSources() { return mDataSources;	}

	@Override
	public IDataSourceDefinition getMainDataSource()
	{
		return mMainDataSource;
	}

	public void setMainDataSource(IDataSourceDefinition dataSource) { mMainDataSource = dataSource; }

	/* ----------------------------------------------------------
	 *  START DEPRECATED SECTION - MUST BE DELETED!
	 */

	@Deprecated
	public StructureDefinition getStructure()
	{
		return (getMainDataSource() != null ? getMainDataSource().getStructure() : null);
	}

	@Deprecated
	public DataItem getDataItem(String name)
	{
		return (getMainDataSource() != null ? getMainDataSource().getDataItem(name) : null);
	}

	/* ----------------------------------------------------------
	 *  END DEPRECATED SECTION - MUST BE DELETED!
	 */

	@Override
	public List<VariableDefinition> getVariables()
	{
		return mVariables;
	}

	@Override
	public VariableDefinition getVariable(String name)
	{
		name = DataItemHelper.getNormalizedName(name);
		for (VariableDefinition var : getVariables())
		{
			if (var.getName().equalsIgnoreCase(name))
				return var;
		}

		return null;
	}

	@Override
	public List<ActionDefinition> getActions()
	{
		return mActions;
	}

	@Override
	public String getCaption()
	{
		return Services.Resources.getTranslation(optStringProperty("@caption")); //$NON-NLS-1$
	}

	@Override
	public LayoutDefinition getLayoutForMode(short mode)
	{
		LayoutDefinition layout;
		if (mode == DisplayModes.VIEW)
			layout = getLayout(LayoutDefinition.TYPE_VIEW);
		else
			layout = getLayout(LayoutDefinition.TYPE_EDIT);

		if (layout == null)
			layout = getLayout(LayoutDefinition.TYPE_ANY);

		return layout;
	}

	@Override
	public LayoutDefinition getLayout(String type)
	{
		LayoutsPerOrientation layouts = mChosenLayouts.get(type);
		if (layouts == null)
		{
			layouts = chooseLayouts(type, mLayouts);
			mChosenLayouts.put(type, layouts);
		}

		return layouts.getCurrent();
	}

	private static LayoutsPerOrientation chooseLayouts(String type, List<LayoutDefinition> availableLayouts)
	{
		if (availableLayouts == null || availableLayouts.size() == 0)
			return LayoutsPerOrientation.none();

		List<LayoutDefinition> layoutsOfType;
		if (type != null && !type.equals(LayoutDefinition.TYPE_ANY))
		{
			layoutsOfType = new ArrayList<LayoutDefinition>();
			for (LayoutDefinition layDef : availableLayouts)
			{
				if (layDef.getType().equalsIgnoreCase(type))
				{
					layDef.deserialize();
					layoutsOfType.add(layDef);
				}
			}
		}
		else
			layoutsOfType = new ArrayList<LayoutDefinition>(availableLayouts);

		// Choose the best ones.
		return PlatformHelper.bestLayouts(layoutsOfType);
	}

	@Override
	public List<LayoutDefinition> getLayouts()
	{
		return mLayouts;
	}

	@Override
	public List<ObjectParameterDefinition> getParameters()
	{
		return mParameters;
	}

	@Override
	public RulesDefinition getRules()
	{
		return mRules;
	}

	@Override
	public boolean isSecure()
	{
		// If application is secure, then any data view is secure UNLESS:
		// * It's explicitly marked not to be so.
		// * It's the login screen itself (workaround for ide bug).
		if (MyApplication.getApp().isSecure())
		{
			if (getPattern().getInstanceProperties().notSecureInstance())
				return false;

			String loginObject = MyApplication.getApp().getLoginObject();
			return !(loginObject != null && loginObject.equalsIgnoreCase(getPattern().getName()));

		}
		else
			return false;
	}

	//Ads
	@Override
	public boolean getShowAds()
	{
		return super.optBooleanProperty("@showAds"); //$NON-NLS-1$
	}

	// Connectivity Support
	@Override
	public Connectivity getConnectivitySupport() {
		return getPattern().getInstanceProperties().getConnectivitySupport();
	}

	@Override
	public String getAdsPosition()
	{
		return super.optStringProperty("@adsPosition"); //$NON-NLS-1$
	}

	@Override
	public InstanceProperties getInstanceProperties()
	{
		return getPattern().getInstanceProperties();
	}

	@Override
	public ActionDefinition getEvent(String name)
	{
		return Events.find(mActions, name);
	}

	@Override
	public ActionDefinition getClientStart()
	{
		return getEvent(Events.CLIENT_START);
	}
}
