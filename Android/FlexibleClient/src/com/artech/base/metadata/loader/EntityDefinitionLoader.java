package com.artech.base.metadata.loader;

import java.util.Hashtable;

import com.artech.base.metadata.AttributeDefinition;
import com.artech.base.metadata.DataItem;
import com.artech.base.metadata.IPatternMetadata;
import com.artech.base.metadata.LevelDefinition;
import com.artech.base.metadata.RelationDefinition;
import com.artech.base.metadata.StructureDefinition;
import com.artech.base.serialization.INodeCollection;
import com.artech.base.serialization.INodeObject;
import com.artech.base.services.IContext;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;

public class EntityDefinitionLoader extends MetadataLoader
{
	@Override
	public  IPatternMetadata load(IContext context, String data)
	{
		data = Strings.toLowerCase(data);

		// Try to read with both name formats, new and old.
		INodeObject json = getDefinition(context, data + ".bc");
		if (json == null)
			json = getDefinition(context, data);

		if (json != null)
			return LoadJSON(json);
		else
			return null;
	}

	private static StructureDefinition LoadJSON(INodeObject jsonData)
	{
		INodeObject gxObject = jsonData.getNode("GxObject"); //$NON-NLS-1$
		INodeObject structure = gxObject.getNode("Structure"); //$NON-NLS-1$
		INodeObject relations = gxObject.optNode("Relations"); //$NON-NLS-1$
		StructureDefinition definition = new StructureDefinition(gxObject.getString("Name")); //$NON-NLS-1$
		definition.setConnectivitySupport(ApplicationLoader.readConnectivity(gxObject));
		LoadLevels(definition, structure);
		LoadRelations(definition, relations);

		return definition;
	}

	private static void LoadRelations(StructureDefinition definition, INodeObject relations)
	{
		INodeObject manyToOneReader = relations.optNode("ManyToOne"); //$NON-NLS-1$
		INodeCollection referencesReader = manyToOneReader.optCollection("references"); //$NON-NLS-1$
		for (int i = 0; i < referencesReader.length() ; i++)
		{
			INodeObject obj = referencesReader.getNode(i);
			RelationDefinition rel = ReadRelation(obj);
			if (rel.getBCRelated() == null || !rel.getBCRelated().equals(definition.getName()))
				definition.ManyToOneRelations.add(rel);
		}
	}

	/*{"Name":"OradoresOraCod","BusinessComponent":"Oradores","Description":"Oradores",
		"ForeignKey":
			{"KeyAttributes":[
				{"Name":"OraCod","Type":"char"}]},
			"InferredAttributes":
			{"Attributes":[
				{"Name":"OraApellido","Type":"char"},
				{"Name":"OraNombre","Type":"char"},
				{"Name":"OraEmpresa","Type":"char"}
				]
			}
	 },
*/

	private static  RelationDefinition ReadRelation(INodeObject obj)
	{
		RelationDefinition relation = new RelationDefinition();
		relation.setName(obj.getString("Name")); //$NON-NLS-1$
		relation.setBCRelated(obj.getString("BusinessComponent")); //$NON-NLS-1$
		INodeObject foreignKey = obj.optNode("ForeignKey"); //$NON-NLS-1$
		INodeCollection keyAtts =  foreignKey.optCollection("KeyAttributes"); //$NON-NLS-1$
		INodeObject inferredAtts = obj.optNode("InferredAttributes"); //$NON-NLS-1$
		if (inferredAtts != null) {
			INodeCollection atts = inferredAtts.optCollection("Attributes"); //$NON-NLS-1$
			if (atts != null) {
				for (int i = 0 ; i < atts.length() ; i++) {
					INodeObject attRefjson = atts.getNode(i);

					String attName = attRefjson.optString("Name"); //$NON-NLS-1$
					AttributeDefinition att = Services.Application.getAttribute(attName);
					if (att != null) { // _GXI are being inferred so we have to ignore this here
						att.setProperty("AtributeSuperType", attRefjson.optString("AtributeSuperType")); //$NON-NLS-1$ //$NON-NLS-2$
						relation.getInferredAtts().add(attName);
					}
				}
			}
		}
		for (int i = 0; i < keyAtts.length() ; i++)
		{
			INodeObject attRefjson = keyAtts.getNode(i);
			String attName = attRefjson.optString("Name"); //$NON-NLS-1$
			AttributeDefinition att = Services.Application.getAttribute(attName);
			if (att != null) {
				att.setProperty("AtributeSuperType", attRefjson.optString("AtributeSuperType")); //$NON-NLS-1$ //$NON-NLS-2$
				relation.getKeys().add(attName);
			}
		}
		return relation;
	}

	private static void LoadAttributes(LevelDefinition definition, INodeObject structure)
	{
		INodeCollection  attributes = structure.getCollection("Attributes"); //$NON-NLS-1$
		for (int i = 0; i < attributes.length() ; i++)
		{
			INodeObject attribute = attributes.getNode(i);
			String attName = attribute.optString("InternalName"); //$NON-NLS-1$
			if (!Services.Strings.hasValue(attName))
				attName = attribute.getString("Name"); //$NON-NLS-1$

			//get from globals att
			AttributeDefinition attDefinition = Services.Application.getAttribute(attName);
			if (attDefinition == null)
			{
				Services.Log.warning("Load Entity Attributes", "Attribute Definition not found for " + attName); //$NON-NLS-1$ //$NON-NLS-2$
				continue;
			}

			DataItem trnAtt = new DataItem(attDefinition);
			for (String propName : attribute.names())
				trnAtt.setProperty(propName, attribute.get(propName));

			definition.Items.add(trnAtt);
		}
	}

	private static  void LoadLevels(StructureDefinition structureDef, INodeObject structure)
	{
		Hashtable<String, LevelDefinition> tempLevels = new Hashtable<String, LevelDefinition>();
		INodeCollection  levels = structure.getCollection("Levels"); //$NON-NLS-1$

		for (int i = 0; i < levels.length() ; i++)
		{
			INodeObject level = levels.getNode(i);
			String parentLevel = Strings.toLowerCase(level.getString("ParentLevel")); //$NON-NLS-1$

			LevelDefinition levelDefinition;
			// Is the first level -> take the already created level, otherwise create a new one an add to the hash
			if (parentLevel.length() == 0)
			{
				levelDefinition = structureDef.Root;
				tempLevels.put(Strings.toLowerCase(level.getString("Name")), structureDef.Root); //$NON-NLS-1$
			}
			else
			{
				levelDefinition = new LevelDefinition(null);
				tempLevels.put(Strings.toLowerCase(level.getString("Name")), levelDefinition); //$NON-NLS-1$
			}

			// Load Attributes for the level
			LoadAttributes(levelDefinition, level);

			// Load the Property Bag for the Level
			levelDefinition.deserialize(level);

			levelDefinition.getName(); // only to force deserialization

			levelDefinition.setName(level.getString("LevelName")); //$NON-NLS-1$
			levelDefinition.setDescription(level.getString("Description")); //$NON-NLS-1$

			// Add the Level to the Parent Level
			if (parentLevel.length() > 0)
			{
				LevelDefinition parentLevelDefinition = tempLevels.get(parentLevel);
				parentLevelDefinition.Levels.add(levelDefinition);
				levelDefinition.setParent(parentLevelDefinition);
			}
		}
	}
}
