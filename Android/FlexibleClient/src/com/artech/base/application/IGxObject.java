package com.artech.base.application;

import com.artech.base.model.PropertiesObject;

/**
 * Interface of callable GeneXus objects.
 * @author matiash
 *
 */
public interface IGxObject
{
	OutputResult execute(PropertiesObject parameters);
}
