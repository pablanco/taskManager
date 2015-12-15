package com.artech.base.metadata;

import java.util.ArrayList;
import java.util.List;

import com.artech.base.metadata.enums.Connectivity;
import com.artech.base.utils.Strings;

public class StructureDefinition implements IPatternMetadata
{
	private static final long serialVersionUID = 1L;

	public static final StructureDefinition EMPTY = new StructureDefinition(Strings.EMPTY);

	private String mName;
	private Connectivity mConnectivitySupport = Connectivity.Inherit;
	public LevelDefinition Root = new LevelDefinition(null);
	public List<RelationDefinition> ManyToOneRelations = new ArrayList<RelationDefinition>();

	public StructureDefinition(String name)
	{
		mName = name;
	}

	@Override
	public String getName() { return mName; }
	@Override
	public void setName(String name) { mName = name; }

	public DataItem getAttribute(String name) {
		return Root.getAttribute(name);
	}

	public DataItem getDescriptionAttribute() {
		return Root.getDescriptionAttribute();
	}

	public LevelDefinition getLevel(String code)
	{
		if (Root.getName().equalsIgnoreCase(code))
			return Root;
		return getSubLevel(code);
	}

	private LevelDefinition getSubLevel(String name)
	{
		return Root.getLevel(name);
	}

	public List<DataItem> getItems()
	{
		return Root.getAttributes();
	}

	public void merge(StructureDefinition bc)
	{
		//this.m_Name = bc.getName();
		//this.Root.setName(bc.Root.getName());
		Root.merge(bc.Root);
		ManyToOneRelations = bc.ManyToOneRelations;
	}

	public boolean isEmpty()
	{
		return (getItems().size() == 0);
	}

	public Connectivity getConnectivitySupport() {
		return mConnectivitySupport;
	}

	public void setConnectivitySupport(Connectivity val) {
		mConnectivitySupport = val;
	}


}
