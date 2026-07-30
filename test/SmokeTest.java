import com.willclay.mapeditor.core.editor.EditorModel;
import com.willclay.mapeditor.domain.Layer;
import com.willclay.mapeditor.domain.ParsedLayer;
import com.willclay.mapeditor.domain.TileDef;
import com.willclay.mapeditor.io.LoaderExporter;
import com.willclay.mapeditor.io.MapExporter;
import com.willclay.mapeditor.io.MapImporter;
import com.willclay.mapeditor.io.PaletteIO;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

public class SmokeTest
{
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception
    {
        testNewMap();
        testStrokeUndoRedo();
        testFillAndClear();
        testResizePreservesContent();
        testLayerOperations();
        testPaletteRules();
        testCharRoundTrip();
        testIntRoundTrip();
        testLoaderExport();
        testImportUnknownLetters();
        testPaletteIORoundTrip();
        testImporterTolerance();

        System.out.println();
        System.out.println("passed: " + passed + "   failed: " + failed);
        if (failed > 0) System.exit(1);
    }

    // ---- Tests -----------------------------------------------------------

    private static void testNewMap()
    {
        EditorModel m = new EditorModel();
        m.newMap(10, 5, 16);

        check("newMap: 3 default layers", m.getLayers().size() == 3);
        check("newMap: names", m.getLayers().get(0).name.equals("Terrain")
                && m.getLayers().get(2).name.equals("Markers"));
        check("newMap: dimensions", m.getCols() == 10 && m.getRows() == 5 && m.getTileSize() == 16);
        check("newMap: grid sized [rows][cols]", m.getLayers().get(0).grid.length == 5
                && m.getLayers().get(0).grid[0].length == 10);
        check("newMap: cells empty", m.getCell(0, 2, 3) == EditorModel.EMPTY);
        check("newMap: history cleared", !m.canUndo() && !m.canRedo());
        check("newMap: min tile size clamped", clampedTileSize() == 4);
    }

    private static int clampedTileSize()
    {
        EditorModel m = new EditorModel();
        m.newMap(4, 4, 1);
        return m.getTileSize();
    }

    private static void testStrokeUndoRedo()
    {
        EditorModel m = seeded(10, 5);

        m.beginStroke();
        m.setCell(1, 1, 'g');
        m.setCell(1, 2, 'g');
        m.setCell(1, 1, 'd'); // Revisit the same cell mid-stroke.
        m.endStroke();

        check("stroke: painted", m.getCell(0, 1, 1) == 'd' && m.getCell(0, 1, 2) == 'g');
        check("stroke: one undo available", m.canUndo());

        m.undo();
        check("stroke: undo reverts whole stroke", m.getCell(0, 1, 1) == EditorModel.EMPTY
                && m.getCell(0, 1, 2) == EditorModel.EMPTY);

        m.redo();
        check("stroke: redo restores final value", m.getCell(0, 1, 1) == 'd');

        // A no-op write must not be recorded.
        m.beginStroke();
        m.setCell(1, 1, 'd');
        m.endStroke();
        m.undo();
        check("stroke: no-op not recorded", m.getCell(0, 1, 1) == EditorModel.EMPTY);
    }

    private static void testFillAndClear()
    {
        EditorModel m = seeded(6, 4);

        m.fillActiveLayer('g');
        check("fill: every cell set", m.getCell(0, 3, 5) == 'g');

        m.fillActiveLayer(EditorModel.EMPTY);
        check("clear: every cell empty", m.getCell(0, 3, 5) == EditorModel.EMPTY);

        m.undo();
        check("clear: undo restores fill", m.getCell(0, 3, 5) == 'g');

        m.undo();
        check("fill: undo restores empty", m.getCell(0, 3, 5) == EditorModel.EMPTY);
    }

