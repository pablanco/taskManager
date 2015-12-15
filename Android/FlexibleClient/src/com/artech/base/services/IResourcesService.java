package com.artech.base.services;

import com.artech.base.metadata.images.ImageCatalog;
import com.artech.base.metadata.languages.LanguageCatalog;

public interface IResourcesService
{
	void initialize(LanguageCatalog languages, ImageCatalog images);

	int getImageResourceId(String imageName);
	String getImageUri(String imageName);

	/**
	 * Returns the current (GX) language name, calculated from the current device locale. May be null.
	 */
	String getCurrentLanguage();

	/**
	 * Returns the properties of the current (GX) language.
	 */
	String getCurrentLanguageProperty(String property);

	/**
	 * Gets the translation of a message in the current language.
	 */
	String getTranslation(String message);

	/**
	 * Gets the translation of a message in the specified language.
	 */
	String getTranslation(String message, String language);

	/**
	 * Gets the translation of an expression (by substituting translatable strings inside it).
	 */
	String getExpressionTranslation(String expression);
}
