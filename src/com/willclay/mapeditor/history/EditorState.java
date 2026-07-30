package com.willclay.mapeditor.history;

import com.willclay.mapeditor.core.EditorModel;
import com.willclay.mapeditor.domain.Layer;

import java.util.ArrayList;
import java.util.List;

/**
 * A full capture of everything an undo needs to rebuild the model: dimensions,
 * the active layer, and a deep copy of every layer.
 */
public class EditorState
{
    private final int rows;
    private final int cols;
    private final int tileSize;
    private final int activeLayerIndex;

    private final List<Layer> layers = new ArrayList<>();

    public EditorState(EditorModel model)
    {
        this.rows = model.getRows();
        this.cols = model.getCols();
        this.tileSize = model.getTileSize();
        this.activeLayerIndex = model.getActiveLayerIndex();

        for (Layer layer : model.getLayers()) { layers.add(layer.copy()); }
    }

    /**
     * Fresh copies of the captured layers. Copying on the way out as well as
     * in keeps the snapshot reusable across repeated undo/redo cycles.
     */
    public List<Layer> copyLayers()
    {
        List<Layer> copies = new ArrayList<>();
        for (Layer layer : layers) { copies.add(layer.copy()); }

        return copies;
    }

    // Getters
    public int getRows() { return rows; }
    public int getCols() { return cols; }
    public int getTileSize() { return tileSize; }
    public int getActiveLayerIndex() { return activeLayerIndex; }
}