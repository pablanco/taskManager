package com.artech.base.metadata;

import java.util.List;
import java.util.Vector;

import com.artech.base.metadata.layout.LayoutDefinition;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.metadata.loader.MetadataLoader;
import com.artech.base.services.Services;

public class SectionDefinition extends DataViewDefinition
{
	private static final long serialVersionUID = 1L;

	private final DetailDefinition mParent;
	private final Vector<LayoutItemDefinition> m_VisibleItems = new Vector<LayoutItemDefinition>();
	private final Vector<LayoutItemDefinition> m_VisibleEditItems = new Vector<LayoutItemDefinition>();

	public SectionDefinition(DetailDefinition parent)
	{
		mParent = parent;
	}

	@Override
	public String getName()
	{
		// WWSDCustomer.Customer.Detail.Invoices
		return String.format("%s.%s", mParent.getName(), getCode()); //$NON-NLS-1$
	}

	public Vector<LayoutItemDefinition> getVisibleItems(String type)
	{
		//generate default structure
		generateForm(getStructure());
		if (type.equals(LayoutDefinition.TYPE_VIEW))
			return m_VisibleItems;
		else
			return m_VisibleEditItems;
	}

	@Override
	public WorkWithDefinition getPattern()
	{
		return mParent.getParent().getParent();
	}

	public String getCode()
	{
		return super.optStringProperty("@code"); //$NON-NLS-1$
	}

	public String getImage()
	{
		return MetadataLoader.getObjectName(optStringProperty("@image")); //$NON-NLS-1$
	}

	public String getImageUnSelected()
	{
		return MetadataLoader.getObjectName(optStringProperty("@unselectedImage")); //$NON-NLS-1$
	}

	public DetailDefinition getParent() { return mParent; }

	private void generateItemsForLayout(StructureDefinition definition, LayoutDefinition layDef, Vector<LayoutItemDefinition> formItems)
	{
		if (layDef==null)
			throw new IllegalArgumentException("null layout in generateItemsForLayout"); //$NON-NLS-1$

		layDef.getDataItems(null, formItems);
		matchRelations(definition, formItems);
	}

	private void generateForm(StructureDefinition definition) {
		//View Items
		if (m_VisibleItems.size() == 0) {
			LayoutDefinition layDef = getLayout(LayoutDefinition.TYPE_VIEW);
			if (layDef != null)
				generateItemsForLayout(definition, layDef, m_VisibleItems);
		}

		//Edit Items
		if (m_VisibleEditItems.size() == 0) {
			LayoutDefinition layDef = getLayout(LayoutDefinition.TYPE_EDIT);
			if (layDef != null)
				generateItemsForLayout(definition, layDef, m_VisibleEditItems);
		}
	}

	private LayoutItemDefinition getFormItem(List<LayoutItemDefinition> visibleItems, String name)
	{
		for (LayoutItemDefinition def : visibleItems)
		{
			String dataId = def.getDataId();
			if (dataId != null && dataId.equalsIgnoreCase(name))
				return def;
		}

		return null;
	}

	private boolean MatchRelation(RelationDefinition relation)
	{
		for (String att : relation.getKeys())
		{
			if (getDataItem(att) == null)
				return false;
		}

		return true;
	}

	private void matchRelations(StructureDefinition structure, List<LayoutItemDefinition> visibleItems)
	{
		if (structure == null)
			return; // It hasn't BC associated

		for(int i = 0; i < structure.ManyToOneRelations.size(); i++)
		{
			RelationDefinition relation = structure.ManyToOneRelations.get(i);
			if (MatchRelation(relation))
			{
				StructureDefinition def = Services.Application.getBusinessComponent(relation.getBCRelated());
				//BC could not be defined in SD WW
				if (def != null)
				{
					DataItem descAttribute = def.getDescriptionAttribute();

					LayoutItemDefinition formitem = null;
					if (descAttribute!=null)
						formitem = getFormItem(visibleItems, descAttribute.getName());
					if (formitem!=null)
						formitem.setFK(relation);
					else
					{
						int keySize = relation.getKeys().size();
						// Add the FK relation in the last attribute of the key
						if (keySize > 0)
						{
							String attRef = relation.getKeys().get(keySize - 1);
							formitem = getFormItem(visibleItems, attRef);
							if (formitem != null)
								formitem.setFK(relation);
							else
							{
								// Add the FK relation in the first inferred att
								int inferredSize = relation.getInferredAtts().size();
								for (int j = 0; j < inferredSize; j++)
								{
									attRef = relation.getInferredAtts().get(j);
									formitem = getFormItem(visibleItems, attRef);
									if (formitem != null)
										formitem.setFK(relation);
								}
							}
						}
					}
				}
			}
		}
	}

	public StructureDefinition getBusinessComponent()
	{
		return Services.Application.getBusinessComponent(getBusinessComponentName());
	}

	private String getBusinessComponentName()
	{
		if (getMainDataSource() != null)
			return getMainDataSource().getAssociatedBCName();

		return super.optStringProperty("BC"); //$NON-NLS-1$
	}
}
