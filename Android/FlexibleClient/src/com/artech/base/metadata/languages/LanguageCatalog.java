package com.artech.base.metadata.languages;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Locale;

import com.artech.base.services.Services;

public class LanguageCatalog implements Serializable
{
	private static final long serialVersionUID = 1L;

	private final ArrayList<Language> mLanguages;
	private String mDefaultLanguage;

	private Language mCurrentLanguage;
	private String mCurrentLanguageId;

	public LanguageCatalog()
	{
		mLanguages = new ArrayList<Language>();
	}

	public void add(Language language)
	{
		mLanguages.add(language);
		mCurrentLanguageId = null; // Reset cached calculation.
	}

	public void setDefault(String defaultLanguage)
	{
		mDefaultLanguage = defaultLanguage;
	}

	public Language getCurrentLanguage()
	{
		// According to Android doc (http://developer.android.com/reference/java/util/Locale.html)
		// Since the user's locale changes dynamically, avoid caching this value (Locale.getDefault()).
		Locale locale = Services.Device.getLocale();
		if (!locale.toString().equals(mCurrentLanguageId))
		{
			// Need to (re)calculate current language (this can be cached).
			mCurrentLanguage = calculateCurrentLanguage(locale);
			mCurrentLanguageId = locale.toString();
		}

		return mCurrentLanguage;
	}

	/**
	 * Maps the current Java locale to a GeneXus language.
	 */
	private Language calculateCurrentLanguage(Locale locale)
	{
		String languageCode = locale.getLanguage();
		String countryCode = locale.getCountry();

		// Get by language first.
		ArrayList<Language> languagesForLanguageCode = new ArrayList<Language>();
		for (Language language : mLanguages)
		{
			if (languageCode.equalsIgnoreCase(language.getLanguageCode()))
				languagesForLanguageCode.add(language);
		}

		// If exactly one language, return it.
		if (languagesForLanguageCode.size() == 1)
			return languagesForLanguageCode.get(0);

		// If no matches, return the default language.
		if (languagesForLanguageCode.size() == 0)
			return getLanguage(mDefaultLanguage);

		// If there is more than one language, disambiguate using country.
		if (Services.Strings.hasValue(countryCode))
		{
			for (Language language : languagesForLanguageCode)
			{
				if (countryCode.equalsIgnoreCase(language.getCountryCode()))
					return language;
			}
		}

		// If there is more than one language but no country code matches, return the first one.
		return languagesForLanguageCode.get(0);
	}

	private Language getLanguage(String name)
	{
		for (Language language : mLanguages)
		{
			if (name.equalsIgnoreCase(language.getName()))
				return language;
		}

		return null;
	}

	public String getTranslation(String message)
	{
		return getTranslation(message, getCurrentLanguage());
	}

	public String getTranslation(String message, String language)
	{
		return getTranslation(message, getLanguage(language));
	}

	private String getTranslation(String message, Language language)
	{
		if (!Services.Strings.hasValue(message))
			return message;

		if (language == null)
			return message;

		return language.getTranslation(message);
	}

	public String getExpressionTranslation(String expression)
	{
		if (!Services.Strings.hasValue(expression))
			return expression;

		Language language = getCurrentLanguage();
		if (language == null)
			return expression;

		return language.getExpressionTranslation(expression);
	}
}
