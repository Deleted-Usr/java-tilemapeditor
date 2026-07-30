package com.willclay.mapeditor.core;

import com.willclay.mapeditor.domain.Layer;
import com.willclay.mapeditor.domain.ParsedLayer;
import com.willclay.mapeditor.domain.TileDef;
import com.willclay.mapeditor.history.EditorState;
import com.willclay.mapeditor.history.History;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Holds all editor state: the tile palette, the layer stack, map dimensions,
 * the current selection and the active layer.
 *
 * Listeners fire on structural changes (new map, resize, layer add/remove) so
 * the UI can rebuild. Individual cell edits do not fire - far too chatty while
 * painting - so the canvas repaints itself directly.
 */
public class EditorModel
{
    public static final char EMPTY = ' ';

    private static final int MIN_TILE_SIZE = 4;
    private static final String[] DEFAULT_LAYER_NAMES = { "Background", "Terrain", "Props", "Markers" };
    private static final String LETTER_POOL =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    // Tile Palette
    private final List<TileDef> palette = new ArrayList<>();
    private TileDef selectedTile = null;

    // Layers
    private final List<Layer> layers = new ArrayList<>();
    private int activeLayerIndex = 0;

    private int rows = 18;
    private int cols = 32;
    private int tileSize = 16;

    // Undo / Redo
    private final History history = new History();

    // Listeners
    public interface Listener
    {
        void modelChanged();
    }

    private final List<Listener> listeners = new ArrayList<>();

    public void addListener(Listener l)
    {
        listeners.add(l);
    }

    private void fire()
    {
        for (Listener l : listeners) l.modelChanged();
    }

    // Map Lifecycle
    /** Rebuild the default layer stack at the current dimensions. */
    public void newMap()
    {
        newMap(cols, rows, tileSize);
    }

    public void newMap(int cols, int rows, int tileSize)
    {
        this.cols = Math.max(1, cols);
        this.rows = Math.max(1, rows);
        this.tileSize = Math.max(MIN_TILE_SIZE, tileSize);

        layers.clear();
        for (String name : DEFAULT_LAYER_NAMES)
        {
            if (name.equals("Background"))
            {
                layers.add(new Layer(Layer.LayerType.INT, name, this.rows, this.cols));
            }
            else
            {
                layers.add(new Layer(Layer.LayerType.CHAR, name, this.rows, this.cols));
            }
        }
        activeLayerIndex = 0;

        history.clear(); // A brand-new map starts a fresh undo history.
        fire();
    }

    public void resize(int newCols, int newRows, int newTileSize)
    {
        final int targetCols = Math.max(1, newCols);
        final int targetRows = Math.max(1, newRows);

        runSnapshotEdit(() ->
        {
            this.tileSize = Math.max(MIN_TILE_SIZE, newTileSize);

            int copyRows = Math.min(targetRows, rows);
            int copyCols = Math.min(targetCols, cols);

            for (Layer layer : layers)
            {
                Layer resized = new Layer(Layer.LayerType.CHAR, layer.name, targetRows, targetCols);
                for (int r = 0; r < copyRows; r++)
                {
                    System.arraycopy(layer.grid[r], 0, resized.grid[r], 0, copyCols);
                }
                layer.grid = resized.grid;
            }

            this.cols = targetCols;
            this.rows = targetRows;
        });

        fire();
    }

    // Palette
    /** First tile with this letter, regardless of layer assignment. */
    public TileDef findTile(char letter)
    {
        for (TileDef t : palette)
        {
            if (t.letter == letter) return t;
        }

        return null;
    }

