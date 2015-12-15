package com.artech.base.metadata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import com.artech.base.metadata.enums.GxObjectTypes;
import com.artech.base.metadata.loader.MetadataLoader;
import com.artech.base.model.PropertiesObject;
import com.artech.base.serialization.INodeObject;
import com.artech.base.services.Services;

public class ActionDefinition extends PropertiesObject implements Serializable
{
	private static final long serialVersionUID = 1L;

	private String mName;
	private final IDataViewDefinition mDataView;
	private String mActionType;

	 // TODO: We should separate "action" from "event". "Action" is a line of a composite block, "Event" is the whole thing.
	private final ArrayList<ActionParameter> mEventParameters = new ArrayList<ActionParameter>();
	private final Vector<ActionParameter> mParameters = new Vector<ActionParameter>();

	private short m_gxObjectType = GxObjectTypes.NONE;
	private String m_gxObject = null;

	public static class STANDARD_ACTION
	{
		public static final String REFRESH = "Refresh"; //$NON-NLS-1$
		public static final String SEARCH = "Search"; //$NON-NLS-1$
		public static final String FILTER = "Filter"; //$NON-NLS-1$
		public static final String INSERT = "Insert"; //$NON-NLS-1$
		public static final String UPDATE = "Update"; //$NON-NLS-1$
		public static final String EDIT = "Edit"; //$NON-NLS-1$
		public static final String DELETE = "Delete"; //$NON-NLS-1$
		public static final String SAVE = "Save"; //$NON-NLS-1$
		public static final String CANCEL = "Cancel"; //$NON-NLS-1$
		public static final String SHARE = "Share"; //$NON-NLS-1$
	}

	public ActionDefinition(IDataViewDefinition dv)
	{
		mDataView = dv;
	}

	@Override
	protected void internalDeserialize(INodeObject data)
	{
		super.internalDeserialize(data);

		// The base deserialization doesn't read any non-atomic nodes. So read extras here.
		deserializeSubNode(data, "expression");
		deserializeSubNode(data, "assignExpression");
	}

	private void deserializeSubNode(INodeObject data, String key)
	{
		INodeObject expression = data.optNode(key);
		if (expression != null)
			setProperty(key, expression);
	}

	// For composed Actions
	private final ArrayList<ActionDefinition> mActions = new ArrayList<ActionDefinition>();

	public List<ActionParameter> getEventParameters() { return mEventParameters; }
	public Vector<ActionParameter> getParameters() { return mParameters; }

	public ActionParameter getParameter(int position)
	{
		if (position < mParameters.size())
			return mParameters.get(position);

		return null;
	}

	public IDataViewDefinition getDataView() { return mDataView; }

	public String getName()
	{
		if (mName == null)
			mName = optStringProperty("@name"); //$NON-NLS-1$

		return mName;
	}

	public List<ActionDefinition> getActions()
	{
		return mActions;
	}

	public String getGxObject() { return m_gxObject; }
	public void setGxObject(String value) { m_gxObject = value; }

	public short getGxObjectType() { return m_gxObjectType; }
	public void setGxObjectType(short value) { m_gxObjectType = value; }

	public String getActionType()
	{
		if (mActionType == null)
			mActionType = optStringProperty("@actionType"); //$NON-NLS-1$

		return mActionType;
		
	}
	public MultipleSelectionInfo getMultipleSelectionInfo()
	{
		if (Services.Strings.hasValue(optStringProperty(MultipleSelectionInfo.PROP_FOREACHLINE)))
			return new MultipleSelectionInfo(this);
		else
			return null;
	}

	public static class MultipleSelectionInfo
	{
		private final String mGrid;
		private final String mCallTarget;
		private final boolean mUseSelection;

		private static final String PROP_FOREACHLINE = "@forEachLine"; //$NON-NLS-1$

		private MultipleSelectionInfo(ActionDefinition action)
		{
			mGrid = action.optStringProperty("@forEachGrid"); //$NON-NLS-1$
			mCallTarget = MetadataLoader.getObjectName(action.optStringProperty("@call")); //$NON-NLS-1$
			mUseSelection = action.optStringProperty(PROP_FOREACHLINE).equalsIgnoreCase("selected"); //$NON-NLS-1$
		}

		public String getGrid() { return mGrid; }
		public String getTarget() { return mCallTarget; }
		public boolean useSelection() { return mUseSelection; }
	}

	public static class DependencyInfo
	{
		public static final String SERVICE = "@DependencyService"; //$NON-NLS-1$
		public static final String SERVICE_INPUT = "@DependencyServiceInput"; //$NON-NLS-1$
		public static final String SERVICE_OUTPUT = "@DependencyServiceOutput"; //$NON-NLS-1$
	}
}
