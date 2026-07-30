package com.willclay.mapeditor.dialogs;

import com.willclay.mapeditor.core.ui.Utils;
import com.willclay.mapeditor.core.EditorModel;
import com.willclay.mapeditor.domain.Layer;
import com.willclay.mapeditor.domain.TileDef;
import com.willclay.mapeditor.panels.TileSwatch;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Creates or edits a single tile definition: letter, name, category, id, an
 * optional layer restriction, a fallback colour and an optional sprite.
 */
public class TileDialog extends JDialog
{
    private static final String ANY_LAYER = "(any layer)";
    private static final int MAX_ID = 99999;
    private static final Color DEFAULT_COLOUR = new Color(120, 120, 120);
    private static final Dimension PREVIEW_SIZE = new Dimension(48, 48);

    private final EditorModel model;
    private final TileDef editing; // null when adding

    private final JTextField letterField = new JTextField(2);
    private final JTextField nameField = new JTextField(14);
    private final JComboBox<TileDef.Category> categoryBox = new JComboBox<>(TileDef.Category.values());
    private final JSpinner idSpinner = new JSpinner(new SpinnerNumberModel(1, 1, MAX_ID, 1));
    private final JComboBox<String> layerBox = new JComboBox<>();

    private Color chosenColour = DEFAULT_COLOUR;
    private BufferedImage chosenSprite = null;
    private String chosenSpritePath = null;

    private final TileDef previewTile;
    private final TileSwatch previewSwatch;

    private boolean accepted = false;

    public TileDialog(Window owner, EditorModel model, TileDef editing)
    {
        super(owner, editing == null ? "Add Tile" : "Edit Tile", ModalityType.APPLICATION_MODAL);

        this.model = model;
        this.editing = editing;

        populateLayerBox();
        if (editing != null) loadFrom(editing);
        else idSpinner.setValue(model.nextFreeId());

        previewTile = new TileDef(currentLetter(), "", TileDef.Category.TERRAIN, chosenColour);
        previewTile.sprite = chosenSprite;
        previewSwatch = new TileSwatch(previewTile);

        buildUI();

        pack();
        setLocationRelativeTo(owner);
    }

    private void populateLayerBox()
    {
        layerBox.addItem(ANY_LAYER);
        for (Layer layer : model.getLayers()) { layerBox.addItem(layer.name); }
    }

    private void loadFrom(TileDef tile)
    {
        letterField.setText(String.valueOf(tile.letter));
        nameField.setText(tile.name);
        categoryBox.setSelectedItem(tile.category);
        idSpinner.setValue(Math.max(1, tile.id));
        layerBox.setSelectedItem(tile.layerName != null ? tile.layerName : ANY_LAYER);

        chosenColour = tile.colour;
        chosenSprite = tile.sprite;
        chosenSpritePath = tile.spritePath;
    }

    private void buildUI()
    {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.anchor = GridBagConstraints.WEST;

        int row = 0;
        addRow(form, gc, row++, "Letter:", letterField, "(single character key for this tile)");
        addWideRow(form, gc, row++, "Name:", nameField);
        addRow(form, gc, row++, "Category:", categoryBox, null);
        addRow(form, gc, row++, "ID:", idSpinner, "(value used in int[][] layers; 0 = empty)");
        addRow(form, gc, row++, "Layer:", layerBox, "(restrict tile to one layer; frees its letter for reuse)");
        addRow(form, gc, row++, "Colour:", Utils.createButton("Pick Colour...", e -> pickColour()), null);
        addSpriteRow(form, gc, row++);

        previewSwatch.setPreferredSize(PREVIEW_SIZE);
        addRow(form, gc, row, "Preview:", previewSwatch, null);

        // Live-update the preview letter as the user types.
        letterField.getDocument().addDocumentListener(new SimpleDocListener(this::refreshPreview));

        JButton ok = Utils.createButton(editing == null ? "Add" : "Save", e -> onOk());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(Utils.createButton("Cancel", e -> dispose()));
        buttons.add(ok);

        setLayout(new BorderLayout());
        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(ok);
    }

    private static void addRow(JPanel form, GridBagConstraints gc, int row, String label, Component field, String hint)
    {
        gc.gridx = 0; gc.gridy = row;
        form.add(new JLabel(label), gc);

        gc.gridx = 1;
        form.add(field, gc);

        if (hint == null) return;

        gc.gridx = 2;
        form.add(Utils.createHintLabel(hint), gc);
    }

