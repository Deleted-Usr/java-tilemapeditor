package com.willclay.mapeditor.menu.functions;

import com.willclay.mapeditor.core.ui.Utils;
import com.willclay.mapeditor.core.ui.Window;
import com.willclay.mapeditor.core.EditorModel;
import com.willclay.mapeditor.dialogs.ExportDialog;
import com.willclay.mapeditor.dialogs.ImportDialog;
import com.willclay.mapeditor.dialogs.MapSizeDialog;
import com.willclay.mapeditor.io.PaletteIO;

import java.io.File;
import java.io.IOException;

public class FileMenuFunctions
{
    private static final String DEFAULT_PALETTE_NAME = "palette." + PaletteIO.EXTENSION;

    private final Window window;

    public FileMenuFunctions(Window window)
    {
        this.window = window;
    }

    public void promptNewMap()
    {
        EditorModel model = window.getModel();
        MapSizeDialog dialog = new MapSizeDialog(window, "New Map",
                model.getCols(), model.getRows(), model.getTileSize());
        if (!dialog.showDialog()) return;

        model.newMap(dialog.getCols(), dialog.getRows(), dialog.getTileSize());
        window.refreshAll();
        window.setStatus("New map: " + describe(dialog));
    }

    public void exportLayers()
    {
        new ExportDialog(window, window.getModel()).setVisible(true);
    }

    public void importLayers()
    {
        EditorModel model = window.getModel();

        ImportDialog dialog = new ImportDialog(window, model);
        dialog.setOnImported(() ->
        {
            window.refreshAll();
            window.setStatus("Imported " + model.getLayers().size() + " layer(s)  -  "
                    + model.getCols() + "x" + model.getRows());
        });

        dialog.setVisible(true);
    }

    public void savePalette()
    {
        File file = Utils.chooseSaveFile(window, DEFAULT_PALETTE_NAME);
        if (file == null) return;

        try
        {
            PaletteIO.savePalette(window.getModel().getPalette(), file);
            window.setStatus("Saved palette to " + file.getName());
        }
        catch (IOException ex)
        {
            Utils.showError(window, "Failed to save palette:\n" + ex.getMessage());
        }
    }

    public void loadPalette()
    {
        File file = Utils.chooseOpenFile(window);
        if (file == null) return;

        try
        {
            window.getModel().setPalette(PaletteIO.loadPalette(file));
            window.refreshAll();
            window.setStatus("Loaded palette from " + file.getName());
        }
        catch (IOException ex)
        {
            Utils.showError(window, "Failed to load palette:\n" + ex.getMessage());
        }
    }

    public void quit()
    {
        window.dispose();
    }

    private static String describe(MapSizeDialog dialog)
    {
        return dialog.getCols() + "x" + dialog.getRows() + " @ " + dialog.getTileSize() + "px";
    }
}