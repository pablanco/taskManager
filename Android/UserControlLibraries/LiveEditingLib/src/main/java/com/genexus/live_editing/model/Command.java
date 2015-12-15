package com.genexus.live_editing.model;

import com.artech.base.services.Services;
import com.google.gson.JsonElement;

public class Command {
    private CommandType type;
    private String partType;
    private String objType;
    private String objName;
    private String styleName;
    private String parent;
    private String transformName;
    private String langCode;
    private JsonElement data;

    public CommandType getType() {
        return type;
    }

    public void setType(CommandType type) {
        this.type = type;
    }

    public String getPartType() {
        return partType;
    }

    public void setPartType(String partType) {
        this.partType = partType;
    }

    public String getObjType() {
        return objType;
    }

    public void setObjType(String objType) {
        this.objType = objType;
    }

    public String getObjName() {
        return objName;
    }

    public void setObjName(String objName) {
        this.objName = objName;
    }

    public String getStyleName() {
        return styleName;
    }

    public void setStyleName(String styleName) {
        this.styleName = styleName;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public String getTransformName() {
        return transformName;
    }

    public void setTransformName(String transformName) {
        this.transformName = transformName;
    }

    public String getLangCode() {
        return langCode;
    }

    public void setLangCode(String langCode) {
        this.langCode = langCode;
    }

    public Object getData() {
        if (CommandType.THEME_COLOR_CHANGED.equals(type)) {
            return Services.Serializer.createCollection(data.toString());
        } else {
            return Services.Serializer.createNode(data.toString());
        }
    }

    public void setData(JsonElement data) {
        this.data = data;
    }
}
