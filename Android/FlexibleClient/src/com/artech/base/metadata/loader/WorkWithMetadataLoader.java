package com.artech.base.metadata.loader;

import java.util.List;

import com.artech.actions.ActionParametersHelper;
import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.ActionParameter;
import com.artech.base.metadata.DataItem;
import com.artech.base.metadata.DataSourceDefinition;
import com.artech.base.metadata.DataViewDefinition;
import com.artech.base.metadata.DetailDefinition;
import com.artech.base.metadata.IDataSourceDefinition;
import com.artech.base.metadata.IDataViewDefinition;
import com.artech.base.metadata.IPatternMetadata;
import com.artech.base.metadata.IViewDefinition;
import com.artech.base.metadata.ObjectParameterDefinition;
import com.artech.base.metadata.SectionDefinition;
import com.artech.base.metadata.VariableDefinition;
import com.artech.base.metadata.WWLevelDefinition;
import com.artech.base.metadata.WWListDefinition;
import com.artech.base.metadata.WorkWithDefinition;
import com.artech.base.metadata.enums.GxObjectTypes;
import com.artech.base.metadata.expressions.Expression;
import com.artech.base.metadata.expressions.ExpressionFactory;
import com.artech.base.metadata.layout.LayoutDefinition;
import com.artech.base.serialization.INodeCollection;
import com.artech.base.serialization.INodeObject;
import com.artech.base.services.IContext;
import com.artech.base.services.Services;
import com.artech.base.utils.PlatformHelper;
import com.artech.base.utils.Strings;

public class WorkWithMetadataLoader extends MetadataLoader
{
	@Override
	public IPatternMetadata load(IContext context, String name)
	{
		INodeObject jsonData = getDefinition(context, Strings.toLowerCase(name));
		if (jsonData == null)
			return null;

		Services.Log.debug(String.format("Loading '%s'.", name));
		return loadJSON(jsonData);
	}

	public WorkWithDefinition loadJSON(INodeObject json)
	{
		WorkWithDefinition wwMetadata = new WorkWithDefinition();
		INodeObject jsonInstance = json.getNode("instance"); //$NON-NLS-1$
		INodeCollection jsonLevels = jsonInstance.optCollection("level"); //$NON-NLS-1$

		/*
		// Read BC (we can have Patterns without businessComponent associated).
		INodeObject bcJson = jsonInstance.optNode("businessComponent");
		readBusinessComponent(wwMetadata, bcJson);
		 */
		String bc = jsonInstance.optString("@businessComponent"); //$NON-NLS-1$
		if (bc.length() > 0)
		{
			/* set BC for ww based in bc */
			//wwMetadata.setBusinessComponent(MetadataLoader.getObjectName(bc));
			wwMetadata.setBusinessComponent(bc);
		}

		//Read Instance Properties
		INodeObject instancePropNode = jsonInstance.optNode("instanceProperties"); //$NON-NLS-1$
		wwMetadata.getInstanceProperties().deserialize(instancePropNode);


		// Read levels
		for (int i = 0; i < jsonLevels.length(); i++)
		{
			INodeObject jsonLevel = jsonLevels.getNode(i);
			WWLevelDefinition level = readLevelDefinition(wwMetadata, jsonLevel);
			wwMetadata.getLevels().add(level);
		}

		return wwMetadata;
	}

	private WWLevelDefinition readLevelDefinition(WorkWithDefinition wwMetadata, INodeObject levelJson)
	{
		String levelId = levelJson.optString("@id"); //$NON-NLS-1$
		WWLevelDefinition levelDefinition = new WWLevelDefinition(wwMetadata, levelId);

		String levelName = levelJson.optString("@name", Strings.EMPTY); //$NON-NLS-1$
		levelDefinition.setName(levelName);

		levelDefinition.setDescription(levelJson.optString("@description", Strings.EMPTY)); //$NON-NLS-1$

		INodeObject listJson = levelJson.optNode("list"); //$NON-NLS-1$
		if (listJson != null)
		{
			WWListDefinition list = readList(wwMetadata, levelDefinition, listJson);
			levelDefinition.setList(list);
		}

		INodeObject jsonDetail = levelJson.optNode("detail"); //$NON-NLS-1$
		if (jsonDetail != null)
			readDetail(wwMetadata, levelDefinition, jsonDetail);

		return levelDefinition;
	}

	private void readDetail(WorkWithDefinition wwMetadata, WWLevelDefinition wwLevelDefinition, INodeObject jsonDetail)
	{
		DetailDefinition detail = new DetailDefinition(wwLevelDefinition);
		detail.deserialize(jsonDetail);

		readSections(wwMetadata, wwLevelDefinition, detail, jsonDetail);
		readFormParameters(detail.getParameters(), jsonDetail);
		readData(jsonDetail, detail);
		readVariables(detail, jsonDetail);
		readRules(detail, jsonDetail);

		readActions(detail, jsonDetail, detail.getActions());
		readLayouts(detail, jsonDetail);

		wwLevelDefinition.setDetail(detail);
	}

