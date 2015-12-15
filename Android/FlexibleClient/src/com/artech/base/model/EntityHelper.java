package com.artech.base.model;

public class EntityHelper
{
	/**
	 * Gets the entity from which a code expression should be evaluated (e.g. the same entity
	 * when it's provided by a DP, or the root entity when it's part of an SDT structure).
	 */
	public static Entity forEvaluation(Entity entity)
	{
		// Get the "TRUE" root entity for executing the action.
		// For a normal Entity (e.g. the Form entity, or an entity in a grid row with a DP) it's the same one.
		// For a "member" entity (e.g. an SDT variable or an SDT collection item) it's the first parent entity
		// that is not a member itself (i.e. one of the "normal" cases outlined above).
		while (entity != null && entity.getParentInfo().isMember())
		{
			if (entity.getParentInfo().getParentCollection() != null)
				entity.getParentInfo().getParentCollection().setCurrentEntity(entity);

			entity = entity.getParentInfo().getParent();
		}

		return entity;
	}
}
