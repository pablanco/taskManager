package com.artech.providers;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Pair;

import com.artech.android.json.NodeCollection;
import com.artech.android.json.NodeObject;
import com.artech.base.model.Entity;
import com.artech.base.model.EntityList;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;

class EntityDatabaseHelper extends SQLiteOpenHelper
{
	private final DatabaseDefinition mDefinition;
	private static final String LOG_TAG = "EntityDatabase";

	public EntityDatabaseHelper(Context context, DatabaseDefinition definition)
	{
		super(context, definition.getName(), null, definition.getVersion());
		mDefinition = definition;
	}

	@Override
	public void onCreate(SQLiteDatabase db)
	{
		Services.Log.info(LOG_TAG, "Creating local database...");

		// Try to drop known tables first.
		// Necessary because if an exception should occur while creating the DB, the version
		// will be 0 and this method (not onUpgrade) will be called on next attempt.
		dropAllTables(db);

		// Create the tables to identify queries.
		for (String createStmt : createMetadataTablesStmts())
			db.execSQL(createStmt);

		for (TableDefinition table : mDefinition.getTables())
		{
			String createStmt = createTableStmt(table);
			db.execSQL(createStmt);
		}
	}

	static List<String> metadataTableNames()
	{
		List<String> tableNames = new LinkedList<String>();
		tableNames.add(EntityDatabase.QUERY.TABLE);
		tableNames.add(EntityDatabase.QUERY_ENTITY.TABLE);
		tableNames.add(EntityDatabase.OPERATION.TABLE);
		return tableNames;
	}

	private static List<String> createMetadataTablesStmts()
	{
		List<String> statements = new LinkedList<String>();

		// Table for Queries. ROWID is query id.
		LinkedList<Pair<String, Integer>> queryColumns = new LinkedList<Pair<String, Integer>>();
		queryColumns.add(new Pair<String, Integer>(EntityDatabase.QUERY.COL_BASE_URI, ColumnDefinition.TYPE_CHARACTER));
		queryColumns.add(new Pair<String, Integer>(EntityDatabase.QUERY.COL_PARAMETERS, ColumnDefinition.TYPE_CHARACTER));
		queryColumns.add(new Pair<String, Integer>(EntityDatabase.QUERY.COL_SERVER_TIMESTAMP, ColumnDefinition.TYPE_DATETIME));
		queryColumns.add(new Pair<String, Integer>(EntityDatabase.QUERY.COL_CLIENT_TIMESTAMP, ColumnDefinition.TYPE_DATETIME));
		queryColumns.add(new Pair<String, Integer>(EntityDatabase.QUERY.COL_IS_COMPLETE, ColumnDefinition.TYPE_BOOLEAN));
		queryColumns.add(new Pair<String, Integer>(EntityDatabase.QUERY.COL_ROW_COUNT, ColumnDefinition.TYPE_INTEGER));
		queryColumns.add(new Pair<String, Integer>(EntityDatabase.QUERY.COL_HASH, ColumnDefinition.TYPE_CHARACTER));

		LinkedList<String> queryKey = new LinkedList<String>();
		queryKey.add(EntityDatabase.QUERY.COL_BASE_URI);
		queryKey.add(EntityDatabase.QUERY.COL_PARAMETERS);

		statements.add(createTableStmt(EntityDatabase.QUERY.TABLE, queryColumns, queryKey));

		// Table for Query-entity relationship.
		LinkedList<Pair<String, Integer>> queryEntitiesColumns = new LinkedList<Pair<String, Integer>>();
		queryEntitiesColumns.add(new Pair<String, Integer>(EntityDatabase.QUERY_ENTITY.COL_QUERY_ID, ColumnDefinition.TYPE_INTEGER));
		queryEntitiesColumns.add(new Pair<String, Integer>(EntityDatabase.QUERY_ENTITY.COL_ENTITY_ID, ColumnDefinition.TYPE_INTEGER));

		LinkedList<String> queryEntitiesKey = new LinkedList<String>();
		queryEntitiesKey.add(EntityDatabase.QUERY_ENTITY.COL_QUERY_ID);
		queryEntitiesKey.add(EntityDatabase.QUERY_ENTITY.COL_ENTITY_ID);

		statements.add(createTableStmt(EntityDatabase.QUERY_ENTITY.TABLE, queryEntitiesColumns, queryEntitiesKey));

		// Table for OPERATIONS.
		LinkedList<Pair<String, Integer>> operationsColumns = new LinkedList<Pair<String, Integer>>();
		operationsColumns.add(new Pair<String, Integer>(EntityDatabase.OPERATION.COL_URI, ColumnDefinition.TYPE_CHARACTER));
		operationsColumns.add(new Pair<String, Integer>(EntityDatabase.OPERATION.COL_TYPE, ColumnDefinition.TYPE_INTEGER));
		operationsColumns.add(new Pair<String, Integer>(EntityDatabase.OPERATION.COL_DATA, ColumnDefinition.TYPE_CHARACTER));
		operationsColumns.add(new Pair<String, Integer>(EntityDatabase.OPERATION.COL_DATAKEY, ColumnDefinition.TYPE_CHARACTER));
		operationsColumns.add(new Pair<String, Integer>(EntityDatabase.OPERATION.COL_STATUS, ColumnDefinition.TYPE_INTEGER));
		operationsColumns.add(new Pair<String, Integer>(EntityDatabase.OPERATION.COL_OPERATION_TIMESTAMP, ColumnDefinition.TYPE_DATETIME));
		operationsColumns.add(new Pair<String, Integer>(EntityDatabase.OPERATION.COL_STATUS_TIMESTAMP, ColumnDefinition.TYPE_DATETIME));

		statements.add(createTableStmt(EntityDatabase.OPERATION.TABLE, operationsColumns, null));

		return statements;
	}

