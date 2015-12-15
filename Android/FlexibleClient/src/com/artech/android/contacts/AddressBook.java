package com.artech.android.contacts;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.IOUtils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;

import com.artech.application.MyApplication;
import com.artech.base.metadata.StructureDataType;
import com.artech.base.metadata.StructureDefinition;
import com.artech.base.model.Entity;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.artech.compatibility.CompatibilityHelper;

public class AddressBook
{
	private final Context mContext;
	
	public AddressBook(Context context)
	{
		mContext = context.getApplicationContext();
	}

	private static final String TYPE_CONTACT = "SDTAddressBookContact";
	private static final String PROP_CONTACT_DISPLAY_NAME = "DisplayName";
	private static final String PROP_CONTACT_FIRST_NAME = "FirstName";
	private static final String PROP_CONTACT_LAST_NAME = "LastName";
	private static final String PROP_CONTACT_EMAIL = "EMail";
	private static final String PROP_CONTACT_PHONE = "Phone";
	private static final String PROP_CONTACT_COMPANY_NAME = "CompanyName";
	private static final String PROP_CONTACT_PHOTO = "Photo";
	private static final String PROP_CONTACT_NOTES = "Notes";

	private static final String CONTACT_PHOTOS_DIRECTORY = "contactPhotos";
	private static final String CONTACT_PHOTO_FILENAME_FORMAT = "contact_%s.photo";
	
	@SuppressLint("InlinedApi")
	public List<Entity> getAllContacts()
	{
		// We need all the contacts and some of their data (phone, email, organization, &c).
		// There are two basic ways to accomplish this:
		// 1) Query the contacts table and then the DATA table for each piece of information.
		// 2) Query the DATA table and do a control break for each contact.
		// We implement the second option, since it should be more efficient.
		ArrayList<Entity> contacts = new ArrayList<Entity>();

		// Get the list of groups against which we will check membership.  
		List<Integer> desiredGroups = getDefaultGroups();
		
		String displayNameField = (CompatibilityHelper.isHoneycomb() ? ContactsContract.Data.DISPLAY_NAME_PRIMARY : ContactsContract.Data.DISPLAY_NAME);
		String[] sortOrder = new String[] { "UPPER(" + displayNameField + ")", ContactsContract.Data.CONTACT_ID, ContactsContract.Data.MIMETYPE, 
											ContactsContract.Data.IS_SUPER_PRIMARY + " DESC", ContactsContract.Data.IS_PRIMARY + " DESC" };
		
		Cursor cursor = mContext.getContentResolver().query(ContactsContract.Data.CONTENT_URI, null, null, null, Services.Strings.join(Arrays.asList(sortOrder), ", "));
		if (cursor != null) // Fix for a bug in Android M, see https://code.google.com/p/android-developer-preview/issues/detail?id=2342
		{
			try
			{
				int currentId = -1;
				Entity current = null;
				boolean isDesired = false;

				int colContactId = cursor.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID);
				int colDisplayName = cursor.getColumnIndexOrThrow(displayNameField);
				int colDataMimeType = cursor.getColumnIndexOrThrow(ContactsContract.Data.MIMETYPE);

				while (cursor.moveToNext())
				{
					int id = cursor.getInt(colContactId);
					if (id != currentId)
					{
						// Switched to New contact.
						if (current != null && isDesired)
							contacts.add(current);

						current = newAddressBookContact();
						String displayName = cursor.getString(colDisplayName);
						setPropertyIfNeeded(current, PROP_CONTACT_DISPLAY_NAME, displayName);

						currentId = id;

						// If we have no special groups, then all contacts are "desired".
						isDesired = (desiredGroups.size() == 0);
					}

					// Read one row of contact data.
					String dataMimeType = cursor.getString(colDataMimeType);
					processDataRow(cursor, dataMimeType, current);

					// Check if this contact belongs to a desired group.
					if (!isDesired && desiredGroups.size() != 0 && dataMimeType.equalsIgnoreCase(CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE))
					{
						int groupId = getInt(cursor, CommonDataKinds.GroupMembership.GROUP_ROW_ID);
						if (desiredGroups.contains(groupId))
							isDesired = true;
					}
				}

				// Add last processed row.
				if (current != null && isDesired)
					contacts.add(current);
			}
			finally
			{
				cursor.close();
			}
		}

