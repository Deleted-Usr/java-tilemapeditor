package com.willclay.mapeditor.menu;

import com.willclay.mapeditor.core.ui.Utils;
import com.willclay.mapeditor.core.EditorModel;
import com.willclay.mapeditor.core.ui.Window;
import com.willclay.mapeditor.menu.functions.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

public class EditorMenuBar
{
    private final Window window;

    private final JMenuBar bar = new JMenuBar();

    // Items
    private final JMenu fileMenu = new JMenu("File");
    private final JMenu editMenu = new JMenu("Edit");
    private final JMenu viewMenu = new JMenu("View");
    private final JMenu zoomMenu = new JMenu("Zoom");

    // Kept separate so they can be enabled/disabled and relabeled.
    private JMenuItem undoItem;
    private JMenuItem redoItem;

    // Item Functions
    private final FileMenuFunctions ff;
    private final EditMenuFunctions ef;
    private final ViewMenuFunctions vf;
    private final ZoomMenuFunctions zf;

    public EditorMenuBar(Window window)
    {
        this.window = window;

        ff = new FileMenuFunctions(window);
        ef = new EditMenuFunctions(window);
        vf = new ViewMenuFunctions(window);
        zf = new ZoomMenuFunctions(window);

        bar.add(setFileMenu());
        bar.add(setEditMenu());
        bar.add(setViewMenu());
        bar.add(setZoomMenu());
    }

    private JMenu setFileMenu()
    {
        Utils.addMenuItem(fileMenu, "New Map...", e -> ff.promptNewMap());

        fileMenu.addSeparator();

        Utils.addMenuItem(fileMenu, "Export Layers As char[][]...", e -> ff.exportLayers());
        Utils.addMenuItem(fileMenu, "Import char[][]....",          e -> ff.importLayers());

        fileMenu.addSeparator();

        Utils.addMenuItem(fileMenu, "Save Palette...", e -> ff.savePalette());
        Utils.addMenuItem(fileMenu, "Load Palette...", e -> ff.loadPalette());

        fileMenu.addSeparator();

        Utils.addMenuItem(fileMenu, "Quit...", e -> ff.quit());

        return fileMenu;
    }

    private JMenu setEditMenu()
    {
        int mod = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        undoItem = Utils.addMenuItem(editMenu, "Undo", KeyEvent.VK_Z, mod, e -> ef.undo());
        redoItem = Utils.addMenuItem(editMenu, "Redo", KeyEvent.VK_Y, mod, e -> ef.redo());

        editMenu.addSeparator();

        Utils.addMenuItem(editMenu, "Fill Active Layer with Selected Tile", e -> ef.fill());
        Utils.addMenuItem(editMenu, "Clear Active Layer",                   e -> ef.clear());

        editMenu.addSeparator();

        Utils.addMenuItem(editMenu, "ReSize Map...",e -> ef.promptResize());

        return editMenu;
    }

    private JMenu setViewMenu()
    {
        Utils.addCheckItem(viewMenu, "Show Grid",           true, vf::setShowGrid);
        Utils.addCheckItem(viewMenu, "Show Letters",        true, vf::setShowLetters);
        Utils.addCheckItem(viewMenu, "Dim Inactive Layers", true, vf::setDimInactiveLayers);

        viewMenu.addSeparator();

        Utils.addCheckItem(viewMenu, "Dark Mode", window.isDarkMode(), vf::setDarkMode);

        return viewMenu;
    }

    private JMenu setZoomMenu()
    {
        Utils.addMenuItem(zoomMenu, "Zoom In",    e -> zf.zoomIn());
        Utils.addMenuItem(zoomMenu, "Zoom Out",   e -> zf.zoomOut());
        Utils.addMenuItem(zoomMenu, "Reset Zoom", e -> zf.resetZoom());

        return zoomMenu;
    }

    /** Enable, disable and relabel the undo & redo items to match the history. */
    public void updateUndoState()
    {
        EditorModel model = window.getModel();

        setHistoryItem(undoItem, "Undo", model.canUndo());
        setHistoryItem(redoItem, "Redo", model.canRedo());
    }

    private static void setHistoryItem(JMenuItem item, String label, boolean available)
    {
        if (item == null) return;

        item.setEnabled(available);
        item.setText(available ? label : label + " (nothing)");
    }

    // Getters
    public JMenuBar getBar() { return bar; }
}