package com.willclay.mapeditor.core.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.function.Consumer;

public class Utils
{
    private static final Insets SMALL_BUTTON_INSETS = new Insets(1, 4, 1, 4);
    private static final float SMALL_BUTTON_FONT_SIZE = 10f;
    private static final float HINT_FONT_SIZE = 11f;

    // Menus
    public static JMenuItem addMenuItem(JMenu menu, String title, int keyCode, int modifiers, ActionListener actionListener)
    {
        JMenuItem item = new JMenuItem(title);

        item.setAccelerator(KeyStroke.getKeyStroke(keyCode, modifiers));
        item.addActionListener(actionListener);
        menu.add(item);

        return item;
    }

    public static JMenuItem addMenuItem(JMenu menu, String title, ActionListener listener)
    {
        JMenuItem item = new JMenuItem(title);

        item.addActionListener(listener);
        menu.add(item);

        return item;
    }

    public static JCheckBoxMenuItem addCheckItem(JMenu menu, String title, boolean selected, Consumer<Boolean> action)
    {
        JCheckBoxMenuItem item = new JCheckBoxMenuItem(title, selected);

        item.addActionListener(e -> action.accept(item.isSelected()));
        menu.add(item);

        return item;
    }

    // Components
    public static JSplitPane createSplitPane(Component left, Component right, double resizeWeight)
    {
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);

        splitPane.setResizeWeight(resizeWeight);
        splitPane.setOneTouchExpandable(true);

        return splitPane;
    }

    public static JButton createButton(String text, ActionListener listener)
    {
        JButton button = new JButton(text);

        button.addActionListener(listener);

        return button;
    }

    /** Compact button for the crowded palette and layer rows. */
    public static JButton createSmallButton(String text, String tooltip, ActionListener listener)
    {
        JButton button = createButton(text, listener);

        button.setMargin(SMALL_BUTTON_INSETS);
        button.setFont(button.getFont().deriveFont(SMALL_BUTTON_FONT_SIZE));
        button.setToolTipText(tooltip);

        return button;
    }

    /** Small grey label used for the "(single character key...)" style hints. */
    public static JLabel createHintLabel(String text)
    {
        JLabel label = new JLabel(text);

        label.setForeground(Color.GRAY);
        label.setFont(label.getFont().deriveFont(HINT_FONT_SIZE));

        return label;
    }

    public static JScrollPane createListScroll(JPanel listPanel, int unitIncrement)
    {
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.getVerticalScrollBar().setUnitIncrement(unitIncrement);

        return scroll;
    }

    public static JTextArea createCodeArea(boolean editable)
    {
        JTextArea area = new JTextArea();

        area.setEditable(editable);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setLineWrap(false);

        return area;
    }

    // Dialogs
    public static void showError(Component parent, String message)
    {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void showWarning(Component parent, String title, String message)
    {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.WARNING_MESSAGE);
    }

    /** Returns the chosen file, or null when the user cancels. */
    public static File chooseOpenFile(Component parent)
    {
        JFileChooser chooser = new JFileChooser();

        return chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION
                ? chooser.getSelectedFile()
                : null;
    }

    /** Returns the chosen file, or null when the user cancels. */
    public static File chooseSaveFile(Component parent, String defaultName)
    {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(defaultName));

        return chooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION
                ? chooser.getSelectedFile()
                : null;
    }
}