    /**
     * Resolve a letter for a specific layer: a tile pinned to that layer wins,
     * then an unrestricted tile, then the first tile with that letter. The last
     * fallback stops cells rendering as "unknown" just because assignments were
     * edited after painting.
     */
    public TileDef findTileForLayer(char letter, int layerIndex)
    {
        String layerName = isValidLayer(layerIndex) ? layers.get(layerIndex).name : null;

        TileDef unrestricted = null;
        TileDef first = null;

        for (TileDef t : palette)
        {
            if (t.letter != letter) continue;

            if (first == null) first = t;
            if (t.layerName == null && unrestricted == null) unrestricted = t;
            if (t.layerName != null && t.layerName.equals(layerName)) return t;
        }

        return unrestricted != null ? unrestricted : first;
    }

    /**
     * Check a candidate against the duplicate-letter rule: tiles may share a
     * letter only when every one of them is pinned to a different layer, since
     * an unrestricted tile would collide on that tile's layer anyway. Returns
     * null when the tile is fine, otherwise a human-readable reason.
     * {@code ignore} is the tile being edited, so it isn't compared to itself.
     */
    public String validateTile(TileDef candidate, TileDef ignore)
    {
        for (TileDef t : palette)
        {
            if (t == ignore || t.letter != candidate.letter) continue;

            if (candidate.layerName == null || t.layerName == null)
            {
                return "Letter '" + candidate.letter + "' is already used by \"" + t.name
                        + "\". Tiles sharing a letter must each be assigned to a specific layer.";
            }

            if (candidate.layerName.equals(t.layerName))
            {
                return "Letter '" + candidate.letter + "' is already used by \"" + t.name
                        + "\" on layer \"" + t.layerName + "\".";
            }
        }

        return null;
    }

    public boolean addTile(TileDef def)
    {
        if (validateTile(def, null) != null) return false;

        palette.add(def);
        if (selectedTile == null) selectedTile = def;

        return true;
    }

    public void removeTile(TileDef def)
    {
        palette.remove(def);
        if (selectedTile == def) { selectedTile = palette.isEmpty() ? null : palette.get(0); }
    }

    /** Move a palette tile up (-1) or down (+1) in the list. */
    public void movePaletteTile(TileDef def, int direction)
    {
        int from = palette.indexOf(def);
        if (from < 0) return;

        int to = from + direction;
        if (to < 0 || to >= palette.size()) return;

        Collections.swap(palette, from, to);
    }

    /** Smallest positive id not yet used by any tile. */
    public int nextFreeId()
    {
        Set<Integer> used = new HashSet<>();
        for (TileDef t : palette) { used.add(t.id); }

        int id = 1;
        while (used.contains(id)) id++;

        return id;
    }

    /** First printable character not used by any palette tile. */
    private char freeLetter()
    {
        for (int i = 0; i < LETTER_POOL.length(); i++)
        {
            char c = LETTER_POOL.charAt(i);
            if (findTile(c) == null) return c;
        }

        // Palette is enormous; fall back to the extended printable range.
        for (char c = '!'; c < 0x7F; c++)
        {
            if (findTile(c) == null) return c;
        }

        return '?';
    }

    /** Can the currently selected tile be painted on the active layer? */
    public boolean isSelectedUsableOnActiveLayer()
    {
        if (selectedTile == null || layers.isEmpty()) return false;

        return selectedTile.isUsableOn(getActiveLayer().name);
    }

    // Layers
    public void renameLayer(int index, String newName)
    {
        if (!isValidLayer(index) || newName == null || newName.trim().isEmpty()) return;

        String oldName = layers.get(index).name;
        String trimmed = newName.trim();
        layers.get(index).name = trimmed;

        // Keep tile restrictions pointing at the layer they were pinned to.
        for (TileDef t : palette)
        {
            if (oldName.equals(t.layerName)) t.layerName = trimmed;
        }

        fire();
    }

    public void addLayer(String name)
    {
        runSnapshotEdit(() ->
        {
            layers.add(new Layer(Layer.LayerType.CHAR, name, rows, cols));
            activeLayerIndex = layers.size() - 1;
        });

        fire();
    }

