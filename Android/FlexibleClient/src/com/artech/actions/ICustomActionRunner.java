package com.artech.actions;

import com.artech.base.metadata.ActionDefinition;
import com.artech.base.model.Entity;

/**
 * Interface that activities may implement to indicate that they want to
 * run the action themselves instead of using the default execution.
 */
public interface ICustomActionRunner
{
	/**
	 * Asks to handle the action with the specified entity as parameters.
	 * @param action Action definition.
	 * @param data Context entity.
	 * @return True if the action was handled, false otherwise (if false, "normal" execution should take over).
	 */
	boolean runAction(ActionDefinition action, Entity data);
}
