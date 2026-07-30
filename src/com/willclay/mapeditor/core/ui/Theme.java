package com.willclay.mapeditor.core.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Centralizes light/dark theming. {@link #apply(boolean)} installs a set of
 * UIManager color defaults so that a subsequent
 * SwingUtilities.updateComponentTreeUI() restyles the whole component tree.
 * The canvas reads its backdrop/grid/text colors from the static accessors
 * here so it matches the surrounding chrome.
 *
 * This is a lightweight approach (no external look-and-feel dependency): it
 * overrides the common keys most Swing components consult. It won't be pixel
 * perfect across every L&F, but gives a coherent dark palette.
 */
public final class Theme
{
    private Theme() {}

    private static boolean dark = false;

    // ---- Palette ---------------------------------------------------------
    // Light
    private static final Color L_BG       = new Color(238, 238, 238);
    private static final Color L_PANEL    = new Color(245, 245, 245);
    private static final Color L_FG       = new Color(20, 20, 20);
    private static final Color L_FIELD    = Color.WHITE;
    private static final Color L_SELECT   = new Color(80, 130, 200);
    private static final Color L_GRID     = new Color(0, 0, 0, 70);
    private static final Color L_CANVAS_A = new Color(54, 54, 60);
    private static final Color L_CANVAS_B = new Color(48, 48, 54);

    // Dark
    private static final Color D_BG       = new Color(43, 43, 48);
    private static final Color D_PANEL    = new Color(50, 50, 56);
    private static final Color D_FG       = new Color(225, 225, 228);
    private static final Color D_FIELD    = new Color(60, 60, 66);
    private static final Color D_SELECT   = new Color(70, 110, 175);
    private static final Color D_GRID     = new Color(255, 255, 255, 40);
    private static final Color D_CANVAS_A = new Color(34, 34, 39);
    private static final Color D_CANVAS_B = new Color(29, 29, 34);

    public static boolean isDark() { return dark; }

    public static Color bg()       { return dark ? D_BG : L_BG; }
    public static Color panel()    { return dark ? D_PANEL : L_PANEL; }
    public static Color fg()       { return dark ? D_FG : L_FG; }
    public static Color field()    { return dark ? D_FIELD : L_FIELD; }
    public static Color select()   { return dark ? D_SELECT : L_SELECT; }
    public static Color grid()     { return dark ? D_GRID : L_GRID; }
    public static Color canvasA()  { return dark ? D_CANVAS_A : L_CANVAS_A; }
    public static Color canvasB()  { return dark ? D_CANVAS_B : L_CANVAS_B; }

    /**
     * Install UIManager color defaults for the chosen mode. Call before
     * SwingUtilities.updateComponentTreeUI(window) to restyle everything.
     */
    public static void apply(boolean darkMode)
    {
        dark = darkMode;

        Color bg = bg(), panel = panel(), fg = fg(), field = field(), select = select();
        Color selectText = Color.WHITE; // Readable on both selection colours.

        // Generic surfaces
        put("Panel.background", panel);
        put("OptionPane.background", panel);
        put("OptionPane.messageForeground", fg);
        put("Label.foreground", fg);
        put("Label.background", panel);

        // Menus
        put("MenuBar.background", bg);
        put("MenuBar.foreground", fg);
        put("Menu.background", bg);
        put("Menu.foreground", fg);
        put("Menu.selectionBackground", select);
        put("Menu.selectionForeground", selectText);
        put("MenuItem.background", bg);
        put("MenuItem.foreground", fg);
        put("MenuItem.selectionBackground", select);
        put("MenuItem.selectionForeground", selectText);
        put("CheckBoxMenuItem.background", bg);
        put("CheckBoxMenuItem.foreground", fg);
        put("CheckBoxMenuItem.selectionBackground", select);
        put("CheckBoxMenuItem.selectionForeground", selectText);
        put("PopupMenu.background", bg);
        put("PopupMenu.foreground", fg);
        put("Separator.foreground", darkMode ? new Color(80, 80, 86) : new Color(200, 200, 200));

        // Buttons
        put("Button.background", field);
        put("Button.foreground", fg);
        put("ToggleButton.background", field);
        put("ToggleButton.foreground", fg);

        // Text components
        put("TextField.background", field);
        put("TextField.foreground", fg);
        put("TextField.caretForeground", fg);
        put("TextArea.background", field);
        put("TextArea.foreground", fg);
        put("TextArea.caretForeground", fg);
        put("TextField.inactiveForeground", darkMode ? new Color(150,150,150) : Color.GRAY);

        // Spinners / combos
        put("Spinner.background", field);
        put("Spinner.foreground", fg);
        put("ComboBox.background", field);
        put("ComboBox.foreground", fg);
        put("ComboBox.selectionBackground", select);
        put("ComboBox.selectionForeground", selectText);

        // Checkboxes
        put("CheckBox.background", panel);
        put("CheckBox.foreground", fg);

        // Scroll panes / viewports / lists
        put("ScrollPane.background", panel);
        put("Viewport.background", panel);
        put("List.background", field);
        put("List.foreground", fg);
        put("List.selectionBackground", select);
        put("List.selectionForeground", selectText);

        // Split panes & borders
        put("SplitPane.background", bg);
        put("TitledBorder.titleColor", fg);

        // Tooltips
        put("ToolTip.background", field);
        put("ToolTip.foreground", fg);
    }

    private static void put(String key, Color c)
    {
        // Wrap in ColorUIResource so L&F treats it as a replaceable default.
        UIManager.put(key, new javax.swing.plaf.ColorUIResource(c));
    }
}