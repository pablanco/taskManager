package com.artech.activities;

import android.content.Intent;

import com.artech.actions.UIContext;
import com.artech.base.model.Entity;

public interface IIntentHandler {

	boolean tryHandleIntent(UIContext context, Intent intent, Entity entity);
}
