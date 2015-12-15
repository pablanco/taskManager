
package com.artech.usercontrols;

import java.lang.reflect.Constructor;
import java.util.HashMap;

import android.content.Context;

import com.artech.android.layout.GridContext;
import com.artech.base.metadata.layout.ControlInfo;
import com.artech.base.metadata.layout.GridDefinition;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.services.Services;
import com.artech.controls.GxListView;
import com.artech.controls.IGridView;
import com.artech.ui.Coordinator;
import com.artech.utils.Cast;

public class UcFactory
{
	private static final Class<?>[] sConstructorSignature1 = new Class[] { Context.class, Coordinator.class, LayoutItemDefinition.class};
	private static final Class<?>[] sConstructorSignature2 = new Class[] { Context.class, LayoutItemDefinition.class};
	private static final Class<?>[] sConstructorSignature3 = new Class[] { GridContext.class, Coordinator.class, GridDefinition.class};

	private static final HashMap<String, UserControlDefinition> sClasses = new HashMap<String, UserControlDefinition>();
	private static final HashMap<String, Constructor<?>> sConstructorMap = new HashMap<String, Constructor<?>>();

	public static  Class<?> getClass(String userControl)
	{
		UserControlDefinition ucDefinition = sClasses.get(userControl);
		if (ucDefinition != null)
		{
			try
			{
				return Class.forName(ucDefinition.ClassName);
			}
			catch (ClassNotFoundException e)
			{
				Services.Log.Error(String.format("User control class '%s' could not be loaded via reflection.", ucDefinition.Name), e); //$NON-NLS-1$
			}
		}
		return null;
	}

	public static void addControl(String cls, UserControlDefinition def) {
		sClasses.put(cls, def);
	}

	public static UserControlDefinition getControlDefinition(String controlName) {
		return sClasses.get(controlName);
	}

	public static Object getUcConstructor(Class<?> clazz, String controlName, Context context, Coordinator coordinator, LayoutItemDefinition layoutItemDefinition)
	{
		try
		{
			Constructor<?> constructor = sConstructorMap.get(controlName);

			// We accept alternate signatures, try to get any of them.
			if (constructor == null)
				constructor = getConstructor(clazz, sConstructorSignature1);

			if (constructor == null)
				constructor = getConstructor(clazz, sConstructorSignature2);

			if (constructor == null && layoutItemDefinition instanceof GridDefinition)
				constructor = getConstructor(clazz, sConstructorSignature3);

			if (constructor == null)
			{
				Services.Log.Error(String.format("User control class '%s' does not have an appropriate constructor.", clazz.getName())); //$NON-NLS-1$
				return null;
			}

			sConstructorMap.put(controlName, constructor);

			// We accept two constructors; one receiving Coordinator, and one without. Build arguments accordingly.
			Object[] constructorArgs;
			if (constructor.getParameterTypes().length == 3)
				constructorArgs = new Object[] { context, coordinator, layoutItemDefinition };
			else
				constructorArgs = new Object[] { context, layoutItemDefinition };

			return constructor.newInstance(constructorArgs);
		}
		catch (Exception ex)
		{
			Services.Log.Error("Exception creating UserControl.", ex); //$NON-NLS-1$
			return null;
		}
	}

	/**
	 * Same as java.lang.Class.getConstructor(), but returns null instead of throwing an exception when there is no match.
	 */
	private static Constructor<?> getConstructor(Class<?> clazz, Class<?>... parameterTypes)
	{
		try
		{
			return clazz.getConstructor(parameterTypes);
		}
		catch (NoSuchMethodException noConstructor)
		{
			return null;
		}
	}

	public static IGxUserControl createUserControl(Context context, Coordinator coordinator, LayoutItemDefinition layoutItemDefinition)
	{
		ControlInfo controlInfo = layoutItemDefinition.getControlInfo();
		String name = controlInfo.getControl();
		Class<?> clazz = getClass(controlInfo.getControl());
		if (clazz == null)
			return null; // no user control found

		return (IGxUserControl)getUcConstructor(clazz, name, context, coordinator, layoutItemDefinition);
	}

	public static IGridView createGrid(GridContext context, Coordinator coordinator, GridDefinition item)
	{
		if (item.getControlInfo() != null)
		{
			IGxUserControl gridControl = createUserControl(context, coordinator, item);
			if (gridControl != null && Cast.as(IGridView.class, gridControl) != null)
				return (IGridView) gridControl;
		}

		return new GxListView(context, item);
	}
}