	//Read FormMetadata Parameters
	private static void readFormParameters(List<ObjectParameterDefinition> parameters, INodeObject viewJson)
	{
		INodeObject jsonParameters = viewJson.optNode("parameters"); //$NON-NLS-1$
		if (jsonParameters != null)
			readObjectParameterList(parameters, jsonParameters);
	}

	public static void readObjectParameterList(List<ObjectParameterDefinition> parameters, INodeObject jsonParameters)
	{
		for (INodeObject jsonParameter : jsonParameters.optCollection("parameter")) //$NON-NLS-1$
		{
			String name = jsonParameter.getString("@name"); //$NON-NLS-1$
			String mode = jsonParameter.optString("@accessor"); //$NON-NLS-1$

			ObjectParameterDefinition parameter = new ObjectParameterDefinition(name, mode);
			parameters.add(parameter);
		}
	}

	private static void readSections(WorkWithDefinition wwMetadata, WWLevelDefinition levelDefinition, DetailDefinition detail, INodeObject viewJson)
	{
		INodeObject sectionsJson = viewJson.optNode("sections"); //$NON-NLS-1$
		if (sectionsJson != null)
		{
			INodeCollection sectionsArray = sectionsJson.optCollection("section"); //$NON-NLS-1$
			for (int i = 0; i < sectionsArray.length() ; i++)
			{
				INodeObject sectionJson = sectionsArray.getNode(i);
				readSection(wwMetadata, detail, sectionJson);
			}
		}
	}

	private static void readSection(WorkWithDefinition wwMetadata, DetailDefinition detail, INodeObject sectionJson)
	{
		SectionDefinition section = new SectionDefinition(detail);
		section.deserialize(sectionJson);

		readFormParameters(section.getParameters(), sectionJson);
		readData(sectionJson, section);
		readVariables(section, sectionJson);
		readRules(section, sectionJson);

		readActions(section, sectionJson, section.getActions());
		readLayouts(section, sectionJson);

		detail.getSections().add(section);
	}

	private static void readListData(WorkWithDefinition wwMetadata, INodeObject jsonComponent, IDataViewDefinition component)
	{
		readData(jsonComponent, component);
		if (!Services.Strings.hasValue(wwMetadata.getBusinessComponentName()) )
		{
			//get bc from first collection datasource.
			for (IDataSourceDefinition ds : component.getDataSources())
			{
				if (ds.isCollection())
					wwMetadata.setBusinessComponent(ds.getAssociatedBCName());
			}
		}
	}

	private static void readData(INodeObject jsonComponent, IDataViewDefinition component)
	{
		String mainDataSourceName = jsonComponent.optString("@DataProvider"); //$NON-NLS-1$
		IDataSourceDefinition mainDataSource = null;

		// Read all data providers for this component.
		INodeCollection jsonDatas = jsonComponent.optCollection("data"); //$NON-NLS-1$
		for (int i = 0; i < jsonDatas.length(); i++)
		{
			IDataSourceDefinition dataSource = new DataSourceDefinition(component, jsonDatas.getNode(i));
			component.getDataSources().add(dataSource);

			if (dataSource.getName().equals(mainDataSourceName))
				mainDataSource = dataSource;
		}

		((DataViewDefinition)component).setMainDataSource(mainDataSource);
	}

	public static void readVariables(IViewDefinition dataView, INodeObject jsonDataView)
	{
		INodeObject jsonVariables = jsonDataView.optNode("variables"); //$NON-NLS-1$
		if (jsonVariables != null)
		{
			for (INodeObject jsonVariable : jsonVariables.optCollection("variable")) //$NON-NLS-1$
			{
				VariableDefinition variable = new VariableDefinition(jsonVariable);
				dataView.getVariables().add(variable);
			}
		}
	}

	private static void readRules(IDataViewDefinition parent, INodeObject parentJson)
	{
		INodeObject jsonRules = parentJson.optNode("rules");
		if (jsonRules != null)
			parent.getRules().deserialize(jsonRules);
	}

	private static void readActions(IDataViewDefinition parent, INodeObject parentJson, List<ActionDefinition> vector)
	{
		INodeObject actionsJson = parentJson.optNode("actions"); //$NON-NLS-1$
		if (actionsJson == null)
			actionsJson = parentJson.optNode("components"); //$NON-NLS-1$

		if (actionsJson != null)
		{
			INodeCollection actionsArray = actionsJson.optCollection("action"); //$NON-NLS-1$
			for (int k = 0; k < actionsArray.length() ; k++)
			{
				ActionDefinition actionDef = readAction(parent, actionsArray.getNode(k));
				if (actionDef != null)
					vector.add(actionDef);
			}
		}
	}

