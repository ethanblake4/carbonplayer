package com.carbonplayer.model.entity;

import org.json.JSONException;
import org.json.JSONObject;

public class ConfigEntry {
    private String name;
    private String value;

    @SuppressWarnings("unused")
    public ConfigEntry(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public ConfigEntry(JSONObject j) throws JSONException{
        this.name =  j.getString("key");
        this.value = j.getString("value");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
