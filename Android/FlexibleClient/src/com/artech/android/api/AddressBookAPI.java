package com.artech.android.api;

import java.util.List;

import android.support.annotation.NonNull;

import com.artech.actions.ActionResult;
import com.artech.android.contacts.AddressBook;
import com.artech.base.model.Entity;
import com.artech.externalapi.ExternalApi;
import com.artech.externalapi.ExternalApiResult;

public class AddressBookAPI extends ExternalApi
{
	private static final String METHOD_ADD_CONTACT = "AddContact";
	private static final String METHOD_GET_ALL_CONTACTS = "GetAllContacts";

	@Override
	public @NonNull ExternalApiResult execute(String method, List<Object> parameters)
	{
		if (method.equalsIgnoreCase(METHOD_ADD_CONTACT)) //$NON-NLS-1$
		{
			if (SDActions.addContactFromParameters(getActivity(), toString(parameters)))
				return new ExternalApiResult(ActionResult.SUCCESS_WAIT);
			else
				return InteropAPI.getInteropActionFailureResult();
		}
		else if (method.equalsIgnoreCase(METHOD_GET_ALL_CONTACTS))
		{
			AddressBook helper = new AddressBook(getContext());
			List<Entity> contactsData = helper.getAllContacts();

			return ExternalApiResult.success(contactsData);
		}
		else
			return ExternalApiResult.failureUnknownMethod(this, method);
	}
}
