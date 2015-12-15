package com.artech.base.metadata.loader;

import com.artech.base.metadata.images.ImageCatalog;
import com.artech.base.metadata.images.ImageCollection;
import com.artech.base.metadata.images.ImageFile;
import com.artech.base.serialization.INodeObject;
import com.artech.base.services.IContext;
import com.artech.base.utils.PlatformHelper;

class ImagesMetadataLoader
{
	public static ImageCatalog loadFrom(IContext context, INodeObject jsonResources)
	{
		ImageCatalog catalog = new ImageCatalog();
		
		String defaultLanguage = jsonResources.optString("DefaultLanguage"); //$NON-NLS-1$
		catalog.setDefaultLanguage(defaultLanguage);
		
		for (INodeObject jsonFileReference : jsonResources.optCollection("Resources")) //$NON-NLS-1$
		{
			boolean isDefault = jsonFileReference.optBoolean("IsDefault"); //$NON-NLS-1$
			String theme = jsonFileReference.optString("Theme"); //$NON-NLS-1$
			String file = jsonFileReference.optString("ResourceFile"); //$NON-NLS-1$

			// Read only the theme that is the current for the application.
			// However, read all languages, since language may change dynamically.
			if (isDefault || (PlatformHelper.getTheme() != null && PlatformHelper.getTheme().getName().equalsIgnoreCase(theme)))
			{
				// Remove file extension because MetadataLoader adds it.
				if (file != null && file.endsWith(".json")) //$NON-NLS-1$
					file = file.substring(0, file.length() - 5);
				
				INodeObject jsonResourceFile = MetadataLoader.getDefinition(context, file);
				if (jsonResourceFile != null)
					loadResourceFile(catalog, jsonResourceFile, isDefault);
			}
		}
		
		return catalog;
	}

	private static void loadResourceFile(ImageCatalog catalog, INodeObject jsonResources, boolean isDefault)
	{
		String baseDirectory = jsonResources.optString("ResourcesLocation"); //$NON-NLS-1$
		String theme = jsonResources.optString("Theme"); //$NON-NLS-1$
		String language = jsonResources.optString("Language"); //$NON-NLS-1$
		
		ImageCollection imageCollection = new ImageCollection(language, theme, isDefault, baseDirectory);
		catalog.add(imageCollection);
		
		for (INodeObject jsonImage : jsonResources.optCollection("Images")) //$NON-NLS-1$
		{
			String name = jsonImage.optString("Name"); //$NON-NLS-1$
			String strType = jsonImage.optString("Type"); //$NON-NLS-1$
			int type = (strType != null && strType.equals("E") ? ImageFile.TYPE_EXTERNAL : ImageFile.TYPE_INTERNAL); //$NON-NLS-1$
			String location = jsonImage.optString("Location"); //$NON-NLS-1$

			ImageFile imageFile = new ImageFile(imageCollection, name, type, location);
			imageCollection.add(imageFile);
		}
	}
}