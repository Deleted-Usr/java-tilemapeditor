package com.willclay.mapeditor;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.willclay.mapeditor.core.ui.Theme;
import com.willclay.mapeditor.core.ui.Window;

import javax.swing.*;

/**
 * Tile Map Editor
 * --------------------------------------------------------------------------
 * A visual editor for building the char-based tile maps the platformer's
 * TileMap format expects, where each cell is a single char.
 *
 *  - Define a palette of tiles, each with a letter, name, category, numeric id
 *    and an optional sprite.
 *  - Paint onto a grid at a configurable tile size and map dimension.
 *  - Work across multiple layers; every layer is its own char grid.
 *  - Export each layer as a Java 2D array, plus loader snippets for props and
 *    markers.
 */

// Anytime "the platformer" or "the game" in mentioned, it is in reference to a separate
// project this application was built for (A 2D Side-Scrolling Platformer).
public class Main
{
    private static final String TITLE = "Tile Map Editor";

    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(() ->
        {
            applySystemLookAndFeel();
            //Theme.apply(false); // Initialise the light-mode defaults.

            Window w = new Window(TITLE);
            w.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            w.setSize(Window.WINDOW_SIZE);
            w.setLocationRelativeTo(null);

            w.setVisible(true);
        });
    }

    private static void applySystemLookAndFeel()
    {
        try
        {
            //UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

			FlatDarculaLaf.setup();
        }
        catch (Exception ignored)
        {
            // The cross-platform default is fine if the system L&F is unavailable.
        }
    }
}