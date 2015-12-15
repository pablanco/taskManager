package com.artech.android.api;

import java.util.Arrays;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.MediaStore;

/**
 * Custom FileProvider to work around a bug in the official Twitter app
 * (expects a "_DATA" column to be always present -- having it return null is enough).
 * See http://stackoverflow.com/a/25020642/82788
 *
 * @author matiash
 */
public class SharingApiFileProvider extends android.support.v4.content.FileProvider
{
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
    {
        Cursor source = super.query(uri, projection, selection, selectionArgs, sortOrder);

        String[] columnNames = source.getColumnNames();
        String[] newColumnNames = columnNamesWithData(columnNames);
        if (columnNames.length == newColumnNames.length)
        	return source;

        MatrixCursor cursor = new MatrixCursor(newColumnNames, source.getCount());

        source.moveToPosition(-1);
        while (source.moveToNext())
        {
            MatrixCursor.RowBuilder row = cursor.newRow();
            for (int i = 0; i < columnNames.length; i++)
                row.add(source.getString(i));
        }

        return cursor;
    }

    private String[] columnNamesWithData(String[] columnNames)
    {
        for (String columnName : columnNames)
        {
            if (MediaStore.MediaColumns.DATA.equals(columnName))
                return columnNames;
        }

        String[] newColumnNames = Arrays.copyOf(columnNames, columnNames.length + 1);
        newColumnNames[columnNames.length] = MediaStore.MediaColumns.DATA;
        return newColumnNames;
    }
}