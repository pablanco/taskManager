package com.artech.base.metadata;

import java.io.Serializable;
import java.util.List;
import java.util.Vector;

public class RelationDefinition implements Serializable
{
	private static final long serialVersionUID = 1L;

	private String m_BCRelated;
	private String m_Name;
	private final Vector<String> m_Keys = new Vector<String>();
	private final Vector<String> m_InferredAtts = new Vector<String>();

	public void setBCRelated(String bcRelated) {
		m_BCRelated = bcRelated;
	}
	public String getBCRelated() {
		return m_BCRelated;
	}

	public List<String> getInferredAtts() {
		return m_InferredAtts;
	}

	public List<String> getKeys() {
		return m_Keys;
	}
	public void setName(String string) {
		m_Name = string;
	}
	public String getName() {
		return m_Name;
	}
}
