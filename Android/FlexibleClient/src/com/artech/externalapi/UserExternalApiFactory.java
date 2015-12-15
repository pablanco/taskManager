package com.artech.externalapi;

public class UserExternalApiFactory implements IUserExternalApiDeclarations {

	@Override
	public ExternalApiDefinition[] getDeclarations() {
		
		ExternalApiDefinition [] definitions = { new ExternalApiDefinition("myapi", "com.artech.android.api.MyApi") , //$NON-NLS-1$ //$NON-NLS-2$
		new ExternalApiDefinition("sdhttpapi", "com.artech.android.api.SDHttpAPI") , //$NON-NLS-1$ //$NON-NLS-2$
		};
		return definitions;
	}

}