	private static String createTableStmt(TableDefinition table)
	{
		LinkedList<Pair<String, Integer>> columns = new LinkedList<Pair<String, Integer>>();

		for (ColumnDefinition column : table.getColumns())
			columns.add(new Pair<String, Integer>(column.getSqlName(), column.getType()));

		// Some DPs have no members, but a table with no fields is not valid, so create a dummy field.
		if (columns.size() == 0)
			columns.add(new Pair<String, Integer>(EntityDatabase.ENTITY.COL_DUMMY, ColumnDefinition.TYPE_INTEGER));

		// Redundant column for EntityKey.
		// columns.add(0, new Pair<String, Integer>(ENTITY.COL_ENTITY_KEY, ColumnDefinition.TYPE_CHARACTER));

		LinkedList<String> pk = new LinkedList<String>();
		for (ColumnDefinition column : table.getKey())
			pk.add(column.getSqlName());

		// Some columns "should" be considered part of the primary key because their data may be different for different queries.
		if (table.getExtraKeyHashColumns().size() != 0)
		{
			columns.add(new Pair<String, Integer>(EntityDatabase.ENTITY.COL_EXTRA_KEY_HASH, ColumnDefinition.TYPE_CHARACTER));
			pk.add(EntityDatabase.ENTITY.COL_EXTRA_KEY_HASH);
		}

		return createTableStmt(table.getSqlName(), columns, pk);
	}

	private static String createTableStmt(String tableName, List<Pair<String, Integer>> columns, List<String> pk)
	{
		StringBuilder createStmt = new StringBuilder();
		createStmt.append("CREATE TABLE "); //$NON-NLS-1$
		createStmt.append(tableName);
		createStmt.append(" ("); //$NON-NLS-1$

		List<String> columnsWithTypes = new LinkedList<String>();
		for (Pair<String, Integer> column : columns)
			columnsWithTypes.add(column.first + Strings.SPACE + sqlType(column.second));

		createStmt.append(Services.Strings.join(columnsWithTypes, ", ")); //$NON-NLS-1$

		if (pk != null && pk.size() != 0)
		{
			createStmt.append(", PRIMARY KEY ("); //$NON-NLS-1$
			createStmt.append(Services.Strings.join(pk, ", ")); //$NON-NLS-1$
			createStmt.append(")"); //$NON-NLS-1$
		}

		createStmt.append(")"); //$NON-NLS-1$
		return createStmt.toString();
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		// Same as create, for now.
		Services.Log.info(LOG_TAG, "Erasing local database...");
		dropAllTables(db);
		onCreate(db);
	}

