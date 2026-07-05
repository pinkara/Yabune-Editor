package com.pinkara.ye.editor.filter;

import com.pinkara.ye.editor.Editor;
import com.pinkara.youma.block.BlockSet;
import com.pinkara.youma.math.AABBInt;

public class EditFilterDelete extends EditFilterBase {
    @Override
    public String getFilterName() {
        return "Delete";
    }

    @Override
    public boolean edit(Editor editor) {
        AABBInt box = editor.getSelectBox();
        if (box == null) {
            return false;
        }
        editor.record(box);
        editor.delete(box, "");
        return true;
    }
}
