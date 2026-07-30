package com.willclay.mapeditor.history;

import com.willclay.mapeditor.core.EditorModel;

/**
 * Undo support for coarse structural operations (new / resize / fill / clear /
 * import / layer add-remove-move) where tracking individual deltas would be
 * fiddly. The before and after states are captured as a pair, so the edit is
 * always complete by the time it reaches the stack.
 */
public class SnapshotEdit implements Edit
{
    private final EditorState before;
    private final EditorState after;

    public SnapshotEdit(EditorState before, EditorState after)
    {
        this.before = before;
        this.after = after;
    }

    @Override
    public void undo(EditorModel m) { m.restore(before); }

    @Override
    public void redo(EditorModel m) { m.restore(after); }
}