package com.artech.controls;

import java.util.List;

/**
 * Interface for controls in which the UI depends on the value of other members.
 * (e.g. dynamic combos with conditions).
 * @author matiash
 *
 */
public interface IGxEditWithDependencies extends IGxEdit
{
	/**
	 * Returns the list of variables on which this edit depends.
	 */
	List<String> getDependencies();

	/**
	 * Notifies the control that some of the values on which it depends have changed.
	 */
	void onDependencyValueChanged(String name, Object value);
}