	@Override
	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		// Same as create, for now.
		Services.Log.info(LOG_TAG, "Erasing local database...");
		dropAllTables(db);
		onCreate(db);
	}

	private static void dropAllTables(SQLiteDatabase db)
	{
		List<String> tableNames = new LinkedList<String>();
		String[] columns = {"name"}; //$NON-NLS-1$

		// Get the names of all tables from the catalog.
		Cursor allTables = db.query("sqlite_master", columns, "type = 'table'", null, null, null, "name"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		try
		{
			while (allTables.moveToNext())
			{
				String tableName = allTables.getString(0);
				if (!tableName.equalsIgnoreCase("android_metadata")) //$NON-NLS-1$
					tableNames.add(tableName);
			}
		}
		finally
		{
			allTables.close();
		}

		for (String tableName : tableNames)
			db.execSQL("DROP TABLE " + sqlName(tableName)); //$NON-NLS-1$
	}

	static String sqlName(String name)
	{
		return "[" + name + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static String sqlType(int type)
	{
		switch (type)
		{
			case ColumnDefinition.TYPE_INTEGER :
			case ColumnDefinition.TYPE_DATETIME : // SQLite has no specific DateTime (http://www.sqlite.org/datatype3.html#datetime)
			case ColumnDefinition.TYPE_BOOLEAN : // SQLite has no specific Boolean (http://www.sqlite.org/datatype3.html#boolean)
				return "INT"; //$NON-NLS-1$

			case ColumnDefinition.TYPE_CHARACTER :
			case ColumnDefinition.TYPE_URI : // Images/Audio/Video are stored with their URL.
			case ColumnDefinition.TYPE_STRUCTURED : // SDTs are stored as their JSON.
				return "CHARACTER"; //$NON-NLS-1$

			case ColumnDefinition.TYPE_FLOAT : // SQLite only has a 15-digit precision, whereas a GX numeric can have more. Therefore don't use conversion and store as text.
				return "CHARACTER"; //$NON-NLS-1$

			case ColumnDefinition.TYPE_BLOB:
				return "BLOB"; //$NON-NLS-1$

			default :
				throw new IllegalArgumentException(String.format("Unexpected type: %s", type)); //$NON-NLS-1$
		}
	}

	/**
	 * Helper method to put entity values in a ContentValues bag.
	 * Necessary since ContentValues doesn't support put(String, Object).
	 * @param entity Entity to take values from.
	 * @param columns Columns to add.
	 * @return ContentValues filled with values.
	 */
	static ContentValues toValues(Entity entity, Collection<ColumnDefinition> columns)
	{
		ContentValues entityValues = new ContentValues(columns.size());
		for (ColumnDefinition column : columns)
			putValue(entity, column, entityValues);

		return entityValues;
	}

	/**
	 * Helper method to put all entity values in a ContentValues bag.
	 * Necessary since ContentValues doesn't support put(String, Object).
	 * If applicable, the "extra key hash" will be added too.
	 * @param entity Entity to take values from.
	 * @param table Table with the columns to add.
	 * @return ContentValues filled with values.
	 */
	static ContentValues toValues(Entity entity, TableDefinition table)
	{
		// Add the standard columns.
		ContentValues entityValues = toValues(entity, table.getColumns());

		// Add the extra key hash value, if applicable.
		if (table.getExtraKeyHashColumns().size() != 0)
			entityValues.put(EntityDatabase.ENTITY.COL_EXTRA_KEY_HASH, getExtraKeyHash(table, entity));

		return entityValues;
	}

	/**
	 * Helper method to put an entity value in a ContentValues bag.
	 * @param entity Entity to take value from.
	 * @param column Specifies the value to put in the bag.
	 * @param inValues Bag to place value.
	 */
	private static void putValue(Entity entity, ColumnDefinition column, ContentValues inValues)
	{
		String key = column.getSqlName();
		String value = serializeValue(entity, column);

		if (value != null)
		{
			switch (column.getType())
			{
				case ColumnDefinition.TYPE_INTEGER :
					inValues.put(key, parseIntFrom(value));
					break;

				case ColumnDefinition.TYPE_CHARACTER :
					inValues.put(key, value);
					break;

				case ColumnDefinition.TYPE_FLOAT :
					// Floating point types are not converted from string to avoid possible precision loss.
					inValues.put(key, value);
					break;

				case ColumnDefinition.TYPE_DATETIME :
					// DATETIME is stored as "YYYY-MM-DD HH:MM:SS.SSS", same as server. So no conversion needed.
					inValues.put(key, value);
					break;

				case ColumnDefinition.TYPE_BOOLEAN :
					// Booleans are mapped to 0/1 integer, but are strings in Entity.
					boolean bValue = value.equalsIgnoreCase("true"); //$NON-NLS-1$
					inValues.put(key, bValue);

				case ColumnDefinition.TYPE_URI :
					// Images/Audio/Video are stored as their Uri. There is an external file cache for them.
					inValues.put(key, value);
					break;

				case ColumnDefinition.TYPE_STRUCTURED :
					// Structured type variables have already been converter by serializeValue().
					inValues.put(key, value);
					break;

				case ColumnDefinition.TYPE_BLOB :
					// TODO: Where should blobs be stored?
					inValues.putNull(key);
					break;

				default :
					throw new IllegalArgumentException(String.format("Unexpected type in putValue %s (%s)", column.getName(), column.getType())); //$NON-NLS-1$
			}
		}
		else
			inValues.putNull(key);
	}

	private static Long parseIntFrom(String value)
	{
		// Value is trimmed because server is returning left spaces (e.g. "   4") as of 2012/03/01.
		value = value.trim();

		if (!Strings.hasValue(value))
			return 0l;
		
		try
		{
			return Long.parseLong(value);
		}
		catch (NumberFormatException e1)
		{
			// Value may actually have decimals, because of a Ruby bug. Try to work around that.
			try
			{
				return new BigDecimal(value).longValue();
			}
			catch (NumberFormatException e2)
			{
				Services.Log.Error(LOG_TAG, "putValueInt", e1); //$NON-NLS-1$
				return null;
			}
		}
	}

	private static String serializeValue(Entity entity, ColumnDefinition column)
	{
		Object value = entity.getProperty(column.getName());

		if (value == null)
			return null;

		// Handle structured type differently because they are not Strings inside an Entity.
		if (column.getType() == ColumnDefinition.TYPE_STRUCTURED)
		{
			// Two cases: EntityList (for a collection SDT) or Entity (for a non-collection SDT).
			// Use a wrapper to ease deserialization later.
			if (value instanceof EntityList)
			{
				NodeCollection serialized = (NodeCollection)((EntityList)value).serialize();
				return serialized.toString();
			}
			else if (value instanceof Entity)
			{
				NodeObject serialized = (NodeObject)((Entity)value).serialize();
				return serialized.toString();
			}
		}

		// Either an atomic type, or an SDT type that was not deserialized.
		return value.toString();
	}

	/**
	 * Helper method to read Entity field values from a database result.
	 * @param source Database cursor.
	 * @param column Specifies the value to read from the cursor.
	 * @param entity Destination entity.
	 */
	static void readValue(Cursor source, ColumnDefinition column, Entity entity)
	{
		int columnIndex = source.getColumnIndexOrThrow(column.getName());
		Object value = source.getString(columnIndex);
		entity.deserializeValue(column.getName(), value);
	}

	/**
	 * Extracts the names from a collection of columns, returning them as a string array.
	 * Useful for getting the selection fields for a query.
	 */
	static String[] columnNames(Collection<ColumnDefinition> columns)
	{
		return columnNames(null, columns);
	}

	/**
	 * Extracts the names from a collection of columns, returning them as a string array.
	 * Useful for getting the selection fields for a query.
	 */
	static String[] columnNames(String tableAlias, Collection<ColumnDefinition> columns)
	{
		int i = 0;
		String[] names = new String[columns.size()];
		for (ColumnDefinition column : columns)
			names[i++] = (tableAlias != null ? tableAlias + Strings.DOT : Strings.EMPTY) + column.getSqlName();

		return names;
	}

	/**
	 * Builds a condition (of the form COL1 = ? and COL2 = ? AND ... AND COLn = ?)
	 * for each column forming part of the key of the table. The "extra key hash" column
	 * is included if the table has one.
	 *
	 * @param entity Entity to take values from.
	 * @param table Table definition.
	 * @return A pair containing the WHERE condition and its arguments.
	 */
	static Pair<String, String[]> keyToWhereCondition(Entity entity, TableDefinition table)
	{
		List<Pair<String, String>> extraKeyHashConditions = null;

		if (table.getExtraKeyHashColumns().size() != 0)
		{
			String extraKeyHash = getExtraKeyHash(table, entity);
			extraKeyHashConditions = new LinkedList<Pair<String,String>>();
			extraKeyHashConditions.add(Pair.create(EntityDatabase.ENTITY.COL_EXTRA_KEY_HASH, extraKeyHash));
		}

		return toWhereCondition(entity, table.getKey(), extraKeyHashConditions);
	}

	/**
	 * Builds a condition (of the form COL1 = ? and COL2 = ? AND ... AND COLn = ?)
	 * for each column of the collection (plus any extra values, if supplied),
	 * and converts the corresponding values in the entity to a String array.
	 *
	 * @param entity Entity to take values from.
	 * @param columns Columns to use in condition text and values.
	 * @param extraConditions Pairs of [name, value] to be added to the condition.
	 * @return A pair containing the WHERE condition and its arguments.
	 */
	private static Pair<String, String[]> toWhereCondition(Entity entity, Collection<ColumnDefinition> columns, List<Pair<String, String>> extraConditions)
	{
		List<Pair<String, String>> conditions = new LinkedList<Pair<String,String>>();

		// Add name=value from columns.
		for (ColumnDefinition column : columns)
		{
			String name = column.getSqlName();
			String value = valueToWhereArgument(entity, column);
			conditions.add(Pair.create(name, value));
		}

		// Add extra conditions.
		if (extraConditions != null)
			conditions.addAll(extraConditions);

		return toWhereCondition(conditions);
	}

	/**
	 * Builds a condition (of the form COL1 = ? and COL2 = ? AND ... AND COLn = ?)
	 * for each name in the supplied condition collection and converts their
	 * corresponding values to a String array.
	 *
	 * @param conditions Pairs of [name, value] that will make up the condition.
	 * @return A pair containing the WHERE condition and its arguments.
	 */
	private static Pair<String, String[]> toWhereCondition(List<Pair<String, String>> conditions)
	{
		StringBuilder whereCondition = new StringBuilder();
		String[] whereArgs = new String[conditions.size()];

		int i = 0;
		for (Pair<String, String> condition : conditions)
		{
			if (i != 0)
				whereCondition.append(" AND "); //$NON-NLS-1$

			whereCondition.append(condition.first);
			String whereValue = condition.second;

			if (whereValue != null)
			{
				whereCondition.append(" = ?"); //$NON-NLS-1$
				whereArgs[i++] = whereValue;
			}
			else
				whereCondition.append(" IS NULL"); //$NON-NLS-1$
		}

		return new Pair<String, String[]>(whereCondition.toString(), whereArgs);
	}

	/**
	 * Gets the hash value associated to a specific record, according the columns that
	 * the table defines as being part of the 'extra key hash' set.
	 */
	private static String getExtraKeyHash(TableDefinition table, Entity entity)
	{
		if (table.getExtraKeyHashColumns().size() == 0)
			return null;

		StringBuilder extraKeyValue = new StringBuilder();
		for (ColumnDefinition column : table.getExtraKeyHashColumns())
		{
			extraKeyValue.append(column.getSqlName());
			extraKeyValue.append("=");
			extraKeyValue.append(serializeValue(entity, column));
			extraKeyValue.append(";");
		}

		return Services.Strings.getStringHash(extraKeyValue.toString());
	}

	/**
	 * Helper function to work around server bug: Always ensure that records have values
	 * for their key attributes.
	 * @param entity Entity to take values from.
	 * @param table Table to get primary key definition.
	 */
	static void ensureHasKey(Entity entity, TableDefinition table)
	{
		for (ColumnDefinition column : table.getKey())
			if (entity.getProperty(column.getName()) == null)
				entity.setProperty(column.getName(), Strings.EMPTY);
	}

	private static String valueToWhereArgument(Entity entity, ColumnDefinition column)
	{
		Object value = entity.getProperty(column.getName());
		if (value != null)
		{
			String strValue = value.toString();
			switch (column.getType())
			{
				case ColumnDefinition.TYPE_INTEGER:
				case ColumnDefinition.TYPE_CHARACTER:
				case ColumnDefinition.TYPE_FLOAT:
				case ColumnDefinition.TYPE_DATETIME:
				case ColumnDefinition.TYPE_URI:
					// Since entity is not strong typed, these types are already there as strings.
					return strValue;

				case ColumnDefinition.TYPE_BOOLEAN:
					boolean bValue = (strValue != null && strValue.equalsIgnoreCase("true")); //$NON-NLS-1$
					return (bValue ? Strings.ONE : Strings.ZERO);

				default : // or BLOB
					throw new IllegalArgumentException(String.format("Unexpected type in WHERE argument %s (%s)", column.getName(), column.getType())); //$NON-NLS-1$
			}
		}
		else
		{
			return null;
		}
	}
}