    private static void testResizePreservesContent()
    {
        EditorModel m = seeded(6, 4);

        m.beginStroke();
        m.setCell(0, 0, 'g');
        m.setCell(3, 5, 'r');
        m.endStroke();

        m.resize(10, 8, 32);
        check("resize: dimensions", m.getCols() == 10 && m.getRows() == 8 && m.getTileSize() == 32);
        check("resize: grid reallocated", m.getLayers().get(0).grid.length == 8
                && m.getLayers().get(0).grid[0].length == 10);
        check("resize: content kept", m.getCell(0, 0, 0) == 'g' && m.getCell(0, 3, 5) == 'r');
        check("resize: new cells empty", m.getCell(0, 7, 9) == EditorModel.EMPTY);

        m.undo();
        check("resize: undo restores size", m.getCols() == 6 && m.getRows() == 4);
        check("resize: undo keeps content", m.getCell(0, 3, 5) == 'r');

        // Shrinking must clip rather than blow up.
        m.resize(2, 2, 16);
        check("resize: shrink clips", m.getCols() == 2 && m.getCell(0, 0, 0) == 'g');
    }

    private static void testLayerOperations()
    {
        EditorModel m = seeded(6, 4);

        m.addLayer("Extra");
        check("addLayer: appended and activated", m.getLayers().size() == 4
                && m.getActiveLayerIndex() == 3 && m.getActiveLayer().name.equals("Extra"));

        m.toggleLayerType(3);
        check("toggleLayerType: flipped to INT", m.getLayers().get(3).isIntType());

        m.moveLayer(3, 0);
        check("moveLayer: reordered", m.getLayers().get(0).name.equals("Extra")
                && m.getActiveLayerIndex() == 0);

        m.undo();
        check("moveLayer: undo restores order", m.getLayers().get(3).name.equals("Extra"));

        // A tile pinned to a layer follows a rename.
        TileDef pinned = new TileDef('x', "Pinned", TileDef.Category.PROP, Color.RED);
        pinned.layerName = "Props";
        m.addTile(pinned);
        m.renameLayer(1, "Objects");
        check("renameLayer: restriction follows", "Objects".equals(pinned.layerName));

        m.setActiveLayerIndex(1);
        m.setSelectedTile(pinned);
        check("layer restriction: usable on its own layer", m.isSelectedUsableOnActiveLayer());
        m.setActiveLayerIndex(0);
        check("layer restriction: blocked elsewhere", !m.isSelectedUsableOnActiveLayer());

        int before = m.getLayers().size();
        m.removeLayer(0);
        check("removeLayer: removed", m.getLayers().size() == before - 1);

        while (m.getLayers().size() > 1) m.removeLayer(0);
        m.removeLayer(0);
        check("removeLayer: keeps at least one", m.getLayers().size() == 1);
    }

    private static void testPaletteRules()
    {
        EditorModel m = seeded(4, 4);

        TileDef a = m.findTile('g');
        check("palette: findTile", a != null && a.name.equals("Ground"));

        // Duplicate letter with no restriction is rejected.
        TileDef clash = new TileDef('g', "Clash", TileDef.Category.TERRAIN, Color.RED);
        check("validateTile: unrestricted duplicate rejected", m.validateTile(clash, null) != null);
        check("addTile: rejected duplicate not added", !m.addTile(clash));

        // Same letter is fine when both sides are pinned to different layers.
        a.layerName = "Terrain";
        clash.layerName = "Props";
        check("validateTile: pinned duplicates allowed", m.validateTile(clash, null) == null);
        check("addTile: pinned duplicate added", m.addTile(clash));

        check("findTileForLayer: resolves per layer",
                m.findTileForLayer('g', 0) == a && m.findTileForLayer('g', 1) == clash);

        check("validateTile: ignores the tile being edited", m.validateTile(a, a) == null);

        int free = m.nextFreeId();
        boolean used = false;
        for (TileDef t : m.getPalette()) if (t.id == free) used = true;
        check("nextFreeId: unused", !used);

        m.setSelectedTile(a);
        m.removeTile(a);
        check("removeTile: selection moves on", m.getSelectedTile() != a && m.getSelectedTile() != null);
    }