		return contacts;
	}

	@SuppressLint("InlinedApi")
	private void processDataRow(Cursor cursor, String dataMimeType, Entity contact)
	{
		// Check whether the data for the current row is "interesting" or not.
		// Since there may be multiple raw contacts associated to this contact, 
		// take data from the first one that has it.
		if (dataMimeType.equalsIgnoreCase(CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE))
		{
	    	String firstName = getString(cursor, CommonDataKinds.StructuredName.GIVEN_NAME);
	    	String lastName = getString(cursor, CommonDataKinds.StructuredName.FAMILY_NAME);
	    	
	    	setPropertyIfNeeded(contact, PROP_CONTACT_FIRST_NAME, firstName);
	    	setPropertyIfNeeded(contact, PROP_CONTACT_LAST_NAME, lastName);
		}
		else if (dataMimeType.equalsIgnoreCase(CommonDataKinds.Email.CONTENT_ITEM_TYPE))
		{
			String email = getString(cursor, CommonDataKinds.Email.ADDRESS);
			setPropertyIfNeeded(contact, PROP_CONTACT_EMAIL, email);
		}
		else if (dataMimeType.equalsIgnoreCase(CommonDataKinds.Phone.CONTENT_ITEM_TYPE))
		{
			String phone = getString(cursor, CommonDataKinds.Phone.NUMBER);			
			setPropertyIfNeeded(contact, PROP_CONTACT_PHONE, phone);
		}
		else if (dataMimeType.equalsIgnoreCase(CommonDataKinds.Organization.CONTENT_ITEM_TYPE))
		{
			String companyName = getString(cursor, CommonDataKinds.Organization.COMPANY);
			setPropertyIfNeeded(contact, PROP_CONTACT_COMPANY_NAME, companyName);
		}
		else if (dataMimeType.equalsIgnoreCase(CommonDataKinds.Photo.CONTENT_ITEM_TYPE))
		{
			// Read photo and move it into a file local to the app.
			// String photoUri = getString(cursor, CommonDataKinds.Photo.PHOTO_URI); This is the URI of the full photo.
			String photoFilename = extractContactPhoto(cursor);
			setPropertyIfNeeded(contact, PROP_CONTACT_PHOTO, photoFilename);
		}
		else if (dataMimeType.equalsIgnoreCase(CommonDataKinds.Note.CONTENT_ITEM_TYPE))
		{
			String notes = getString(cursor, CommonDataKinds.Note.NOTE);
			setPropertyIfNeeded(contact, PROP_CONTACT_NOTES, notes);
		}
	}
	
	private List<Integer> getDefaultGroups()
	{
		ArrayList<Integer> groupIds = new ArrayList<Integer>();
		Cursor cursor = mContext.getContentResolver().query(ContactsContract.Groups.CONTENT_URI, null, null, null, ContactsContract.Groups._ID);
		if (cursor != null)
		{
			try
			{
				while (cursor.moveToNext())
				{
					int groupId = getInt(cursor, ContactsContract.Groups._ID);
					boolean desired = false;

					// Use a heuristic for default contact groups:
					// 1) For API level >= 11 - AUTO_ADD (i.e. "My Contacts") & Favorites.
					// 2) For API level < 11 - By title ("contacts", "starred in android").
					if (CompatibilityHelper.isHoneycomb())
					{
						if (getInt(cursor, ContactsContract.Groups.AUTO_ADD) != 0 || getInt(cursor, ContactsContract.Groups.FAVORITES) != 0)
							desired = true;
					}
					else
					{
						String title = getString(cursor, ContactsContract.Groups.TITLE);
						if (Strings.hasValue(title) && (Strings.endsWithIgnoreCase(title, "my contacts") || title.equalsIgnoreCase("starred in android")))
							desired = true;
					}

					if (desired)
						groupIds.add(groupId);
				}
			}
			finally
			{
				cursor.close();
			}
		}

		return groupIds;
	}

	private String extractContactPhoto(Cursor cursor)
	{
		File cacheDir = new File(mContext.getCacheDir(), CONTACT_PHOTOS_DIRECTORY);
		cacheDir.mkdirs();
		
		// We read the thumbnail instead of the full-size photo, and extract it into a file.
		int contactId = getInt(cursor, ContactsContract.Data.CONTACT_ID);
		String filename = String.format(CONTACT_PHOTO_FILENAME_FORMAT, contactId);
		File file = new File(cacheDir, filename);

		// Check if the file hasn't been extracted recently.
		if (file.exists())
		{
			final int CONTACT_THUMBNAIL_CACHE_HOURS = 12;
			
			long msElapsed = Math.abs(new Date().getTime() - file.lastModified());
			long hoursElapsed = msElapsed / (1000 * 60 * 60);
			
			if (hoursElapsed < CONTACT_THUMBNAIL_CACHE_HOURS)
				return file.getAbsolutePath();
		}
		
		// Extract the thumbnail.
		final byte[] thumbnailBytes = cursor.getBlob(cursor.getColumnIndexOrThrow(CommonDataKinds.Photo.PHOTO));
		if (thumbnailBytes == null || thumbnailBytes.length == 0)
			return Strings.EMPTY;
		
		InputStream byteStream = new ByteArrayInputStream(thumbnailBytes);
		try
		{
			OutputStream fileStream = new FileOutputStream(file);
			try
			{
				IOUtils.copy(byteStream, fileStream);
				return file.getAbsolutePath();
			}
			finally
			{
				IOUtils.closeQuietly(fileStream);
			}
		}
		catch (IOException e)
		{
			Services.Log.Error(String.format("Error trying to extract thumbnail for contact '%s'.", contactId), e);
			return Strings.EMPTY;
		}
		finally
		{
			IOUtils.closeQuietly(byteStream);
		}
	}
	
	private static Entity newAddressBookContact()
	{
		StructureDataType sdt = MyApplication.getApp().getDefinition().getSDT(TYPE_CONTACT);
		StructureDefinition structure = (sdt != null ? sdt.getStructure() : StructureDefinition.EMPTY);
		Entity entity = new Entity(structure);
		entity.initialize();
		return entity;
	}

	private static boolean setPropertyIfNeeded(Entity item, String property, String value)
	{
		if (!Strings.hasValue(value))
			return false; // No new value provided.
		
		String oldValue = item.optStringProperty(property);
		if (Strings.hasValue(oldValue))
			return false; // Already had an old value.
			
		item.setProperty(property, value);
		return true;
	}
	
	private static String getString(Cursor cursor, String column)
	{
		return cursor.getString(cursor.getColumnIndexOrThrow(column));
	}
	
	private static int getInt(Cursor cursor, String column)
	{
		return cursor.getInt(cursor.getColumnIndexOrThrow(column));
	}
}
