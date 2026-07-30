package com.willclay.mapeditor.menu.functions;

import com.willclay.mapeditor.core.ui.Window;

public class ViewMenuFunctions
{
    private final Window window;

    public ViewMenuFunctions(Window window)
    {
        this.window = window;
    }

    public void setShowGrid(boolean show)
    {
        window.getMapCanvasPanel().setShowGrid(show);
    }

    public void setShowLetters(boolean show)
    {
        window.getMapCanvasPanel().setShowLetters(show);
    }

    public void setDimInactiveLayers(boolean dim)
    {
        window.getMapCanvasPanel().setDimInactiveLayers(dim);
    }

    public void setDarkMode(boolean on)
    {
        window.setDarkMode(on);
    }
}