    private static void addWideRow(JPanel form, GridBagConstraints gc, int row, String label, Component field)
    {
        gc.gridx = 0; gc.gridy = row;
        form.add(new JLabel(label), gc);

        gc.gridx = 1; gc.gridwidth = 2;
        form.add(field, gc);
        gc.gridwidth = 1;
    }

    private void addSpriteRow(JPanel form, GridBagConstraints gc, int row)
    {
        gc.gridx = 0; gc.gridy = row;
        form.add(new JLabel("Sprite:"), gc);

        gc.gridx = 1;
        form.add(Utils.createButton("Load Sprite...", e -> loadSprite()), gc);

        gc.gridx = 2;
        form.add(Utils.createButton("Clear", e -> clearSprite()), gc);
    }

    // Actions
    private void pickColour()
    {
        Color picked = JColorChooser.showDialog(this, "Tile Colour", chosenColour);
        if (picked == null) return;

        chosenColour = picked;
        refreshPreview();
    }

    private void loadSprite()
    {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Images (png, jpg, gif, bmp)",
                "png", "jpg", "jpeg", "gif", "bmp"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        try
        {
            BufferedImage image = ImageIO.read(file);
            if (image == null) throw new IOException("Unsupported image format");

            chosenSprite = image;
            chosenSpritePath = file.getAbsolutePath();
            refreshPreview();
        }
        catch (IOException ex)
        {
            Utils.showError(this, "Could not load image:\n" + ex.getMessage());
        }
    }

    private void clearSprite()
    {
        chosenSprite = null;
        chosenSpritePath = null;
        refreshPreview();
    }

    private void refreshPreview()
    {
        previewTile.colour = chosenColour;
        previewTile.sprite = chosenSprite;
        previewTile.letter = currentLetter();

        previewSwatch.repaint();
    }

    private void onOk()
    {
        if (letterField.getText().isEmpty())
        {
            Utils.showWarning(this, "Missing letter", "Please enter a letter for this tile.");
            return;
        }

        TileDef candidate = buildCandidate();

        // The model's rule decides: duplicate letters are only allowed when
        // every holder is pinned to a different layer.
        String problem = model.validateTile(candidate, editing);
        if (problem != null)
        {
            Utils.showWarning(this, "Letter conflict", problem);
            return;
        }

        if (editing == null) model.addTile(candidate);
        else copyInto(editing, candidate);

        accepted = true;
        dispose();
    }

    private TileDef buildCandidate()
    {
        char letter = currentLetter();
        String name = nameField.getText().trim();
        if (name.isEmpty()) name = "Tile " + letter;

        TileDef candidate = new TileDef(letter, name,
                (TileDef.Category) categoryBox.getSelectedItem(), chosenColour);

        String selectedLayer = (String) layerBox.getSelectedItem();
        candidate.layerName = ANY_LAYER.equals(selectedLayer) ? null : selectedLayer;
        candidate.id = (Integer) idSpinner.getValue();
        candidate.sprite = chosenSprite;
        candidate.spritePath = chosenSpritePath;

        return candidate;
    }

    /** Edit in place, so anything already holding this tile keeps its reference. */
    private static void copyInto(TileDef target, TileDef source)
    {
        target.letter = source.letter;
        target.name = source.name;
        target.category = source.category;
        target.id = source.id;
        target.layerName = source.layerName;
        target.colour = source.colour;
        target.sprite = source.sprite;
        target.spritePath = source.spritePath;
    }

    private char currentLetter()
    {
        String text = letterField.getText();

        return text.isEmpty() ? '?' : text.charAt(0);
    }

    /** Shows the dialog and returns true when the tile was added or saved. */
    public boolean showDialog()
    {
        setVisible(true);

        return accepted;
    }

    /** Runs one action for any kind of document change. */
    private record SimpleDocListener(Runnable action) implements DocumentListener
    {
        @Override public void insertUpdate(DocumentEvent e) { action.run(); }
        @Override public void removeUpdate(DocumentEvent e) { action.run(); }
        @Override public void changedUpdate(DocumentEvent e) { action.run(); }
    }
}
