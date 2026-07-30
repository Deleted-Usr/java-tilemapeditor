package com.willclay.mapeditor.panels;

import com.willclay.mapeditor.core.ui.Theme;
import com.willclay.mapeditor.core.ui.Utils;
import com.willclay.mapeditor.core.EditorModel;
import com.willclay.mapeditor.dialogs.TileDialog;
import com.willclay.mapeditor.domain.TileDef;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Left-side panel listing every tile in the palette. Each row shows its
 * swatch, name, letter and category; clicking one makes it the active paint
 * tile. The row buttons reorder, edit and remove tiles.
 */
public class PalettePanel extends JPanel
{
    private static final int ROW_HEIGHT = 52;
    private static final int SCROLL_UNIT = 16;
    private static final Color TRANSPARENT = new Color(0, 0, 0, 0);

    private final EditorModel model;
    private final JPanel listPanel = new JPanel();

    private Runnable onPaletteChanged = () -> {};

    public PalettePanel(EditorModel model)
    {
        this.model = model;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Tile Palette"));

        add(Utils.createListScroll(listPanel, SCROLL_UNIT), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);
    }

    private JPanel buildButtons()
    {
        JPanel buttons = new JPanel(new GridLayout(0, 1, 4, 4));

        buttons.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        buttons.add(Utils.createButton("Add Tile...", e -> showTileDialog(null)));

        return buttons;
    }

    public void rebuild()
    {
        listPanel.removeAll();

        for (TileDef tile : model.getPalette())
        {
            listPanel.add(new TileRow(tile));
            listPanel.add(Box.createVerticalStrut(2));
        }

        listPanel.revalidate();
        listPanel.repaint();
    }

    /** Match the platformer's existing tile letters so exports drop in cleanly. */
    public void seedDefaultPalette()
    {
        if (!model.getPalette().isEmpty()) return;

        seed('s', "Sky",         TileDef.Category.BACKGROUND, new Color(135, 206, 235));
        seed('g', "Ground",      TileDef.Category.TERRAIN,    new Color(34, 139, 34));
        seed('d', "Dirt",        TileDef.Category.TERRAIN,    new Color(160, 82, 45));
        seed('r', "Rock",        TileDef.Category.TERRAIN,    new Color(128, 128, 128));
        seed('c', "Cloud",       TileDef.Category.PROP,       new Color(245, 245, 245));
        seed('p', "Pickup",      TileDef.Category.MARKER,     new Color(255, 215, 0));
        seed('E', "Enemy Spawn", TileDef.Category.MARKER,     new Color(220, 40, 40));
        seed('X', "Exit",        TileDef.Category.MARKER,     new Color(40, 120, 220));

        model.setSelectedLetter('g');
        rebuild();
    }

    private void seed(char letter, String name, TileDef.Category category, Color colour)
    {
        TileDef tile = new TileDef(letter, name, category, colour);
        tile.id = model.nextFreeId(); // Ids are used by int[][] layers; 0 is empty.

        model.addTile(tile);
    }

    private void showTileDialog(TileDef existing)
    {
        TileDialog dialog = new TileDialog(SwingUtilities.getWindowAncestor(this), model, existing);

        if (dialog.showDialog()) { refresh(); }
    }

    private void refresh()
    {
        rebuild();
        onPaletteChanged.run();
    }

    /** A single clickable row in the palette list. */
    private class TileRow extends JPanel
    {
        private final TileDef tile;

        TileRow(TileDef tile)
        {
            this.tile = tile;

            setLayout(new BorderLayout(8, 0));
            setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_HEIGHT));

            add(new TileSwatch(tile), BorderLayout.WEST);
            add(buildLabels(), BorderLayout.CENTER);
            add(buildActions(), BorderLayout.EAST);

            updateSelectedLook();
            addMouseListener(new MouseAdapter()
            {
                @Override public void mousePressed(MouseEvent e) { select(); }
            });
        }

        private JPanel buildLabels()
        {
            JPanel text = new JPanel();
            text.setOpaque(false);
            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

            JLabel nameLabel = new JLabel(tile.name + "  '" + tile.letter + "'  #" + tile.id);
            nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));

            String restriction = tile.layerName != null ? "  \u2022 " + tile.layerName + " only" : "";
            JLabel categoryLabel = Utils.createHintLabel(tile.category.name().toLowerCase() + restriction);

            text.add(nameLabel);
            text.add(categoryLabel);

            return text;
        }

        private JPanel buildActions()
        {
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
            actions.setOpaque(false);

            actions.add(Utils.createSmallButton("\u25B2", "Move up", e -> move(-1)));
            actions.add(Utils.createSmallButton("\u25BC", "Move down", e -> move(+1)));
            actions.add(Utils.createSmallButton("Edit", "Edit this tile", e -> showTileDialog(tile)));
            actions.add(Utils.createSmallButton("X", "Remove this tile", e -> remove()));

            return actions;
        }

        private void move(int direction)
        {
            model.movePaletteTile(tile, direction);
            refresh();

            System.out.println(getWidth() + "x" + getHeight());
        }

        private void remove()
        {
            model.removeTile(tile);
            refresh();
        }

        private void select()
        {
            model.setSelectedTile(tile); // By identity, since duplicate letters are allowed.

            for (Component component : listPanel.getComponents())
            {
                if (component instanceof TileRow row) row.updateSelectedLook();
            }
        }

        void updateSelectedLook()
        {
            boolean selected = model.getSelectedTile() == tile;

            setBackground(selected ? Theme.select() : TRANSPARENT);
            setOpaque(selected);
            repaint();
        }
    }

    // Setters
    public void setOnPaletteChanged(Runnable action) { this.onPaletteChanged = action; }
}
