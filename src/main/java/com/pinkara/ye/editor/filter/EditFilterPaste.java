package com.pinkara.ye.editor.filter;

import com.pinkara.ye.editor.Editor;
import com.pinkara.youma.math.AABBInt;

public class EditFilterPaste extends EditFilterBase {
    @Override
    public void init(Config par) {
        super.init(par);
        par.addBoolean("IgnoreAir", false);
    }

    @Override
    public String getFilterName() {
        return "Paste";
    }

    @Override
    public boolean edit(Editor editor) {
        boolean ignoreAir = this.getCfg().getBoolean("IgnoreAir");
        StringBuilder sb = new StringBuilder();
        if (ignoreAir) {
            sb.append("IgnoreAir");
        }
        AABBInt box = editor.getPasteBox();
        if (box != null) {
            editor.record(box);
            editor.paste(box, sb.toString());
            return true;
        }
        return false;
    }
}
