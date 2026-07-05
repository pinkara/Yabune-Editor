package com.pinkara.ye.editor.filter;

import java.util.LinkedHashMap;
import java.util.Map;

public class Config {
    public final Map<String, CfgParameter<?>> parameters = new LinkedHashMap<>();

    private void addParameter(String name, CfgParameter<?> par) {
        this.parameters.put(name, par);
    }

    public void addInt(String name, int value, int min, int max) {
        this.addParameter(name, new CfgParameter.CfgParameterInt(value, min, max));
    }

    public int getInt(String name) {
        if (this.parameters.containsKey(name)) {
            return (Integer) this.parameters.get(name).getValue();
        }
        throw new IllegalArgumentException("Value Not Found");
    }

    public void addBoolean(String name, boolean value) {
        this.addParameter(name, new CfgParameter.CfgParameterBoolean(value));
    }

    public boolean getBoolean(String name) {
        if (this.parameters.containsKey(name)) {
            return (Boolean) this.parameters.get(name).getValue();
        }
        throw new IllegalArgumentException("Value Not Found");
    }
}
