package com.artech.fragments;

import android.support.annotation.NonNull;

import com.artech.app.ComponentParameters;
import com.artech.base.metadata.DashboardMetadata;
import com.artech.base.metadata.IDataViewDefinition;
import com.artech.base.metadata.SectionDefinition;
import com.artech.base.metadata.enums.DisplayModes;

public class FragmentFactory
{
	public static @NonNull BaseFragment newFragment(@NonNull ComponentParameters params)
	{
		if (params.Object instanceof SectionDefinition && DisplayModes.isEdit(params.Mode))
		{
			SectionDefinition section = (SectionDefinition)params.Object;
			if (section.getBusinessComponent() != null)
				return new LayoutFragmentEditBC();
		}
		
		if (params.Object instanceof IDataViewDefinition)
			return new LayoutFragment();
		
		if (params.Object instanceof DashboardMetadata)
			return new DashboardFragment();
		
		throw new IllegalArgumentException("Cannot create a Fragment for these parameters.");
	}
}