    private static void testCharRoundTrip()
    {
        EditorModel m = seeded(6, 3);
        m.beginStroke();
        m.setCell(0, 0, 'g');
        m.setCell(1, 2, 'd');
        m.setCell(2, 5, 'r');
        m.endStroke();

        String java = MapExporter.exportAsJava(m, true, "");
        check("export: declares char[][]", java.contains("char[][] Terrain = {"));
        check("export: quotes cells", java.contains("'g'"));

        List<ParsedLayer> parsed = MapImporter.parseArrays(java);
        check("round trip: layer count", parsed.size() == 3);
        check("round trip: names recovered", parsed.get(0).name.equals("Terrain"));
        check("round trip: dimensions", parsed.get(0).rows == 3 && parsed.get(0).cols == 6);
        check("round trip: cells match", parsed.get(0).grid[0][0] == 'g'
                && parsed.get(0).grid[1][2] == 'd'
                && parsed.get(0).grid[2][5] == 'r');
        check("round trip: empties preserved", parsed.get(0).grid[0][1] == EditorModel.EMPTY);

        // Escaping: a quote and a backslash must survive the trip.
        m.addTile(new TileDef('\'', "Quote", TileDef.Category.TERRAIN, Color.BLUE));
        m.addTile(new TileDef('\\', "Slash", TileDef.Category.TERRAIN, Color.BLUE));
        m.beginStroke();
        m.setCell(0, 1, '\'');
        m.setCell(0, 2, '\\');
        m.endStroke();

        List<ParsedLayer> escaped = MapImporter.parseArrays(MapExporter.exportAsJava(m, true, ""));
        check("round trip: escaped quote", escaped.get(0).grid[0][1] == '\'');
        check("round trip: escaped backslash", escaped.get(0).grid[0][2] == '\\');

        // Prefixed variable names.
        String prefixed = MapExporter.exportAsJava(m, true, "level1");
        check("export: honours prefix", prefixed.contains("char[][] level1_Terrain = {"));
    }

    private static void testIntRoundTrip()
    {
        EditorModel m = seeded(5, 3);
        m.toggleLayerType(0);

        m.beginStroke();
        m.setCell(0, 0, 'g');
        m.setCell(1, 1, 'r');
        m.endStroke();

        int groundId = m.findTile('g').id;
        int rockId = m.findTile('r').id;

        String java = MapExporter.exportAsJava(m, true, "");
        check("int export: declares int[][]", java.contains("int[][] Terrain = {"));

        List<ParsedLayer> parsed = MapImporter.parseArrays(java);
        ParsedLayer first = parsed.get(0);
        check("int export: parsed as int layer", first.intType);
        check("int export: ids written", first.intGrid[0][0] == groundId && first.intGrid[1][1] == rockId);
        check("int export: empty is 0", first.intGrid[2][4] == 0);

        // Applying maps ids back to letters.
        m.applyImportedLayers(parsed, true, true);
        check("int import: ids map back to letters", m.getCell(0, 0, 0) == 'g' && m.getCell(0, 1, 1) == 'r');
        check("int import: layer type kept", m.getLayers().get(0).isIntType());
    }

