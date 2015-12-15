/**
 * App Entry Types
 */
package com.artech.base.metadata.enums;

public final class GxObjectTypes
{
	// Directly callable as application.
	public static final String WorkWithGuid = "15cf49b5-fc38-4899-91b5-395d02d79889"; //$NON-NLS-1$
	public static final String DashboardGuid = "9bdcc055-174e-4af6-96cb-a2ceef6c5f09"; //$NON-NLS-1$
	public static final String SDPanelGuid = "d82625fd-5892-40b0-99c9-5c8559c197fc"; //$NON-NLS-1$

	// Other GX objects.
	private static final String DataProviderGuid = "2a9e9aba-d2de-4801-ae7f-5e3819222daf"; //$NON-NLS-1$
	private static final String ProcedureGuid = "84a12160-f59b-4ad7-a683-ea4481ac23e9"; //$NON-NLS-1$
	private static final String TransactionGuid = "1db606f2-af09-4cf9-a3b5-b481519d28f6"; //$NON-NLS-1$
	private static final String WebPanelGuid = "c9584656-94b6-4ccd-890f-332d11fc2c25"; //$NON-NLS-1$
	private static final String ApiGuid = "c163e562-42c6-4158-ad83-5b21a14cf30e"; //$NON-NLS-1$
	private static final String VariableObjectGuid = "00000000-0000-0000-0000-000000000000"; //$NON-NLS-1$

	public static final short NONE = 0;

	public static final short PROCEDURE = 1;
	public static final short TRANSACTION = 2;
	public static final short WEBPANEL = 3;
	public static final short SDPANEL = 4;
	public static final short DASHBOARD = 5;
	public static final short API = 6;
	public static final short DATAPROVIDER = 7;

	public static final short VARIABLE_OBJECT = 10;

	private static short getGxObjectTypeFromGuid(String guid)
	{
		if (guid.equals(TransactionGuid))
			return TRANSACTION;
		if (guid.equals(WebPanelGuid))
			return WEBPANEL;
		if (guid.equals(DashboardGuid))
			return DASHBOARD;
		if (guid.equals(WorkWithGuid) || guid.equals(SDPanelGuid))
			return SDPANEL; // SDPanels are merged with WWSD.
		if (guid.equals(ApiGuid))
			return API;
		if (guid.equals(ProcedureGuid))
			return PROCEDURE;
		if (guid.equals(DataProviderGuid))
			return DATAPROVIDER;
		if (guid.equals(VariableObjectGuid))
			return VARIABLE_OBJECT;
		return NONE;
	}

	public static short getGxObjectTypeFromName(String name)
	{
		if (name != null && name.length() > 37)
		{
			String guidObject = name.substring(0, 36);
			return getGxObjectTypeFromGuid(guidObject);
		}
		return NONE;
	}
}