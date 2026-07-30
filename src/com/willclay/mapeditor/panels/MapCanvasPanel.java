package com.willclay.mapeditor.panels;

import com.willclay.mapeditor.core.ui.Theme;
import com.willclay.mapeditor.core.EditorModel;
import com.willclay.mapeditor.domain.Layer;
import com.willclay.mapeditor.domain.TileDef;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;

/**
 * The central editing surface. Draws every layer, paints the active one with
 * the selected tile (left mouse) and erases with the right mouse. Zoom is an
 * internal scale factor applied on top of the model's tile size.
 */
public class MapCanvasPanel extends JPanel implements Scrollable
{
    private static final double MIN_SCALE = 0.15;
    private static final double MAX_SCALE = 6.0;
    private static final double WHEEL_ZOOM_IN = 1.1;
    private static final double WHEEL_ZOOM_OUT = 0.9;

    private static final float INACTIVE_ALPHA = 0.35f;
    private static final int MIN_LETTER_CELL = 14; // Below this, letters are unreadable.
    private static final double BRIGHT_LUMINANCE = 140.0;

    private static final int SCROLL_UNIT = 24;
    private static final int SCROLL_BLOCK = 240;

    private static final Color HOVER_FILL = new Color(255, 255, 255, 60);
    private static final Color HOVER_OUTLINE = new Color(255, 255, 255, 180);

    private final EditorModel model;

    private boolean showGrid = true;
    private boolean showLetters = true;
    private boolean dimInactiveLayers = true;

    private double scale = 2.0;

    private int hoverRow = -1;
    private int hoverCol = -1;

    private Consumer<String> statusSink = s -> {};

    public MapCanvasPanel(EditorModel model)
    {
        this.model = model;

        setBackground(Theme.bg());
        addMouseHandlers();
    }

    private void addMouseHandlers()
    {
        MouseAdapter mouse = new MouseAdapter()
        {
            @Override public void mousePressed(MouseEvent e) { model.beginStroke(); paintAt(e); }
            @Override public void mouseDragged(MouseEvent e) { paintAt(e); updateHover(e); }
            @Override public void mouseReleased(MouseEvent e) { model.endStroke(); }
            @Override public void mouseMoved(MouseEvent e) { updateHover(e); }
            @Override public void mouseExited(MouseEvent e) { clearHover(); }
        };

        addMouseListener(mouse);
        addMouseMotionListener(mouse);

        addMouseWheelListener(e ->
        {
            // Ctrl+wheel zooms; anything else is the scroll pane's business.
            if (e.isControlDown()) zoom(e.getWheelRotation() < 0 ? WHEEL_ZOOM_IN : WHEEL_ZOOM_OUT);
            else getParent().dispatchEvent(e);
        });
    }

