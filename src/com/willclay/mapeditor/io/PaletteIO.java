package com.willclay.mapeditor.io;

import com.willclay.mapeditor.domain.TileDef;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Reads and writes the palette as a simple key=value text format. Sprites are
 * embedded as base64 PNG so a palette file is self-contained and portable.
 */
public class PaletteIO
{
    public static final String EXTENSION = "tmepal";

    private static final String HEADER = "TMEPALETTE v1";
    private static final String TILE_START = "TILE";
    private static final String TILE_END = "END";

    public static void savePalette(List<TileDef> palette, File file) throws IOException
    {
        try (PrintWriter out = new PrintWriter(new FileWriter(file)))
        {
            out.println(HEADER);

            for (TileDef tile : palette)
            {
                out.println(TILE_START);
                out.println("letter=" + tile.letter);
                out.println("name=" + tile.name);
                out.println("id=" + tile.id);
                out.println("category=" + tile.category.name());
                out.println("colour=" + tile.colour.getRGB());

                if (tile.layerName != null)
                {
                    out.println("layer=" + tile.layerName);
                }

                if (tile.spritePath != null)
                {
                    out.println("spritePath=" + tile.spritePath);
                }

                if (tile.sprite != null)
                {
                    out.println("sprite=" + encodeSprite(tile));
                }

                out.println(TILE_END);
            }
        }
    }

    public static List<TileDef> loadPalette(File file) throws IOException
    {
        List<TileDef> loaded = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(file)))
        {
            String line = br.readLine();
            if (line == null || !line.startsWith("TMEPALETTE"))
            {
                throw new IOException("Not a Palette File!");
            }

            TileDef cur = null;
            while ((line = br.readLine()) != null)
            {
                if (line.equals(TILE_START))
                {
                    cur = new TileDef('?', "", TileDef.Category.TERRAIN, Color.GRAY);
                }
                else if (line.equals(TILE_END))
                {
                    if (cur != null) loaded.add(cur);
                    cur = null;
                }
                else if (cur != null)
                {
                    readField(cur, line);
                }
            }
        }

        return loaded;
    }

    private static void readField(TileDef tile, String line) throws IOException
    {
        int eq = line.indexOf('=');
        if (eq < 0) return;

        String key = line.substring(0, eq);
        String val = line.substring(eq + 1);

        try
        {
            switch (key)
            {
                case "letter"     -> tile.letter = val.isEmpty() ? '?' : val.charAt(0);
                case "name"       -> tile.name = val;
                case "id"         -> tile.id = Integer.parseInt(val);
                case "category"   -> tile.category = TileDef.Category.valueOf(val);
                case "colour"     -> tile.colour = new Color(Integer.parseInt(val), true);
                case "layer"      -> tile.layerName = val;
                case "spritePath" -> tile.spritePath = val;
                case "sprite"     -> tile.sprite = decodeSprite(val);
            }
        }
        catch (IllegalArgumentException ex)
        {
            // Covers both a bad number and an unknown category name.
            throw new IOException("Bad value for \"" + key + "\": " + val);
        }
    }

    private static String encodeSprite(TileDef tile) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(tile.sprite, "png", out);

        return Base64.getEncoder().encodeToString(out.toByteArray());
    }

    private static java.awt.image.BufferedImage decodeSprite(String encoded) throws IOException
    {
        byte[] bytes = Base64.getDecoder().decode(encoded);

        return ImageIO.read(new ByteArrayInputStream(bytes));
    }
}