    public void removeLayer(int index)
    {
        if (layers.size() <= 1 || !isValidLayer(index)) return; // Always keep one.

        runSnapshotEdit(() ->
        {
            layers.remove(index);
            if (activeLayerIndex >= layers.size()) activeLayerIndex = layers.size() - 1;
        });

        fire();
    }

    public void moveLayer(int from, int to)
    {
        if (!isValidLayer(from) || !isValidLayer(to)) return;

        runSnapshotEdit(() ->
        {
            layers.add(to, layers.remove(from));
            activeLayerIndex = to;
        });

        fire();
    }

    /** Flip a layer between char[][] and int[][] export (undoable). */
    public void toggleLayerType(int index)
    {
        if (!isValidLayer(index)) return;

        runSnapshotEdit(() -> layers.get(index).toggleType());
        fire();
    }

    private boolean isValidLayer(int index)
    {
        return index >= 0 && index < layers.size();
    }

    // Cell Editing
    public void setCell(int row, int col, char letter)
    {
        if (!inBounds(row, col)) return;

        char[][] grid = getActiveLayer().grid;
        char previous = grid[row][col];
        if (previous == letter) return; // No-op, nothing worth recording.

        history.recordCell(activeLayerIndex, row, col, previous, letter);
        grid[row][col] = letter;
    }

    public char getCell(int layerIndex, int row, int col)
    {
        if (!inBounds(row, col) || !isValidLayer(layerIndex)) return EMPTY;

        return layers.get(layerIndex).grid[row][col];
    }

    public void fillActiveLayer(char letter)
    {
        runSnapshotEdit(() -> getActiveLayer().fill(letter));
    }

