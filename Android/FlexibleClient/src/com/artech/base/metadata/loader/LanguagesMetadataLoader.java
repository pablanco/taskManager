package com.artech.base.metadata.loader;

import com.artech.base.metadata.languages.Language;
import com.artech.base.metadata.languages.LanguageCatalog;
import com.artech.base.serialization.INodeObject;
import com.artech.base.services.IContext;

public class LanguagesMetadataLoader
{
	public static LanguageCatalog loadFrom(IContext context, INodeObject jsonLanguages)
	{
		LanguageCatalog catalog = new LanguageCatalog();
		catalog.setDefault(jsonLanguages.optString("DefaultLanguage")); //$NON-NLS-1$

		for (INodeObject jsonLanguage : jsonLanguages.optCollection("Languages")) //$NON-NLS-1$
		{
			String name = jsonLanguage.getString("Name"); //$NON-NLS-1$
			String languageCode = jsonLanguage.optString("LanguageCode"); //$NON-NLS-1$
			String countryCode = jsonLanguage.optString("CountryCode"); //$NON-NLS-1$

			// TODO: Download all languages, but only deserialize the current one.
			// (Others will be deserialized later, if needed).
			INodeObject jsonLanguageFile = MetadataLoader.getDefinition(context, name + ".language"); //$NON-NLS-1$
			if (jsonLanguageFile != null)
			{
				Language language = new Language(name, languageCode, countryCode);
				loadLanguage(language, jsonLanguageFile, jsonLanguage.optNode("properties")); //$NON-NLS-1$
				catalog.add(language);
			}
		}

		return catalog;
	}

	private static void loadLanguage(Language language, INodeObject jsonLanguage, INodeObject jsonProperties)
	{
		if (jsonProperties != null)
		{
			for (String propKey : jsonProperties.names())
			{
				String propValue = jsonProperties.getString(propKey);
				language.getProperties().put(propKey, propValue);
			}
		}

		for (INodeObject jsonTranslation : jsonLanguage.optCollection("Translations")) //$NON-NLS-1$
		{
			String message = jsonTranslation.getString("M"); //$NON-NLS-1$
			String translation = jsonTranslation.getString("T"); //$NON-NLS-1$
			language.add(message, translation);
		}
	}
}
