package com.willclay.mapeditor.panels;

import com.willclay.mapeditor.domain.TileDef;

import javax.swing.*;
import java.awt.*;

/** Small preview square showing a tile's sprite, or its fallback colour. */
public class TileSwatch extends JComponent
{
    private static final int DEFAULT_SIZE = 40;

    private final TileDef tile;

    public TileSwatch(TileDef tile)
    {
        this.tile = tile;
        setPreferredSize(new Dimension(DEFAULT_SIZE, DEFAULT_SIZE));
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        int size = Math.min(getWidth(), getHeight());

        if (tile.sprite != null)
        {
            g.drawImage(tile.sprite, 0, 0, size, size, null);
        }
        else
        {
            g.setColor(tile.colour);
            g.fillRect(0, 0, size, size);
        }

        g.setColor(Color.DARK_GRAY);
        g.drawRect(0, 0, size - 1, size - 1);
    }
}
