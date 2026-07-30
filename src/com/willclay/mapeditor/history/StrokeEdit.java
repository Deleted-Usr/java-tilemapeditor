package com.willclay.mapeditor.history;

import com.willclay.mapeditor.core.EditorModel;
import com.willclay.mapeditor.domain.Layer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A grouped set of cell changes from a single paint stroke (mouse press to
 * release). Only changed cells are stored, so painting stays memory-cheap.
 */
public class StrokeEdit implements Edit
{
    private record CellChange(int layer, int row, int col, char before, char after) {}

    private final List<CellChange> changes = new ArrayList<>();

    // Maps a packed cell key to its slot in 'changes', so a drag that revisits
    // a cell updates that entry instead of appending a duplicate.
    private final Map<Long, Integer> slots = new HashMap<>();

    public void record(int layer, int row, int col, char before, char after)
    {
        Integer slot = slots.get(key(layer, row, col));

        if (slot == null)
        {
            slots.put(key(layer, row, col), changes.size());
            changes.add(new CellChange(layer, row, col, before, after));
        }
        else
        {
            // Keep the original 'before' so one undo reverts the whole stroke.
            CellChange existing = changes.get(slot);
            changes.set(slot, new CellChange(layer, row, col, existing.before(), after));
        }
    }

    public boolean isEmpty() { return changes.isEmpty(); }

    @Override
    public void undo(EditorModel m)
    {
        for (CellChange cc : changes) { apply(m, cc, cc.before()); }
    }

    @Override
    public void redo(EditorModel m)
    {
        for (CellChange cc : changes) { apply(m, cc, cc.after()); }
    }

    private static void apply(EditorModel m, CellChange cc, char letter)
    {
        // A later structural edit may have removed or shrunk the layer.
        if (cc.layer() >= m.getLayers().size()) return;

        Layer layer = m.getLayers().get(cc.layer());
        if (!layer.contains(cc.row(), cc.col())) return;

        layer.grid[cc.row()][cc.col()] = letter;
    }

    private static long key(int layer, int row, int col)
    {
        return (((long) layer) << 40) ^ (((long) row) << 20) ^ col;
    }
}