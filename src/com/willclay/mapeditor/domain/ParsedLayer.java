package com.willclay.mapeditor.domain;

/**
 * One layer recovered from pasted Java source. Only one of {@code grid} /
 * {@code intGrid} is populated, depending on the declared array type.
 */
public class ParsedLayer
{
    public String name;

    public char[][] grid;   // set for CHAR layers
    public int[][] intGrid; // set for INT layers

    public boolean intType = false;
    public boolean padded = false; // true when ragged rows were widened

    public int rows;
    public int cols;

    public ParsedLayer(String name, char[][] grid, int rows, int cols)
    {
        this.name = name;
        this.grid = grid;

        this.rows = rows;
        this.cols = cols;
    }

    public ParsedLayer(String name, int[][] intGrid, int rows, int cols)
    {
        this.name = name;
        this.intGrid = intGrid;
        this.intType = true;

        this.rows = rows;
        this.cols = cols;
    }

    /** "Terrain (32x18)" - used by the import preview. */
    public String describe()
    {
        return name + " (" + cols + "x" + rows + ")";
    }
}