package com.willclay.mapeditor.domain;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * One entry in the tile palette: the letter written into a layer grid plus
 * everything needed to draw and export it.
 */
public class TileDef
{
    public static enum Category { BACKGROUND, TERRAIN, PROP, MARKER }

    public char letter;
    public int id = 0;  // Numeric value used by int[][] layers

    public String name;

    public Category category;
    public String layerName = null; // Restrict to one Layer; null = any

    public Color colour;         // Fallback fill when there's no sprite
    public BufferedImage sprite; // optional image, may be null
    public String spritePath;    // remembered for palette save/load

    public TileDef(char letter, String name, Category category, Color colour)
    {
        this.letter = letter;
        this.name = name;

        this.category = category;
        this.colour = colour;
    }

    /** True when this tile may be painted onto the given layer. */
    public boolean isUsableOn(String targetLayerName)
    {
        return layerName == null || layerName.equals(targetLayerName);
    }

    /** True for the categories exported as objects rather than grid data. */
    public boolean isLoadable()
    {
        return category == Category.PROP || category == Category.MARKER;
    }
}