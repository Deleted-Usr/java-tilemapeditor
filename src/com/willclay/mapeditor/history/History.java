package com.willclay.mapeditor.history;

import com.willclay.mapeditor.core.EditorModel;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Owns the undo/redo stacks and the stroke currently being painted. The model
 * delegates here rather than managing stacks itself.
 */
public class History
{
    private static final int MAX_EDITS = 200;

    private final Deque<Edit> undoStack = new ArrayDeque<>();
    private final Deque<Edit> redoStack = new ArrayDeque<>();

    private StrokeEdit currentStroke = null;

    // Strokes
    public void beginStroke()
    {
        endStroke(); // Close anything left open so ordering stays correct.
        currentStroke = new StrokeEdit();
    }

    /** No-op when no stroke is open, so the model can call this freely. */
    public void recordCell(int layer, int row, int col, char before, char after)
    {
        if (currentStroke != null) { currentStroke.record(layer, row, col, before, after); }
    }

    public void endStroke()
    {
        if (currentStroke != null && !currentStroke.isEmpty()) { push(currentStroke); }
        currentStroke = null;
    }

    // Snapshots
    public void pushSnapshot(EditorState before, EditorState after)
    {
        push(new SnapshotEdit(before, after));
    }

    // Undo / Redo
    public void undo(EditorModel model)
    {
        endStroke();
        if (undoStack.isEmpty()) return;

        Edit edit = undoStack.pop();
        edit.undo(model);
        redoStack.push(edit);
    }

    public void redo(EditorModel model)
    {
        if (redoStack.isEmpty()) return;

        Edit edit = redoStack.pop();
        edit.redo(model);
        undoStack.push(edit);
    }

    public void clear()
    {
        undoStack.clear();
        redoStack.clear();
        currentStroke = null;
    }

    private void push(Edit edit)
    {
        undoStack.push(edit);
        while (undoStack.size() > MAX_EDITS) { undoStack.removeLast(); }

        redoStack.clear(); // A new edit invalidates the redo branch.
    }

    // Getters
    public boolean canUndo() { return !undoStack.isEmpty(); }
    public boolean canRedo() { return !redoStack.isEmpty(); }
}