    // Zoom
    public void zoom(double factor)
    {
        scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale * factor));
        refreshSize();
    }

    public void resetZoom()
    {
        scale = 1.0;
        refreshSize();
    }

    public void recomputePreferredSize()
    {
        int cell = cellSize();
        setPreferredSize(new Dimension(model.getCols() * cell + 1, model.getRows() * cell + 1));
    }

    private void refreshSize()
    {
        recomputePreferredSize();
        revalidate();
        repaint();
    }

    private int cellSize()
    {
        return (int) Math.round(model.getTileSize() * scale);
    }

    // Painting Input
    private void paintAt(MouseEvent e)
    {
        int row = rowAt(e);
        int col = colAt(e);
        if (!inBounds(row, col)) return;

        if (SwingUtilities.isRightMouseButton(e))
        {
            model.setCell(row, col, EditorModel.EMPTY);
        }
        else if (SwingUtilities.isLeftMouseButton(e))
        {
            // A tile pinned to one layer can only be painted on that layer.
            if (!model.isSelectedUsableOnActiveLayer())
            {
                reportUnusableTile();
                return;
            }

            model.setCell(row, col, model.getSelectedLetter());
        }

        repaint();
    }

    private void reportUnusableTile()
    {
        TileDef selected = model.getSelectedTile();
        if (selected == null) return;

        statusSink.accept("\"" + selected.name + "\" can only be painted on layer \""
                + selected.layerName + "\"");
    }

    private void updateHover(MouseEvent e)
    {
        int row = rowAt(e);
        int col = colAt(e);
        if (row == hoverRow && col == hoverCol) return;

        hoverRow = row;
        hoverCol = col;

        if (inBounds(row, col)) statusSink.accept(describeCell(row, col));
        repaint();
    }

    private void clearHover()
    {
        hoverRow = -1;
        hoverCol = -1;
        repaint();
    }

    private String describeCell(int row, int col)
    {
        char letter = model.getCell(model.getActiveLayerIndex(), row, col);
        TileDef tile = model.findTileForLayer(letter, model.getActiveLayerIndex());
        String name = (tile == null) ? "(empty)" : tile.name + " '" + tile.letter + "'";

        return "Row " + row + ", Col " + col + "  -  " + name;
    }

    private int rowAt(MouseEvent e) { return e.getY() / cellSize(); }
    private int colAt(MouseEvent e) { return e.getX() / cellSize(); }

    private boolean inBounds(int row, int col)
    {
        return row >= 0 && row < model.getRows() && col >= 0 && col < model.getCols();
    }

    // Rendering
    @Override
    protected void paintComponent(Graphics graphics)
    {
        super.paintComponent(graphics);

        Graphics2D g = (Graphics2D) graphics;
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        int cell = cellSize();

        drawBackdrop(g, cell);
        drawLayers(g, cell);
        if (showGrid) drawGrid(g, cell);
        drawHover(g, cell);
    }

    /** Checkerboard so empty cells read as transparent rather than solid. */
    private void drawBackdrop(Graphics2D g, int cell)
    {
        for (int r = 0; r < model.getRows(); r++)
        {
            for (int c = 0; c < model.getCols(); c++)
            {
                g.setColor((r + c) % 2 == 0 ? Theme.canvasA() : Theme.canvasB());
                g.fillRect(c * cell, r * cell, cell, cell);
            }
        }
    }

    /** Bottom to top, so upper layers sit over lower ones. */
    private void drawLayers(Graphics2D g, int cell)
    {
        List<Layer> layers = model.getLayers();

        for (int li = 0; li < layers.size(); li++)
        {
            Layer layer = layers.get(li);
            if (!layer.visible) continue;

            boolean active = (li == model.getActiveLayerIndex());
            Composite previous = g.getComposite();
            if (!active && dimInactiveLayers)
            {
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, INACTIVE_ALPHA));
            }

            for (int r = 0; r < model.getRows(); r++)
            {
                for (int c = 0; c < model.getCols(); c++)
                {
                    char letter = layer.grid[r][c];
                    if (letter == EditorModel.EMPTY) continue;

                    drawTile(g, letter, li, c * cell, r * cell, cell);
                }
            }

            g.setComposite(previous);
        }
    }

    private void drawTile(Graphics2D g, char letter, int layerIndex, int x, int y, int size)
    {
        // Resolving per layer means duplicate letters render with the right
        // sprite on each layer they're pinned to.
        TileDef tile = model.findTileForLayer(letter, layerIndex);

        if (tile == null)
        {
            g.setColor(Color.MAGENTA); // Unknown letter: loud on purpose.
            g.fillRect(x, y, size, size);
        }
        else if (tile.sprite != null)
        {
            g.drawImage(tile.sprite, x, y, size, size, null);
        }
        else if (tile.category == TileDef.Category.MARKER)
        {
            drawMarker(g, tile, x, y, size);
        }
        else
        {
            g.setColor(tile.colour);
            g.fillRect(x, y, size, size);
        }

        if (showLetters && size >= MIN_LETTER_CELL) drawLetter(g, letter, tile, x, y, size);
    }

    /** Markers draw as a diamond so they stand out from terrain. */
    private void drawMarker(Graphics2D g, TileDef tile, int x, int y, int size)
    {
        int cx = x + size / 2;
        int cy = y + size / 2;
        int half = size / 2 - 2;

        g.setColor(tile.colour);
        g.fillPolygon(new int[] { cx, cx + half, cx, cx - half },
                new int[] { cy - half, cy, cy + half, cy }, 4);
    }

    private void drawLetter(Graphics2D g, char letter, TileDef tile, int x, int y, int size)
    {
        g.setColor(contrastColour(tile));
        g.setFont(getFont().deriveFont(Font.BOLD, Math.max(9, size / 3)));

        FontMetrics fm = g.getFontMetrics();
        String text = String.valueOf(letter);

        g.drawString(text,
                x + (size - fm.stringWidth(text)) / 2,
                y + (size + fm.getAscent() - fm.getDescent()) / 2);
    }

    private Color contrastColour(TileDef tile)
    {
        if (tile == null || tile.sprite != null) return Color.WHITE;

        Color base = tile.colour;
        double luminance = 0.299 * base.getRed() + 0.587 * base.getGreen() + 0.114 * base.getBlue();

        return luminance > BRIGHT_LUMINANCE ? Color.BLACK : Color.WHITE;
    }

    private void drawGrid(Graphics2D g, int cell)
    {
        int width = model.getCols() * cell;
        int height = model.getRows() * cell;

        g.setColor(Theme.grid());
        for (int c = 0; c <= model.getCols(); c++) { g.drawLine(c * cell, 0, c * cell, height); }
        for (int r = 0; r <= model.getRows(); r++) { g.drawLine(0, r * cell, width, r * cell); }
    }

    private void drawHover(Graphics2D g, int cell)
    {
        if (!inBounds(hoverRow, hoverCol)) return;

        g.setColor(HOVER_FILL);
        g.fillRect(hoverCol * cell, hoverRow * cell, cell, cell);

        g.setColor(HOVER_OUTLINE);
        g.drawRect(hoverCol * cell, hoverRow * cell, cell, cell);
    }

    // Scrollable
    @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return SCROLL_UNIT; }
    @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return SCROLL_BLOCK; }
    @Override public boolean getScrollableTracksViewportWidth() { return false; }
    @Override public boolean getScrollableTracksViewportHeight() { return false; }

    // Setters
    public void setStatusSink(Consumer<String> sink) { this.statusSink = sink; }

    public void setShowGrid(boolean show) { showGrid = show; repaint(); }
    public void setShowLetters(boolean show) { showLetters = show; repaint(); }
    public void setDimInactiveLayers(boolean dim) { dimInactiveLayers = dim; repaint(); }

    /** Re-read the backdrop colour after a theme change. */
    public void refreshTheme()
    {
        setBackground(Theme.bg());
        repaint();
    }
}