    private static void testLoaderExport()
    {
        EditorModel m = seeded(5, 3);
        m.setActiveLayerIndex(1); // Props

        m.beginStroke();
        m.setCell(0, 0, 'c'); // Cloud, a PROP
        m.endStroke();

        m.setActiveLayerIndex(2); // Markers
        m.beginStroke();
        m.setCell(1, 1, 'E'); // Enemy Spawn, a MARKER
        m.endStroke();

        String loaders = LoaderExporter.exportLoaders(m, "");
        check("loaders: singularised class name", loaders.contains("List<Prop> propList"));
        check("loaders: marker layer too", loaders.contains("List<Marker> markerList"));
        check("loaders: switch case per letter", loaders.contains("case 'c' -> propList.add(new Prop(\"Cloud\", row, col));"));
        check("loaders: grid variable matches export", loaders.contains("for (int row = 0; row < Props.length; row++)"));
        check("loaders: terrain-only layer skipped", !loaders.contains("List<Terrain>"));

        EditorModel bare = seeded(4, 4);
        check("loaders: empty map explains itself",
                LoaderExporter.exportLoaders(bare, "").contains("No Prop or Marker tiles found"));
    }

    private static void testImportUnknownLetters()
    {
        EditorModel m = seeded(4, 4);
        int before = m.getPalette().size();

        String source = "char[][] cave = { {'z','z'}, {'z','q'} };";
        List<ParsedLayer> parsed = MapImporter.parseArrays(source);

        m.applyImportedLayers(parsed, true, true);
        check("import: replaces layer stack", m.getLayers().size() == 1);
        check("import: resized to fit", m.getCols() == 2 && m.getRows() == 2);
        check("import: unknown letters added", m.getPalette().size() == before + 2);
        check("import: added tiles have ids", m.findTile('z').id > 0 && m.findTile('q').id > 0);
        check("import: distinct ids", m.findTile('z').id != m.findTile('q').id);

        m.undo();
        check("import: undoable", m.getLayers().size() == 3 && m.getCols() == 4);
    }

    private static void testPaletteIORoundTrip() throws Exception
    {
        EditorModel m = seeded(4, 4);

        TileDef fancy = new TileDef('@', "Fancy", TileDef.Category.MARKER, new Color(12, 34, 56, 200));
        fancy.id = 77;
        fancy.layerName = "Markers";
        fancy.spritePath = "/tmp/fancy.png";
        fancy.sprite = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        fancy.sprite.setRGB(0, 0, 0xFFAA1122);
        m.addTile(fancy);

        File file = File.createTempFile("palette", ".tmepal");
        file.deleteOnExit();

        PaletteIO.savePalette(m.getPalette(), file);
        List<TileDef> loaded = PaletteIO.loadPalette(file);

        check("paletteIO: tile count", loaded.size() == m.getPalette().size());

        TileDef back = null;
        for (TileDef t : loaded) if (t.letter == '@') back = t;

        check("paletteIO: letter survives", back != null);
        check("paletteIO: name survives", back.name.equals("Fancy"));
        check("paletteIO: id survives", back.id == 77);
        check("paletteIO: category survives", back.category == TileDef.Category.MARKER);
        check("paletteIO: colour survives (incl. alpha)", back.colour.getRGB() == fancy.colour.getRGB());
        check("paletteIO: layer restriction survives", "Markers".equals(back.layerName));
        check("paletteIO: sprite path survives", "/tmp/fancy.png".equals(back.spritePath));
        check("paletteIO: sprite survives", back.sprite != null
                && back.sprite.getRGB(0, 0) == 0xFFAA1122);

        TileDef ground = null;
        for (TileDef t : loaded) if (t.letter == 'g') ground = t;
        check("paletteIO: plain tile survives", ground != null && ground.name.equals("Ground"));

        // A file that isn't a palette must be rejected, not silently accepted.
        File junk = File.createTempFile("junk", ".txt");
        junk.deleteOnExit();
        java.nio.file.Files.writeString(junk.toPath(), "hello world");
        boolean threw = false;
        try { PaletteIO.loadPalette(junk); } catch (Exception e) { threw = true; }
        check("paletteIO: rejects non-palette file", threw);
    }

