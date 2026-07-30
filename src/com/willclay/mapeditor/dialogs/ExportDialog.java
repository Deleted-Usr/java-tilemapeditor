package com.willclay.mapeditor.dialogs;

import com.willclay.mapeditor.core.ui.Utils;
import com.willclay.mapeditor.core.EditorModel;
import com.willclay.mapeditor.io.LoaderExporter;
import com.willclay.mapeditor.io.MapExporter;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Shows the two generated sections stacked:
 *   1. One array per layer (the map data).
 *   2. Loader snippets that populate a List of objects per Prop/Marker layer.
 *
 * Each section copies on its own; the pair can also be saved to one .java file.
 */
public class ExportDialog extends JDialog
{
    private static final String DEFAULT_FILE_NAME = "MapData.java";
    private static final Dimension PREFERRED_SIZE = new Dimension(760, 620);
    private static final double ARRAYS_WEIGHT = 0.55;

    private final EditorModel model;

    private final JTextArea arraysArea = Utils.createCodeArea(false);
    private final JTextArea loadersArea = Utils.createCodeArea(false);

    private final JCheckBox quoteBox = new JCheckBox("Quoted chars  {'g','d',...}", true);
    private final JTextField prefixField = new JTextField(12);

    public ExportDialog(Window owner, EditorModel model)
    {
        super(owner, "Export", ModalityType.APPLICATION_MODAL);

        this.model = model;

        buildUI();
        regenerate();

        pack();
        setLocationRelativeTo(owner);
    }

    private void buildUI()
    {
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                buildSection("Map data  -  one array per layer", arraysArea),
                buildSection("Object loaders  -  one List per Prop/Marker layer", loadersArea));
        split.setResizeWeight(ARRAYS_WEIGHT);
        split.setPreferredSize(PREFERRED_SIZE);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(Utils.createButton("Save All to .java...", e -> saveToFile()));
        buttons.add(Utils.createButton("Close", e -> dispose()));

        setLayout(new BorderLayout());
        add(buildOptions(), BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
    }

    private JPanel buildOptions()
    {
        JPanel options = new JPanel(new FlowLayout(FlowLayout.LEFT));

        quoteBox.addActionListener(e -> regenerate());
        prefixField.addActionListener(e -> regenerate()); // Regenerate on Enter.

        options.add(quoteBox);
        options.add(new JLabel("   Name prefix:"));
        options.add(prefixField);
        options.add(Utils.createButton("Regenerate", e -> regenerate()));

        return options;
    }

    private JPanel buildSection(String title, JTextArea area)
    {
        JLabel label = new JLabel(title);
        label.setFont(label.getFont().deriveFont(Font.BOLD));

        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(6, 8, 4, 8));
        header.add(label, BorderLayout.WEST);
        header.add(Utils.createButton("Copy", e -> copy(area)), BorderLayout.EAST);

        JPanel section = new JPanel(new BorderLayout());
        section.add(header, BorderLayout.NORTH);
        section.add(new JScrollPane(area), BorderLayout.CENTER);

        return section;
    }

    private static void copy(JTextArea area)
    {
        area.selectAll();
        area.copy();
        area.select(0, 0);
    }

    private void regenerate()
    {
        String prefix = prefixField.getText().trim();

        setText(arraysArea, MapExporter.exportAsJava(model, quoteBox.isSelected(), prefix));
        setText(loadersArea, LoaderExporter.exportLoaders(model, prefix));
    }

    private static void setText(JTextArea area, String text)
    {
        area.setText(text);
        area.setCaretPosition(0);
    }

    private void saveToFile()
    {
        File file = Utils.chooseSaveFile(this, DEFAULT_FILE_NAME);
        if (file == null) return;

        try (PrintWriter out = new PrintWriter(new FileWriter(file)))
        {
            out.print(arraysArea.getText());
            out.println();
            out.print(loadersArea.getText());

            JOptionPane.showMessageDialog(this, "Saved to " + file.getName());
        }
        catch (IOException ex)
        {
            Utils.showError(this, "Failed to save:\n" + ex.getMessage());
        }
    }
}