    private boolean inBounds(int row, int col)
    {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    // Undo / Redo
    /** Called by the canvas when a paint stroke begins (mouse pressed). */
    public void beginStroke() { history.beginStroke(); }

    /** Called by the canvas when a paint stroke ends (mouse released). */
    public void endStroke() { history.endStroke(); }

    public void undo()
    {
        history.undo(this);
        fire();
    }

    public void redo()
    {
        history.redo(this);
        fire();
    }

    public void clearHistory() { history.clear(); }

    /**
     * Run a structural mutation as a single undoable snapshot: capture before,
     * mutate, capture after, push the pair.
     */
    private void runSnapshotEdit(Runnable mutation)
    {
        history.endStroke(); // Close any open stroke so ordering stays correct.

        EditorState before = new EditorState(this);
        mutation.run();
        history.pushSnapshot(before, new EditorState(this));
    }

    /**
     * Rebuild this model from a snapshot. Called by the history package; the
     * caller fires the change notification once the whole edit is applied.
     */
    public void restore(EditorState state)
    {
        this.rows = state.getRows();
        this.cols = state.getCols();
        this.tileSize = state.getTileSize();

        layers.clear();
        layers.addAll(state.copyLayers());

        this.activeLayerIndex = Math.min(state.getActiveLayerIndex(), layers.size() - 1);
    }

    // Import
    /**
     * Replace the layer stack with parsed layers. When {@code sizeFromImport}
     * is set the map grows to fit the largest one, otherwise layers are clipped
     * to the current size. {@code autoAddTiles} adds palette entries for any
     * unknown letters so imported data is actually visible.
     */
    public void applyImportedLayers(List<ParsedLayer> parsed, boolean sizeFromImport, boolean autoAddTiles)
    {
        runSnapshotEdit(() ->
        {
            if (sizeFromImport) { sizeToFit(parsed); }

            layers.clear();
            for (ParsedLayer p : parsed) { layers.add(toLayer(p, autoAddTiles)); }
            if (layers.isEmpty()) { layers.add(new Layer(Layer.LayerType.CHAR, "Layer 1", rows, cols)); }

            activeLayerIndex = 0;
            if (autoAddTiles) { addMissingTilesFromLayers(); }
        });

        fire();
    }

    private void sizeToFit(List<ParsedLayer> parsed)
    {
        int maxRows = 0;
        int maxCols = 0;

        for (ParsedLayer p : parsed)
        {
            maxRows = Math.max(maxRows, p.rows);
            maxCols = Math.max(maxCols, p.cols);
        }

        this.rows = Math.max(1, maxRows);
        this.cols = Math.max(1, maxCols);
    }

    private Layer toLayer(ParsedLayer parsed, boolean autoAddTiles)
    {
        Layer layer = new Layer(Layer.LayerType.CHAR, parsed.name, rows, cols);

        int copyRows = Math.min(rows, parsed.rows);
        int copyCols = Math.min(cols, parsed.cols);

        if (!parsed.intType)
        {
            for (int r = 0; r < copyRows; r++)
            {
                System.arraycopy(parsed.grid[r], 0, layer.grid[r], 0, copyCols);
            }

            return layer;
        }

        // Map each int id back to a tile letter; 0 is empty.
        layer.type = Layer.LayerType.INT;
        for (int r = 0; r < copyRows; r++)
        {
            for (int c = 0; c < copyCols; c++)
            {
                int id = parsed.intGrid[r][c];
                layer.grid[r][c] = (id == 0) ? EMPTY : letterForId(id, autoAddTiles);
            }
        }

        return layer;
    }

    /**
     * Letter for an imported int id. With {@code autoAdd} a missing id gets a
     * new palette tile; without it, unknown ids fall back to '?' so they at
     * least render as something.
     */
    private char letterForId(int id, boolean autoAdd)
    {
        for (TileDef t : palette)
        {
            if (t.id == id) return t.letter;
        }

        if (!autoAdd) return '?';

        char letter = freeLetter();
        TileDef def = new TileDef(letter, "Tile " + id, TileDef.Category.TERRAIN, new Color(140, 140, 150));
        def.id = id;
        palette.add(def); // Direct add: the letter is guaranteed free.

        return letter;
    }

    /** Give every unrecognised letter in the layer stack a neutral palette entry. */
    private void addMissingTilesFromLayers()
    {
        Color[] swatches =
                {
                        new Color(180, 120, 90),  new Color(90, 160, 120),
                        new Color(120, 120, 200), new Color(200, 160, 90),
                        new Color(160, 100, 160), new Color(100, 170, 170),
                };

        int next = palette.size();

        for (Layer layer : layers)
        {
            for (char[] row : layer.grid)
            {
                for (char letter : row)
                {
                    if (letter == EMPTY || findTile(letter) != null) continue;

                    TileDef def = new TileDef(letter, "Tile " + letter,
                            TileDef.Category.TERRAIN, swatches[next % swatches.length]);
                    def.id = nextFreeId();
                    addTile(def);
                    next++;
                }
            }
        }
    }

    // Getters
    public int getRows() { return rows; }
    public int getCols() { return cols; }
    public int getTileSize() { return tileSize; }

    public List<TileDef> getPalette() { return palette; }
    public TileDef getSelectedTile() { return selectedTile; }
    public char getSelectedLetter() { return selectedTile == null ? EMPTY : selectedTile.letter; }

    public List<Layer> getLayers() { return layers; }
    public int getActiveLayerIndex() { return activeLayerIndex; }
    public Layer getActiveLayer() { return layers.get(activeLayerIndex); }

    public boolean canUndo() { return history.canUndo(); }
    public boolean canRedo() { return history.canRedo(); }

    // Setters
    public void setSelectedTile(TileDef tile) { selectedTile = tile; }
    public void setSelectedLetter(char letter) { selectedTile = findTile(letter); }

    public void setActiveLayerIndex(int index)
    {
        if (isValidLayer(index)) activeLayerIndex = index;
    }

    public void setPalette(List<TileDef> tiles)
    {
        palette.clear();
        palette.addAll(tiles);
        selectedTile = palette.isEmpty() ? null : palette.get(0);
    }
}