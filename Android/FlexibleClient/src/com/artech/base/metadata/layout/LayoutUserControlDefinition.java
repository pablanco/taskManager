package com.artech.base.metadata.layout;

import com.artech.base.metadata.loader.MetadataLoader;
import com.artech.base.metadata.loader.WorkWithMetadataLoader;
import com.artech.base.serialization.INodeObject;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;

import java.util.ArrayList;

/**
 * Created by gmilano on 8/3/15.
 */
public class LayoutUserControlDefinition extends LayoutItemDefinition {


    public LayoutUserControlDefinition(LayoutDefinition layout, LayoutItemDefinition itemParent)
    {
        super(layout, itemParent);
    }

    @Override
    public void readData(INodeObject node)
    {
        super.readData(node);
    }

}
