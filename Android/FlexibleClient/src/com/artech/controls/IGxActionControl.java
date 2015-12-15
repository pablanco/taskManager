package com.artech.controls;

import android.view.View;

import com.artech.base.metadata.ActionDefinition;
import com.artech.base.model.Entity;

public interface IGxActionControl
{
	ActionDefinition getAction();
	void setOnClickListener(View.OnClickListener listener);
	Entity getEntity();
	void setEntity(Entity entity);
}
