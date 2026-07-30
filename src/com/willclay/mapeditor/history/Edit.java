package com.willclay.mapeditor.history;

import com.willclay.mapeditor.core.EditorModel;

public interface Edit
{
    void undo(EditorModel m);
    void redo(EditorModel m);
}
