package com.artech.ui;

import android.view.View;

import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.DataItem;
import com.artech.base.metadata.enums.DataTypes;
import com.artech.base.utils.Strings;

public class DragHelper
{
	public static String getDragDropType(ActionDefinition action)
	{
		if (action != null && action.getEventParameters().size() >= 1)
		{
			DataItem parameterType = action.getEventParameters().get(0).getValueDefinition();
			if (parameterType != null)
			{
				String dataType = parameterType.getDataTypeName().GetDataType();
				if (dataType.equalsIgnoreCase(DataTypes.sdt) || dataType.equalsIgnoreCase(DataTypes.businesscomponent))
					return parameterType.optStringProperty("TypeName");
			}
		}

		return Strings.EMPTY;
	}

	public static class DragLocalState
	{
		public final View draggedControl;
		public boolean dragStarted;
		public boolean dragFinished;

		public DragLocalState(View control)
		{
			draggedControl = control;
		}
	}
}