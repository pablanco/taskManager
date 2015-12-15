package com.artech.actions;

import java.util.ArrayList;
import java.util.List;

import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.enums.GxObjectTypes;
import com.artech.utils.Cast;

public class ActionFactory
{
	public static CompositeAction getAction(UIContext context, ActionDefinition definition, ActionParameters parameters)
	{
		if (parameters == null)
			parameters = ActionParameters.EMPTY;

		CompositeAction composite = new CompositeAction(context, definition, parameters);
		putActionAndChildren(composite, context, definition, parameters);
		return composite;
	}

	private static void putActionAndChildren(CompositeAction composite, UIContext context, ActionDefinition definition, ActionParameters parameters)
	{
		// NOTE: Handlers are added here and not in getActionHandlers() because they must surround
		// the whole composite (unlike getPre/Post actions) which are for individual steps.
		ActionDefinitionWithHandlers actionWithHandlers = Cast.as(ActionDefinitionWithHandlers.class, definition);
		if (actionWithHandlers != null)
			definition = actionWithHandlers.getDefinition();

		// Add pre handler...
		if (actionWithHandlers != null && actionWithHandlers.getPreHandler() != null)
			composite.addAction(new RunnableAction(context, actionWithHandlers.getPreHandler(), definition, parameters));

		// ... then "root" action...
		composite.addActions(getActionHandlers(context, definition, parameters));

		// ... then any subordinated actions, if present...
		for (ActionDefinition subAction : definition.getActions())
			putActionAndChildren(composite, context, subAction, parameters);

		// ... then post handler.
		if (actionWithHandlers != null && actionWithHandlers.getPostHandler() != null)
			composite.addAction(new RunnableAction(context, actionWithHandlers.getPostHandler(), definition, parameters));
	}

	private static List<Action> getActionHandlers(UIContext context, ActionDefinition definition, ActionParameters parameters)
	{
		List<Action> allHandlers = new ArrayList<Action>();
		Action handler = getSingleAction(context, definition, parameters);

		// Add pre-actions...
		List<Action> preHandlers = handler.getPreActions();
		if (preHandlers != null)
			allHandlers.addAll(preHandlers);

		// ... then the REAL action...
		allHandlers.add(handler);

		// ... and post actions afterwards.
		List<Action> postHandlers = handler.getPostActions();
		if (postHandlers != null)
			allHandlers.addAll(postHandlers);

		return allHandlers;
	}

	private static Action getSingleAction(UIContext context, ActionDefinition definition, ActionParameters parameters)
	{
		if (SetControlPropertyAction.isAction(definition))
			return new SetControlPropertyAction(context, definition, parameters);

		if (GetControlPropertyAction.isAction(definition))
			return new GetControlPropertyAction(context, definition, parameters);

		if (ControlMethodAction.isAction(definition))
			return new ControlMethodAction(context, definition, parameters);

		if (AssignmentAction.isAction(definition))
			return new AssignmentAction(context, definition, parameters);

		if (MultipleSelectionAction.isAction(definition))
			return new MultipleSelectionAction(context, definition, parameters);

		if (JumpAction.isAction(definition))
			return new JumpAction(context, definition, parameters);

		if (DependentValuesAction.isAction(definition))
			return new DependentValuesAction(context, definition, parameters);

		if (SetCallOptionsAction.isAction(definition))
			return new SetCallOptionsAction(context, definition, parameters);

		if (SyncronizationAction.isAction(definition))
			return new SyncronizationAction(context, definition, parameters);
		
		if (definition.getGxObjectType() == GxObjectTypes.VARIABLE_OBJECT)
			return new DynamicCallAction(context, definition, parameters);

		if (definition.getGxObjectType() == GxObjectTypes.TRANSACTION)
			return new CallBCAction(context, definition, parameters);

		if (definition.getGxObjectType() == GxObjectTypes.PROCEDURE || definition.getGxObjectType() == GxObjectTypes.DATAPROVIDER)
			return new CallGxObjectAction(context, definition, parameters);

		if (definition.getGxObjectType() == GxObjectTypes.WEBPANEL)
			return new CallWebPanelAction(context, definition, parameters);

		if (definition.getGxObjectType() == GxObjectTypes.DASHBOARD)
			return new CallDashboardAction(context, definition, parameters);

		if (definition.getGxObjectType() == GxObjectTypes.SDPANEL)
			return new WorkWithAction(context, definition, parameters);

		if (definition.getGxObjectType() == GxObjectTypes.API)
		{
			ApiAction apiAction = new ApiAction(context, definition, parameters);

			// "Special" API actions.
			if (apiAction.isLoginAction())
				return new CallLoginAction(context, definition, parameters);
			else if (apiAction.isLoginExternalAction())
				return new CallLoginExternalAction(context, definition, parameters);

			return apiAction;
		}
	
		return new NotImplementedAction(context, definition, parameters);
	}
}
