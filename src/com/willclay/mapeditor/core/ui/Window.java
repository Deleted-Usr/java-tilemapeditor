package com.willclay.mapeditor.core.ui;

import com.willclay.mapeditor.core.EditorModel;
import com.willclay.mapeditor.menu.EditorMenuBar;
import com.willclay.mapeditor.panels.LayerPanel;
import com.willclay.mapeditor.panels.MapCanvasPanel;
import com.willclay.mapeditor.panels.PalettePanel;

import javax.swing.*;
import java.awt.*;

/**
 * The editor window. Owns the model and the three panels, and is the single
 * place the menu functions reach through to refresh the UI.
 */
public class Window extends JFrame
{
    public static final Dimension WINDOW_SIZE = new Dimension(1700, 820);
    private static final int PALETTE_PANEL_WIDTH = 320;
    private static final int LAYER_PANEL_WIDTH = 280;
    private static final int SCROLL_UNIT = 24;

    private final EditorModel model = new EditorModel();

    private final MapCanvasPanel mapCanvasPanel = new MapCanvasPanel(model);
    private final PalettePanel palettePanel = new PalettePanel(model);
    private final LayerPanel layerPanel = new LayerPanel(model);

    private final JScrollPane canvasScroll = new JScrollPane(mapCanvasPanel);

    private final JLabel statusLabel = new JLabel("Ready!");

    private final EditorMenuBar menuBar;

    private boolean darkMode = false;

    public Window(String title)
    {
        super(title);

        menuBar = new EditorMenuBar(this);
        setJMenuBar(menuBar.getBar());

        buildLayout();
        wireCallbacks();

        // Start on a default map so the canvas isn't empty.
        model.newMap();
        palettePanel.seedDefaultPalette();
        refreshAll();
    }

    private void buildLayout()
    {
        palettePanel.setPreferredSize(new Dimension(PALETTE_PANEL_WIDTH,0));
        layerPanel.setPreferredSize(new Dimension(LAYER_PANEL_WIDTH,0));

        canvasScroll.getVerticalScrollBar().setUnitIncrement(SCROLL_UNIT);
        canvasScroll.getHorizontalScrollBar().setUnitIncrement(SCROLL_UNIT);

        JSplitPane centreRight = Utils.createSplitPane(canvasScroll, layerPanel, 1.0);
        JSplitPane root = Utils.createSplitPane(palettePanel, centreRight, 0.0);

        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createEmptyBorder(3,8,3,8));
        statusBar.add(statusLabel, BorderLayout.WEST);

        add(root, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);
    }

    private void wireCallbacks()
    {
        model.addListener(this::onModelChanged);

        mapCanvasPanel.setStatusSink(this::setStatus);
        palettePanel.setOnPaletteChanged(mapCanvasPanel::repaint);
        layerPanel.setOnLayersChanged(mapCanvasPanel::repaint);
    }

    /** The model fires this on structural changes: resync and repaint. */
    private void onModelChanged()
    {
        layerPanel.rebuild();
        resizeCanvas();
        menuBar.updateUndoState();
    }

    /** Full rebuild, for when the palette may have changed as well. */
    public void refreshAll()
    {
        palettePanel.rebuild();
        layerPanel.rebuild();
        resizeCanvas();
        menuBar.updateUndoState();
    }

    private void resizeCanvas()
    {
        mapCanvasPanel.recomputePreferredSize();
        mapCanvasPanel.revalidate();
        mapCanvasPanel.repaint();
    }

    public void setDarkMode(boolean on)
    {
        darkMode = on;
        Theme.apply(on);

        System.out.println(getWidth() + "x" + getHeight());

        // updateComponentTreeUI restyles everything but drops our custom
        // backgrounds, so those are reasserted and the custom-painted panels
        // rebuilt afterwards.
        SwingUtilities.updateComponentTreeUI(this);
        getContentPane().setBackground(Theme.bg());
        statusLabel.setForeground(Theme.fg());

        mapCanvasPanel.refreshTheme();
        palettePanel.rebuild();
        layerPanel.rebuild();

        setStatus(on ? "Dark mode on" : "Dark mode off");
    }

    public void setStatus(String s) { statusLabel.setText(s); }

    // Getters
    public EditorModel getModel() { return model; }
    public MapCanvasPanel getMapCanvasPanel() { return mapCanvasPanel; }
    public PalettePanel getPalettePanel() { return palettePanel; }
    public LayerPanel getLayerPanel() { return layerPanel; }
    public boolean isDarkMode() { return darkMode; }
}