    private static void testImporterTolerance()
    {
        // Comments, trailing commas and odd whitespace.
        String messy = """
                // a leading comment with { braces } in it
                char[][] terrain = {
                    {'s','s','s'}, // trailing comment
                    /* block */ {'g','g','g'},
                };
                """;
        List<ParsedLayer> parsed = MapImporter.parseArrays(messy);
        check("importer: tolerates comments", parsed.size() == 1 && parsed.get(0).rows == 2);
        check("importer: cells intact", parsed.get(0).grid[1][0] == 'g');

        // Compact rows.
        List<ParsedLayer> compact = MapImporter.parseArrays("char[][] m = { {sgd}, {rrr} };");
        check("importer: compact rows", compact.get(0).cols == 3 && compact.get(0).grid[0][1] == 'g');

        // Bare initialiser with no declaration.
        List<ParsedLayer> bare = MapImporter.parseArrays("{ {'a','b'}, {'c','d'} }");
        check("importer: bare initialiser", bare.size() == 1 && bare.get(0).grid[1][1] == 'd');
        check("importer: names bare layers", bare.get(0).name.equals("Imported 1"));

        // Ragged rows are padded and flagged.
        List<ParsedLayer> ragged = MapImporter.parseArrays("char[][] r = { {'a','b','c'}, {'d'} };");
        check("importer: pads ragged rows", ragged.get(0).cols == 3
                && ragged.get(0).grid[1][2] == EditorModel.EMPTY);
        check("importer: flags padding", ragged.get(0).padded);

        // C-style declaration with dimensions in the brackets.
        List<ParsedLayer> cStyle = MapImporter.parseArrays("char level[2][2] = { {'a','b'}, {'c','d'} };");
        check("importer: C-style declaration", cStyle.size() == 1 && cStyle.get(0).name.equals("level"));

        // Two declarations become two layers.
        List<ParsedLayer> two = MapImporter.parseArrays(
                "char[][] one = { {'a'} }; char[][] two = { {'b'} };");
        check("importer: multiple declarations", two.size() == 2
                && two.get(1).name.equals("two"));

        // Nothing usable must raise a readable error rather than return empty.
        boolean threw = false;
        String message = "";
        try { MapImporter.parseArrays("public void update() { move(); }"); }
        catch (IllegalArgumentException e) { threw = true; message = e.getMessage(); }
        check("importer: rejects non-array source", threw && message.contains("No char[][] arrays found"));

        boolean unbalanced = false;
        try { MapImporter.parseArrays("char[][] x = { {'a','b'}, "); }
        catch (IllegalArgumentException e) { unbalanced = true; }
        check("importer: reports unbalanced braces", unbalanced);
    }

    // ---- Helpers ---------------------------------------------------------

    /** A model with a default map and the same palette the editor seeds. */
    private static EditorModel seeded(int cols, int rows)
    {
        EditorModel m = new EditorModel();
        m.newMap(cols, rows, 16);

        seed(m, 's', "Sky", TileDef.Category.BACKGROUND, new Color(135, 206, 235));
        seed(m, 'g', "Ground", TileDef.Category.TERRAIN, new Color(34, 139, 34));
        seed(m, 'd', "Dirt", TileDef.Category.TERRAIN, new Color(160, 82, 45));
        seed(m, 'r', "Rock", TileDef.Category.TERRAIN, new Color(128, 128, 128));
        seed(m, 'c', "Cloud", TileDef.Category.PROP, new Color(245, 245, 245));
        seed(m, 'E', "Enemy Spawn", TileDef.Category.MARKER, new Color(220, 40, 40));

        m.setSelectedLetter('g');
        return m;
    }

    private static void seed(EditorModel m, char letter, String name, TileDef.Category cat, Color colour)
    {
        TileDef t = new TileDef(letter, name, cat, colour);
        t.id = m.nextFreeId();
        m.addTile(t);
    }

    private static void check(String label, boolean condition)
    {
        if (condition)
        {
            passed++;
        }
        else
        {
            failed++;
            System.out.println("FAIL  " + label);
        }
    }
}
