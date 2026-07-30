package com.willclay.mapeditor.domain;

import com.willclay.mapeditor.core.EditorModel;

import java.util.Arrays;

/**
 * A single char grid in the layer stack. Storage is always char; {@link LayerType}
 * only decides whether the layer is written out as char[][] or int[][].
 */
public class Layer
{
    public static enum LayerType { CHAR, INT }

    public String name;
    public boolean visible = true;

    public LayerType type = LayerType.CHAR;
    public char[][] grid; // [rows][cols] - internal storage is always char

    public Layer(LayerType type, String name, int rows, int cols)
    {
        this.name = name;
        this.grid = new char[rows][cols];
        for (char[] r : grid) { Arrays.fill(r, EditorModel.EMPTY); }
    }

    /** Deep copy, so history snapshots never alias a live grid. */
    public Layer copy()
    {
        Layer copy = new Layer(type, name, getRows(), getCols());

        copy.visible = visible;
        copy.type = type;
        for (int r = 0; r < grid.length; r++) { copy.grid[r] = grid[r].clone(); }

        return copy;
    }

    public boolean contains(int row, int col)
    {
        return row >= 0 && row < getRows() && col >= 0 && col < getCols();
    }

    public void fill(char letter)
    {
        for (char[] r : grid) { Arrays.fill(r, letter); }
    }

    public void toggleType()
    {
        type = (type == LayerType.CHAR) ? LayerType.INT : LayerType.CHAR;
    }

    // Getters
    public int getRows() { return grid.length; }
    public int getCols() { return grid.length == 0 ? 0 : grid[0].length; }
    public boolean isIntType() { return type == LayerType.INT; }
}