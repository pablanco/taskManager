package com.artech.base.metadata.expressions;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

import com.artech.base.metadata.DataItem;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.model.Entity;
import com.artech.base.model.EntityList;
import com.artech.base.utils.Strings;
import com.genexus.GXutil;

public abstract class Expression
{
	public abstract Value eval(IExpressionContext context);

	public enum Type
	{
		UNKNOWN,
		STRING,
		INTEGER,
		DECIMAL,
		BOOLEAN,
		DATE,
		DATETIME,
		TIME,
		GUID,
		CONTROL,
		ENTITY,
		ENTITY_COLLECTION;

		public boolean isNumeric()
		{
			return (this == DECIMAL || this == INTEGER);
		}

		public boolean isDateTime()
		{
			return (this == DATE || this == DATETIME || this == TIME);
		}

		public boolean isSimple()
		{
			return (this == STRING || this == INTEGER || this == DECIMAL || this == BOOLEAN ||
					this == DATE || this == DATETIME || this == TIME || this == GUID);
		}
	}

	public static class Value
	{
		private final Object mValue;
		private final Type mType;

		private DataItem mDefinition;
		private String mPicture;

		public Value(Type type, Object value)
		{
			mType = type;
			mValue = value;
		}

		public static Value newValue(Object value)
		{
			if (value instanceof String)
				return newString((String)value);
			else if (value instanceof Integer)
				return newInteger((Integer)value);
			else if (value instanceof BigDecimal)
				return newDecimal((BigDecimal)value);
			else if (value instanceof Boolean)
				return newBoolean((Boolean)value);
			else if (value instanceof ThemeClassDefinition)
				return newString(((ThemeClassDefinition)value).getName()); // Because theme classes can be compared by equality on their names.
			else
				throw new IllegalArgumentException(String.format("Could not guess value type for '%s'.", value));
		}

		public static Value newString(String value)
		{
			if (value == null)
				value = Strings.EMPTY;

			return new Value(Type.STRING, value);
		}

		public static Value newInteger(long value)
		{
			return new Value(Type.INTEGER, new BigDecimal(value));
		}

		public static Value newDecimal(BigDecimal value)
		{
			return new Value(Type.DECIMAL, value);
		}

		public static Value newBoolean(boolean value)
		{
			return new Value(Type.BOOLEAN, value);
		}

		public static Value newEntity(Entity value)
		{
			return new Value(Type.ENTITY, value);
		}

		public static Value newEntityCollection(EntityList value)
		{
			return new Value(Type.ENTITY_COLLECTION, value);
		}

		public static Value newGuid(UUID value)
		{
			return new Value(Type.GUID, value);
		}

		@Override
		public String toString()
		{
			return String.format("[%s: %s]", mType, mValue);
		}

		public DataItem getDefinition()
		{
			return mDefinition;
		}

		public String getPicture()
		{
			if (!Strings.hasValue(mPicture))
				mPicture = ExpressionFormatHelper.getDefaultPicture(this);

			return mPicture;
		}

		public void setDefinition(DataItem definition)
		{
			mDefinition = definition;
			mPicture = definition.getInputPicture();
		}

		public void setPicture(String picture)
		{
			mPicture = picture;
		}

		public String coerceToString()
		{
			return mValue.toString();
		}

		public BigDecimal coerceToNumber()
		{
			BigDecimal value = (BigDecimal)mValue;
			if (mType == Type.INTEGER)
				return new BigDecimal(value.longValue());
			else
				return value;
		}

		public int coerceToInt()
		{
			return ((BigDecimal)mValue).intValue();
		}

		public Boolean coerceToBoolean()
		{
			return (Boolean)mValue;
		}

		public Date coerceToDate()
		{
			Date value = (Date)mValue;
			if (mType == Type.DATE)
				return GXutil.resetTime(value);
			else if (mType == Type.TIME)
				return GXutil.resetDate(value);
			else
				return value;
		}

		public UUID coerceToGuid()
		{
			return (UUID)mValue;
		}

		public Entity coerceToEntity()
		{
			if (mValue instanceof EntityList && ((EntityList)mValue).size() != 0)
				return ((EntityList)mValue).get(0);

			return (Entity)mValue;
		}

		public EntityList coerceToEntityCollection()
		{
			return (EntityList)mValue;
		}

		public Type getType()
		{
			return mType;
		}

		public Object getValue()
		{
			return mValue;
		}

		public final static Value UNKNOWN = new Value(Type.UNKNOWN, null);
	}
}
