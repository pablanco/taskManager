package com.artech.base.metadata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.artech.base.metadata.enums.ActionTypes;
import com.artech.base.metadata.enums.ControlTypes;
import com.artech.base.metadata.enums.DataTypes;
import com.artech.base.utils.Strings;

public class DataTypeName implements Serializable
{
	private static final long serialVersionUID = 1L;

	private String m_DataType;

	public String GetDataType()
	{
		return m_DataType;
	}

	public DataTypeName(String dataType)
	{
		dataType = Strings.toLowerCase(dataType);
		if (dataType.equals("int")) //$NON-NLS-1$
			dataType = "numeric"; //$NON-NLS-1$
		if (dataType.equals("char")) //$NON-NLS-1$
			dataType = "string"; //$NON-NLS-1$
		if (dataType.equals("vchar")) //$NON-NLS-1$
			dataType = "string"; //$NON-NLS-1$
		if (dataType.equals("svchar")) //$NON-NLS-1$
			dataType = "string"; //$NON-NLS-1$
		if (dataType.equals("boolean")) //$NON-NLS-1$
			dataType = "bool"; //$NON-NLS-1$

		m_DataType = dataType;
	}

	public List<String> GetActions()
	{
		ArrayList<String> list = new ArrayList<String>();
		if (m_DataType != null)
		{
			if (m_DataType.equals(DataTypes.email))
				list.add(ActionTypes.SendEmail);
			else if (m_DataType.equals(DataTypes.address))
				list.add(ActionTypes.LocateAddress);
			else if (m_DataType.equals(DataTypes.phone))
				list.add(ActionTypes.CallNumber);
			else if (m_DataType.equals(DataTypes.url))
				list.add(ActionTypes.ViewUrl);
			else if (m_DataType.equals(DataTypes.geolocation))
				list.add(ActionTypes.LocateGeoLocation);
		}

		return list;
	}

	public String GetControlType()
	{
		if (m_DataType != null)
		{
			if (m_DataType.equals(DataTypes.geolocation))
				return ControlTypes.LocationControl;
			if (m_DataType.equals(DataTypes.numeric))
				return ControlTypes.NumericTextBox;
			else  if (m_DataType.equals(DataTypes.phone))
				return ControlTypes.PhoneNumericTextBox;
			else if (m_DataType.equals(DataTypes.video))
				return ControlTypes.VideoView;
			else if (m_DataType.equals(DataTypes.audio))
				return ControlTypes.AudioView;
			else if (m_DataType.equals(DataTypes.email))
				return ControlTypes.EmailTextBox;
			else if (m_DataType.equals(DataTypes.date) || m_DataType.equals(DataTypes.dtime) || m_DataType.equals(DataTypes.time) || m_DataType.equals(DataTypes.datetime))
				return ControlTypes.DateBox;
			else if (m_DataType.equals(DataTypes.photo) || m_DataType.equals(DataTypes.photourl) || m_DataType.equals(DataTypes.image) )
				return ControlTypes.PhotoEditor;
			else if (m_DataType.equals(DataTypes.component) || m_DataType.equals(DataTypes.url) || m_DataType.equals(DataTypes.html))
				return ControlTypes.WebView;
			else
				return ControlTypes.TextBox;
		}
		return null;
	}
}
