package com.willclay.mapeditor.dialogs;

import com.willclay.mapeditor.core.ui.Utils;
import com.willclay.mapeditor.core.EditorModel;
import com.willclay.mapeditor.domain.ParsedLayer;
import com.willclay.mapeditor.io.MapImporter;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.StringJoiner;

/**
 * Takes pasted Java source holding one or more char[][] arrays - the editor's
 * own export, or the project's TileMap.java - and imports them as layers.
 * Preview parses without applying, so the detected layer count and dimensions
 * can be confirmed first.
 */
public class ImportDialog extends JDialog
{
    private static final Dimension INPUT_SIZE = new Dimension(640, 360);
    private static final Color OK_COLOUR = new Color(40, 120, 40);

    private static final String HELP_TEXT =
            """
            Paste one or more char[][] arrays. Both quoted rows {'s','g',...} and
            compact rows {sg...} are accepted. Each array becomes a layer.""";

    private final EditorModel model;

    private final JTextArea input = Utils.createCodeArea(true);
    private final JLabel previewLabel = new JLabel("Paste source and press Preview.");

    private final JCheckBox sizeFromImport = new JCheckBox("Resize map to fit imported data", true);
    private final JCheckBox autoAddTiles = new JCheckBox("Add unknown letters to palette", true);

    private boolean imported = false;
    private Runnable onImported = () -> {};

    public ImportDialog(Window owner, EditorModel model)
    {
        super(owner, "Import char[][]", ModalityType.APPLICATION_MODAL);

        this.model = model;

        buildUI();

        pack();
        setLocationRelativeTo(owner);
    }

    private void buildUI()
    {
        JScrollPane inputScroll = new JScrollPane(input);
        inputScroll.setPreferredSize(INPUT_SIZE);

        JPanel centre = new JPanel(new BorderLayout());
        centre.add(inputScroll, BorderLayout.CENTER);
        centre.add(buildOptions(), BorderLayout.SOUTH);

        JButton importButton = Utils.createButton("Import", e -> doImport());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(Utils.createButton("Preview", e -> doPreview()));
        buttons.add(Utils.createButton("Cancel", e -> dispose()));
        buttons.add(importButton);

        setLayout(new BorderLayout());
        add(buildHeader(), BorderLayout.NORTH);
        add(centre, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(importButton);
    }

    private JPanel buildHeader()
    {
        JTextArea help = new JTextArea(HELP_TEXT);
        help.setEditable(false);
        help.setOpaque(false);
        help.setFont(help.getFont().deriveFont(11f));
        help.setForeground(Color.GRAY);
        help.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        JPanel loadWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        loadWrap.add(Utils.createButton("Load from File...", e -> loadFromFile()));

        JPanel header = new JPanel(new BorderLayout());
        header.add(help, BorderLayout.CENTER);
        header.add(loadWrap, BorderLayout.EAST);

        return header;
    }

    private JPanel buildOptions()
    {
        previewLabel.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        previewLabel.setFont(previewLabel.getFont().deriveFont(Font.BOLD));

        JPanel options = new JPanel(new GridLayout(0, 1));
        options.setBorder(BorderFactory.createEmptyBorder(6, 8, 0, 8));
        options.add(sizeFromImport);
        options.add(autoAddTiles);
        options.add(previewLabel);

        return options;
    }

    // Actions
    private void loadFromFile()
    {
        File file = Utils.chooseOpenFile(this);
        if (file == null) return;

        try
        {
            input.setText(Files.readString(file.toPath()));
            input.setCaretPosition(0);
            doPreview();
        }
        catch (IOException ex)
        {
            Utils.showError(this, "Could not read file:\n" + ex.getMessage());
        }
    }

    private void doPreview()
    {
        try
        {
            List<ParsedLayer> parsed = MapImporter.parseArrays(input.getText());

            StringJoiner joiner = new StringJoiner(",  ");
            for (ParsedLayer layer : parsed) { joiner.add(layer.describe()); }

            previewLabel.setForeground(OK_COLOUR);
            previewLabel.setText("<html>Found " + parsed.size()
                    + (parsed.size() == 1 ? " layer:  " : " layers:  ") + joiner + "</html>");
        }
        catch (IllegalArgumentException ex)
        {
            previewLabel.setForeground(Color.RED.darker());
            previewLabel.setText(ex.getMessage());
        }
    }

    private void doImport()
    {
        List<ParsedLayer> parsed;
        try
        {
            parsed = MapImporter.parseArrays(input.getText());
        }
        catch (IllegalArgumentException ex)
        {
            Utils.showWarning(this, "Nothing to import", ex.getMessage());
            return;
        }

        if (!confirmImport(parsed)) return;

        model.applyImportedLayers(parsed, sizeFromImport.isSelected(), autoAddTiles.isSelected());
        imported = true;
        onImported.run();
        dispose();
    }

    private boolean confirmImport(List<ParsedLayer> parsed)
    {
        // Warn about ragged rows the parser padded, so it isn't a surprise.
        // The flag is used rather than re-walking grids, since int layers leave
        // the char grid null.
        boolean ragged = false;
        for (ParsedLayer layer : parsed)
        {
            if (layer.padded) { ragged = true; break; }
        }

        String message = "Import " + parsed.size() + " layer(s)? This replaces all current layers."
                + (ragged ? "\n(Some rows were padded to a uniform width.)" : "");

        return JOptionPane.showConfirmDialog(this, message, "Confirm import",
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
    }

    // Getters
    public boolean wasImported() { return imported; }

    // Setters
    public void setOnImported(Runnable action) { this.onImported = action; }
}