	private static ActionDefinition readAction(IDataViewDefinition parent, INodeObject nodeObject)
	{
		if (nodeObject == null)
			return null;

		ActionDefinition action = new ActionDefinition(parent);

		action.deserialize(nodeObject);
		String gxObject = action.optStringProperty("@call"); //$NON-NLS-1$
		if (gxObject != null)
		{
			action.setGxObjectType(GxObjectTypes.getGxObjectTypeFromName(gxObject));
			action.setGxObject(MetadataLoader.getObjectName(gxObject));
		}

		readEventParameters(parent, action.getEventParameters(), nodeObject);
		readActionParameters(parent, action.getParameters(), nodeObject);
		readActions(parent, nodeObject, action.getActions());

		return action;
	}

	public static void readEventParameters(IViewDefinition parent, List<ActionParameter> eventParameterList, INodeObject jsonAction)
	{
		INodeObject jsonParameterList = jsonAction.optNode("eventParameters"); //$NON-NLS-1$
		if (jsonParameterList != null)
			readActionParameterList(parent, eventParameterList, jsonParameterList);
	}

	public static void readActionParameters(IViewDefinition parent, List<ActionParameter> parameterList, INodeObject jsonAction)
	{
		// Read Parameters in Order
		// "Parameters" is lower-case in WWSD, upper-case in Dashboard.
		INodeObject jsonParameterList = jsonAction.optNode("parameters"); //$NON-NLS-1$
		if (jsonParameterList == null)
			jsonParameterList = jsonAction.optNode("Parameters");  //$NON-NLS-1$

		if (jsonParameterList != null)
			readActionParameterList(parent, parameterList, jsonParameterList);
	}

	public static void readActionParameterList(IViewDefinition parent, List<ActionParameter> parameterList, INodeObject jsonParameterList)
	{
		if (jsonParameterList != null)
		{
			// Read parameters
			INodeCollection parArray = jsonParameterList.optCollection("parameter"); //$NON-NLS-1$
			for (int k = 0; k < parArray.length() ; k++)
			{
				ActionParameter parameter = readOneActionParameter(parent, parArray.getNode(k));
				parameterList.add(parameter);
			}
		}
	}

	private static ActionParameter readOneActionParameter(IViewDefinition parent, INodeObject oneParJson)
	{
		String name = oneParJson.optString("@name");
		String value = oneParJson.optString("@value");
		INodeObject expression = oneParJson.optNode("expression");

		ActionParameter parameter = newActionParameter(name, value, expression);

		if (parent != null && !ActionParametersHelper.isConstantExpression(value))
		{
			DataItem dataItem = parent.getVariable(value);
			if (dataItem == null && parent instanceof IDataViewDefinition)
			{
				for (IDataSourceDefinition dataSource : ((IDataViewDefinition)parent).getDataSources())
				{
					dataItem = dataSource.getDataItem(value);
					if (dataItem != null)
						break;
				}
			}

			parameter.setValueDefinition(dataItem);
		}

		return parameter;
	}

	public static ActionParameter newActionParameter(String jsonName, String jsonValue, INodeObject jsonExpression)
	{
		if (jsonValue != null)
			jsonValue = jsonValue.trim(); // Bug in IDE: Event parameters may contain leading/trailing spaces.

		Expression expression = null;
		if (jsonExpression != null)
			expression = ExpressionFactory.parse(jsonExpression);

		return new ActionParameter(jsonName, jsonValue, expression);
	}

	private WWListDefinition readList(WorkWithDefinition wwMetadata, WWLevelDefinition levelDefinition, INodeObject jsonList)
	{
		WWListDefinition list = new WWListDefinition(levelDefinition);
		list.deserialize(jsonList);

		readFormParameters(list.getParameters(), jsonList);
		readListData(wwMetadata, jsonList, list);
		readVariables(list, jsonList);
		readRules(list, jsonList);

		readActions(list, jsonList, list.getActions());
		readLayouts(list, jsonList);

		return list;
	}

	//SectionDefinition

	private static void readLayouts(IDataViewDefinition dataView, INodeObject jsonDataView)
	{
		INodeCollection jsonLayouts = jsonDataView.optCollection("layout"); //$NON-NLS-1$
		for (int i = 0; i < jsonLayouts.length(); i++)
		{
			LayoutDefinition layout = readLayout(dataView, jsonLayouts.getNode(i));

			if (PlatformHelper.isValidLayout(layout))
				dataView.getLayouts().add(layout);
		}
	}

	//Layout WWSelectionDefinition

	private static LayoutDefinition readLayout(IDataViewDefinition parent, INodeObject jsonLayout)
	{
		LayoutDefinition layoutDef = new LayoutDefinition(parent);
		layoutDef.setContent(jsonLayout);
		return layoutDef;
	}
}
