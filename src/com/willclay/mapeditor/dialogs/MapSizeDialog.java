package com.willclay.mapeditor.dialogs;

import com.willclay.mapeditor.core.ui.Utils;

import javax.swing.*;
import java.awt.*;

/**
 * Captures map dimensions and tile size. Used for both "New Map" and
 * "Resize Map", which differ only in the title and what the caller does with
 * the values.
 */
public class MapSizeDialog extends JDialog
{
    private static final int MAX_DIMENSION = 1000;
    private static final int MIN_TILE_SIZE = 4;
    private static final int MAX_TILE_SIZE = 512;

    private final JSpinner colsSpinner;
    private final JSpinner rowsSpinner;
    private final JSpinner tileSpinner;

    private boolean accepted = false;

    public MapSizeDialog(Window owner, String title, int cols, int rows, int tileSize)
    {
        super(owner, title, ModalityType.APPLICATION_MODAL);

        colsSpinner = new JSpinner(new SpinnerNumberModel(cols, 1, MAX_DIMENSION, 1));
        rowsSpinner = new JSpinner(new SpinnerNumberModel(rows, 1, MAX_DIMENSION, 1));
        tileSpinner = new JSpinner(new SpinnerNumberModel(tileSize, MIN_TILE_SIZE, MAX_TILE_SIZE, 1));

        buildUI();

        pack();
        setLocationRelativeTo(owner);
    }

    private void buildUI()
    {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(5, 5, 5, 5);
        gc.anchor = GridBagConstraints.WEST;

        addRow(form, gc, 0, "Width (columns):", colsSpinner);
        addRow(form, gc, 1, "Height (rows):", rowsSpinner);
        addRow(form, gc, 2, "Tile size (px):", tileSpinner);

        JButton ok = Utils.createButton("OK", e -> { accepted = true; dispose(); });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(Utils.createButton("Cancel", e -> dispose()));
        buttons.add(ok);

        setLayout(new BorderLayout());
        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(ok);
    }

    private static void addRow(JPanel form, GridBagConstraints gc, int row, String label, JSpinner spinner)
    {
        gc.gridx = 0; gc.gridy = row;
        form.add(new JLabel(label), gc);

        gc.gridx = 1;
        form.add(spinner, gc);
    }

    /** Shows the dialog and returns true when the user accepted it. */
    public boolean showDialog()
    {
        setVisible(true);

        return accepted;
    }

    // Getters
    public int getCols() { return (Integer) colsSpinner.getValue(); }
    public int getRows() { return (Integer) rowsSpinner.getValue(); }
    public int getTileSize() { return (Integer) tileSpinner.getValue(); }
}
