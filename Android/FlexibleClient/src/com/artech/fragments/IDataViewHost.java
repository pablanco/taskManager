package com.artech.fragments;

import com.artech.app.ComponentId;
import com.artech.app.ComponentParameters;
import com.artech.app.ComponentUISettings;
import com.artech.base.metadata.ILayoutDefinition;

public interface IDataViewHost
{
	ILayoutDefinition getMainLayout();

	/**
	 * Creates and initializes a Fragment based on the supplied component data. The caller is responsible
	 * for adding it as a Fragment to the Activity later.
	 * @param id Component id. Should be unique for each component in an Activity.
	 * @param parameters Component parameters (such as the object to be instantiated, its parameter values, &c).
	 * @param uiSettings Component UI settings (such as its desired size, parent component, &c).
	 * @return An initialized Fragment.
	 */
	BaseFragment createComponent(ComponentId id, ComponentParameters parameters, ComponentUISettings uiSettings);

	/**
	 * Releases all resources associated to a particular component fragment.
	 * @param fragment Fragment to be released. It should already be detached from the FragmentManager.
	 */
	void destroyComponent(BaseFragment fragment);
}
