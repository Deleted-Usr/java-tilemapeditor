package com.willclay.mapeditor.menu.functions;

import com.willclay.mapeditor.core.ui.Window;
import com.willclay.mapeditor.core.EditorModel;
import com.willclay.mapeditor.dialogs.MapSizeDialog;

public class EditMenuFunctions
{
    private final Window window;

    public EditMenuFunctions(Window window)
    {
        this.window = window;
    }

    public void undo()
    {
        window.getModel().undo();
        window.setStatus("Undo");
    }

    public void redo()
    {
        window.getModel().redo();
        window.setStatus("Redo");
    }

    public void fill()
    {
        EditorModel model = window.getModel();

        model.fillActiveLayer(model.getSelectedLetter());
        window.getMapCanvasPanel().repaint();
        window.setStatus("Filled " + model.getActiveLayer().name);
    }

    public void clear()
    {
        EditorModel model = window.getModel();

        model.fillActiveLayer(EditorModel.EMPTY);
        window.getMapCanvasPanel().repaint();
        window.setStatus("Cleared " + model.getActiveLayer().name);
    }

    public void promptResize()
    {
        EditorModel model = window.getModel();
        MapSizeDialog dialog = new MapSizeDialog(window, "Resize Map",
                model.getCols(), model.getRows(), model.getTileSize());
        if (!dialog.showDialog()) return;

        model.resize(dialog.getCols(), dialog.getRows(), dialog.getTileSize());
        window.refreshAll();
        window.setStatus("Resized to: " + dialog.getCols() + "x" + dialog.getRows()
                + " @ " + dialog.getTileSize() + "px");
    }
}