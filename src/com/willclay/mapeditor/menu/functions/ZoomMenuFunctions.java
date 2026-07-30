package com.willclay.mapeditor.menu.functions;

import com.willclay.mapeditor.core.ui.Window;

public class ZoomMenuFunctions
{
    private static final double ZOOM_IN_FACTOR = 1.25;
    private static final double ZOOM_OUT_FACTOR = 0.8;

    private final Window window;

    public ZoomMenuFunctions(Window window)
    {
        this.window = window;
    }

    public void zoomIn()
    {
        window.getMapCanvasPanel().zoom(ZOOM_IN_FACTOR);
    }

    public void zoomOut()
    {
        window.getMapCanvasPanel().zoom(ZOOM_OUT_FACTOR);
    }

    public void resetZoom()
    {
        window.getMapCanvasPanel().resetZoom();
    }
}