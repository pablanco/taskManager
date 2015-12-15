package com.artech.base.metadata.expressions;

import com.artech.android.layout.ControlHelper;
import com.artech.base.metadata.StructureDataType;
import com.artech.base.model.Entity;
import com.artech.base.model.EntityList;
import com.artech.base.serialization.INodeObject;
import com.artech.base.utils.Strings;
import com.artech.controls.IGxControl;
import com.artech.utils.Cast;

class PropertyExpression extends Expression implements IAssignableExpression, ITargetedExpression
{
	static final String TYPE = "property";

	private Expression mTarget;
	private String mProperty;
	private Type mType;

	public PropertyExpression(INodeObject node)
	{
		mTarget = ExpressionFactory.parse(node.getNode("target"));
		mProperty = node.getString("@propName");

		// Control properties do not have a data type, but SDT properties do.
		String exprDataType = node.optString("@exprDataType");
		if (Strings.hasValue(exprDataType))
			mType = ExpressionFactory.parseGxDataType(node.optString("@exprDataType"));
		else
			mType = Type.UNKNOWN;
	}

	@Override
	public String toString()
	{
		return String.format("%s.%s", mTarget, mProperty);
	}

	@Override
	public Value eval(IExpressionContext context)
	{
		Value target = mTarget.eval(context);

		if (target.getType() == Type.CONTROL)
		{
			Object controlPropertyValue = ControlHelper.getProperty(context.getExecutionContext(), (IGxControl)target.getValue(), mProperty);
			if (controlPropertyValue != null)
				return Value.newValue(controlPropertyValue);
			else
				return new Value(Type.UNKNOWN, null);
		}

		if (target.getType() == Type.ENTITY_COLLECTION)
		{
			EntityList collection = target.coerceToEntityCollection();
			if (mProperty.equalsIgnoreCase("Count"))
				return Value.newInteger(collection.size());
			else if (mProperty.equalsIgnoreCase(StructureDataType.COLLECTION_PROPERTY_CURRENT_ITEM))
				return Value.newEntity(collection.getCurrentEntity());
		}

		if (target.getType() == Type.ENTITY || target.getType() == Type.ENTITY_COLLECTION)
		{
			// The second part of this condition shouldn't be necessary.
			// However, in entity deserialization we are assuming that ALL levels are collections.
			// It's that way for BCs, but not necessarily for SDTs.
			Entity entity = target.coerceToEntity();

			// Check for sublevel first
			// TODO: This should not exist, but getProperty() doesn't return sublevels.
			EntityList subLevel = entity.getLevel(mProperty);
			if (subLevel != null)
				return Value.newEntityCollection(subLevel);

			// Generic property of entity.
			return ExpressionValueBridge.convertEntityFormatToValue(entity, mProperty, mType);
		}

		throw new IllegalArgumentException(String.format("Unknown property ('%s').", toString()));
	}

	@Override
	public Expression getTarget()
	{
		return mTarget;
	}

	@Override
	public boolean setValue(IExpressionContext context, Object value)
	{
		Value target = mTarget.eval(context);

		if (target.getType() == Type.ENTITY_COLLECTION)
		{
			// Handle special case: setting &SDTCollection.CurrentItem = &SDTItem.
			EntityList collection = target.coerceToEntityCollection();
			if (mProperty.equalsIgnoreCase(StructureDataType.COLLECTION_PROPERTY_CURRENT_ITEM))
			{
				Entity setCurrentItem = Cast.as(Entity.class, value);
				if (setCurrentItem != null && collection.contains(setCurrentItem))
				{
					collection.setCurrentEntity(setCurrentItem);
					return true;
				}
			}
		}

		if (target.getType() == Type.ENTITY || target.getType() == Type.ENTITY_COLLECTION)
		{
			Entity entity = target.coerceToEntity();
			return entity.setProperty(mProperty, value);
		}

		return false;
	}

	@Override
	public String getRootName()
	{
		return getRootName(this);
	}

	private static String getRootName(Expression expression)
	{
		if (expression instanceof ValueExpression)
			return ((ValueExpression)expression).getName();

		if (expression instanceof ITargetedExpression)
			return getRootName(((ITargetedExpression)expression).getTarget());

		return null;
	}
}
