package com.artech.base.metadata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import com.artech.base.metadata.layout.ILayoutItem;
import com.artech.base.metadata.loader.MetadataLoader;
import com.artech.base.model.PropertiesObject;
import com.artech.base.serialization.INodeObject;
import com.artech.base.services.Services;

public class DashboardItem extends PropertiesObject implements Serializable, ILayoutItem
{
	private static final long serialVersionUID = 1L;

	private final ArrayList<DashboardItem> m_Items = new ArrayList<DashboardItem>();
	private final Vector<ActionParameter> m_Parameters = new Vector<ActionParameter>();

	private String mObjectName;
	private String m_ImageName;
	private String m_Title;
	private String m_Name;
	private short m_Kind;
	private String mLinkType;
	private String mClass;

	@Override
	protected void internalDeserialize(INodeObject data)
	{
		super.internalDeserialize(data);

		// The base deserialization doesn't read any non-atomic nodes. So read "expression" here.
		INodeObject expression = data.optNode("expression");
		if (expression != null)
			setProperty("expression", expression);
	}

	public List<DashboardItem> getItems() {
		return m_Items;
	}

	public String getTitle() {
		return Services.Resources.getTranslation(m_Title);
	}

	public void setTitle(String value) {
		m_Title = value;
	}

	public void setKind(short s) {
		m_Kind = s;
	}

	public short getKind() {
		return m_Kind;
	}

	public String getImageName() {
		return m_ImageName;
	}

	public void setImage(String image) {
		m_ImageName = MetadataLoader.getAttributeName(image);
	}

	public void setObjectName(String attributeName) {
		mObjectName = attributeName;
	}

	public String getObjectName() {
		return mObjectName;
	}

	public List<ActionParameter> getParameters() {
		return m_Parameters;
	}

	public ActionDefinition getActionDefinition()
	{
		ActionDefinition def = new ActionDefinition(null);
		def.setInternalProperties(getInternalProperties());
		def.setGxObject(getObjectName());
		def.setGxObjectType(getKind());
		def.getParameters().addAll(m_Parameters);

		if (getItems().size() > 0)
		{
			for (int i = 0 ; i< getItems().size(); i++)
			{
				DashboardItem childItem = getItems().get(i);
				ActionDefinition childAction = childItem.getActionDefinition();
				def.getActions().add(childAction);

				//Put child childs action in definition
				//Not necessary each action has it childs.
				//addChildAction(def, childItem  );
			}
		}

		return def;
	}

	public void setName(String name) {
		m_Name = name;
	}

	@Override
	public String getName() {
		return m_Name;
	}

	public void setThemeClass(String mClass) {
		this.mClass = mClass;
	}

	public String getThemeClass() {
		return mClass;
	}
}
