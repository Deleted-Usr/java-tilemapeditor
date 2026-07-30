package com.willclay.mapeditor.panels;

import com.willclay.mapeditor.core.ui.Theme;
import com.willclay.mapeditor.core.ui.Utils;
import com.willclay.mapeditor.core.EditorModel;
import com.willclay.mapeditor.domain.Layer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Right-side panel managing the layer stack: add, remove, rename, reorder,
 * toggle visibility, flip export type, and choose which layer painting affects.
 */
public class LayerPanel extends JPanel
{
    private static final int ROW_HEIGHT = 40;
    private static final int SCROLL_UNIT = 16;

    private final EditorModel model;
    private final JPanel listPanel = new JPanel();

    private Runnable onLayersChanged = () -> {};

    public LayerPanel(EditorModel model)
    {
        this.model = model;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Layers"));

        add(Utils.createListScroll(listPanel, SCROLL_UNIT), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);
    }

    private JPanel buildButtons()
    {
        JPanel buttons = new JPanel(new GridLayout(0, 2, 4, 4));
        buttons.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        buttons.add(Utils.createButton("Add", e -> addLayer()));
        buttons.add(Utils.createButton("Remove", e -> removeActiveLayer()));
        buttons.add(Utils.createButton("Move Up", e -> moveActiveLayer(-1)));
        buttons.add(Utils.createButton("Move Down", e -> moveActiveLayer(+1)));

        return buttons;
    }

    public void rebuild()
    {
        listPanel.removeAll();

        // Topmost layer first, so it reads like an image editor's layer stack.
        List<Layer> layers = model.getLayers();
        for (int i = layers.size() - 1; i >= 0; i--)
        {
            listPanel.add(new LayerRow(i, layers.get(i)));
            listPanel.add(Box.createVerticalStrut(2));
        }

        listPanel.revalidate();
        listPanel.repaint();
    }

    private void addLayer()
    {
        String suggested = "Layer " + (model.getLayers().size() + 1);
        String name = JOptionPane.showInputDialog(this, "Layer name:", suggested);
        if (name == null || name.trim().isEmpty()) return;

        model.addLayer(name.trim());
        refresh();
    }

    private void removeActiveLayer()
    {
        model.removeLayer(model.getActiveLayerIndex());
        refresh();
    }

    private void moveActiveLayer(int direction)
    {
        int from = model.getActiveLayerIndex();
        int to = from + direction;
        if (to < 0 || to >= model.getLayers().size()) return;

        model.moveLayer(from, to);
        refresh();
    }

    private void refresh()
    {
        rebuild();
        onLayersChanged.run();
    }

    private class LayerRow extends JPanel
    {
        private final int index;
        private final Layer layer;

        LayerRow(int index, Layer layer)
        {
            this.index = index;
            this.layer = layer;

            setLayout(new BorderLayout(6, 0));
            setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_HEIGHT));

            boolean active = (index == model.getActiveLayerIndex());
            setOpaque(active);
            setBackground(active ? Theme.select() : null);

            add(buildVisibilityBox(), BorderLayout.WEST);
            add(buildNameLabel(active), BorderLayout.CENTER);
            add(buildActions(), BorderLayout.EAST);

            addMouseListener(new MouseAdapter()
            {
                @Override public void mousePressed(MouseEvent e) { activate(); }
            });
        }

        private JCheckBox buildVisibilityBox()
        {
            JCheckBox visible = new JCheckBox("", layer.visible);

            visible.setOpaque(false);
            visible.setToolTipText("Visible");
            visible.addActionListener(e ->
            {
                layer.visible = visible.isSelected();
                onLayersChanged.run();
            });

            return visible;
        }

        private JLabel buildNameLabel(boolean active)
        {
            JLabel name = new JLabel(layer.name);
            name.setFont(name.getFont().deriveFont(active ? Font.BOLD : Font.PLAIN));

            // Clicking the label should activate the layer too.
            name.addMouseListener(new MouseAdapter()
            {
                @Override public void mousePressed(MouseEvent e) { activate(); }
            });

            return name;
        }

        private JPanel buildActions()
        {
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
            actions.setOpaque(false);

            actions.add(Utils.createSmallButton(layer.isIntType() ? "int" : "char",
                    "Export type: click to toggle char[][] / int[][]", e -> toggleType()));
            actions.add(Utils.createSmallButton("R", "Rename", e -> rename()));

            return actions;
        }

        private void toggleType()
        {
            model.toggleLayerType(index);
            refresh();
        }

        private void rename()
        {
            String name = JOptionPane.showInputDialog(this, "Rename layer:", layer.name);
            if (name == null || name.trim().isEmpty()) return;

            model.renameLayer(index, name); // Keeps tile restrictions in sync.
            refresh();
        }

        private void activate()
        {
            model.setActiveLayerIndex(index);
            refresh();
        }
    }

    // Setters
    public void setOnLayersChanged(Runnable action) { this.onLayersChanged = action; }
}
