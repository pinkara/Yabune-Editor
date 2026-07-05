package com.pinkara.ye.editor.filter;

import com.pinkara.ye.editor.Editor;

public abstract class EditFilterBase {
    protected Config cfg;

    public void init(Config par) {
        this.cfg = par;
    }

    public Config getCfg() {
        return this.cfg;
    }

    public abstract String getFilterName();

    public abstract boolean edit(Editor var1);
}
