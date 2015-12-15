package com.genexus.live_editing.model.serializers;

import java.lang.reflect.Type;

import com.genexus.live_editing.model.ControlData;
import com.genexus.live_editing.model.MasterLayoutData;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class MasterLayoutDataSerializer implements JsonSerializer<MasterLayoutData> {

    @Override
    public JsonElement serialize(MasterLayoutData masterLayoutData, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject data = new JsonObject();
        data.add("Controls", getControls(masterLayoutData));
        data.add("Window", getWindowInfo(masterLayoutData));
        return data;
    }

    private JsonArray getControls(MasterLayoutData masterLayoutData) {
        JsonArray controls = new JsonArray();
        Gson gson = new Gson();
        for (ControlData controlData : masterLayoutData.getControls()) {
            controls.add(gson.toJsonTree(controlData));
        }
        return controls;
    }

    private JsonArray getWindowInfo(MasterLayoutData masterLayoutData) {
        JsonArray windows = new JsonArray();
        JsonObject window = new JsonObject();
        window.addProperty("Height", masterLayoutData.getWindowHeight());
        window.addProperty("Width", masterLayoutData.getWindowWidth());
        windows.add(window);
        return windows;
    }
}
