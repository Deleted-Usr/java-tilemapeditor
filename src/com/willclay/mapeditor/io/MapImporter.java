package com.willclay.mapeditor.io;

import com.willclay.mapeditor.core.EditorModel;
import com.willclay.mapeditor.domain.ParsedLayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Parses pasted Java source containing one or more char[][] or int[][]
 * declarations back into layer grids. Handles the editor's own output as well
 * as the project's TileMap.java style, and is tolerant of comments, trailing
 * commas and whitespace. Each top-level { ... } holding row-braces becomes one
 * layer; ragged rows are padded so the result is always rectangular.
 */
public class MapImporter
{
    private static final String CHAR_TYPE = "char";
    private static final String INT_TYPE = "int";

    /**
     * Parse every array declaration found in the text.
     *
     * @throws IllegalArgumentException with a human-readable reason when
     *         nothing valid is found.
     */
    public static List<ParsedLayer> parseArrays(String text)
    {
        String src = stripComments(text);

        List<ParsedLayer> result = parseDeclarations(src);
        if (result.isEmpty()) { result = parseBareInitialisers(src); }

        if (result.isEmpty())
        {
            throw new IllegalArgumentException(
                    "No char[][] arrays found. Expected e.g. char[][] map = { {'s','g'}, ... }; "
                            + "or a bare { {...}, {...} } block.");
        }

        return result;
    }

    /**
     * Find every "char[][] name = { ... }" declaration and parse its
     * initialiser. Going by declaration avoids mistaking a class or method body
     * for an array.
     */
    private static List<ParsedLayer> parseDeclarations(String src)
    {
        List<ParsedLayer> result = new ArrayList<>();
        int searchFrom = 0;

        while (true)
        {
            int declChar = indexOfArrayType(src, searchFrom, CHAR_TYPE);
            int declInt = indexOfArrayType(src, searchFrom, INT_TYPE);
            if (declChar < 0 && declInt < 0) break;

            // Take whichever typed declaration comes first in the source.
            boolean isInt = declChar < 0 || (declInt >= 0 && declInt < declChar);
            int decl = isInt ? declInt : declChar;

            int eq = src.indexOf('=', decl);
            int brace = src.indexOf('{', decl);
            if (brace < 0) break;

            if (eq >= 0 && eq > brace)
            {
                // Malformed; step past this type token so we don't loop forever.
                searchFrom = decl + CHAR_TYPE.length();
                continue;
            }

            String name = extractNameBetween(src, decl, brace);
            int close = matchBrace(src, brace);
            if (close < 0)
            {
                throw new IllegalArgumentException("Unbalanced { } braces after array declaration.");
            }

            String block = src.substring(brace + 1, close);
            ParsedLayer layer = isInt
                    ? parseIntBlock(block, name, result.size())
                    : parseCharBlock(block, name, result.size());
            if (layer != null) result.add(layer);

            searchFrom = close + 1;
        }

        return result;
    }

    /** Fallback for copy-pasted "{ {...}, {...} }" blocks with no declaration. */
    private static List<ParsedLayer> parseBareInitialisers(String src)
    {
        List<ParsedLayer> result = new ArrayList<>();
        int i = 0;

        while (i < src.length())
        {
            int brace = src.indexOf('{', i);
            if (brace < 0) break;

            int close = matchBrace(src, brace);
            if (close < 0) throw new IllegalArgumentException("Unbalanced { } braces in input.");

            String block = src.substring(brace + 1, close);
            if (looksLikeRowList(block))
            {
                ParsedLayer layer = parseCharBlock(block, null, result.size());
                if (layer != null) result.add(layer);
            }

            i = close + 1;
        }

        return result;
    }

    /**
     * Index of a standalone "char[][]" (or "char [ROWS][COLS]") type token at
     * or after {@code from}, else -1.
     */
    private static int indexOfArrayType(String s, int from, String word)
    {
        int idx = from;

        while (true)
        {
            int c = s.indexOf(word, idx);
            if (c < 0) return -1;

            // Make sure it's a whole word, not part of "charm" or "println".
            boolean wordStart = (c == 0) || !Character.isLetterOrDigit(s.charAt(c - 1));
            int after = c + word.length();
            boolean wordEnd = after >= s.length() || !Character.isLetterOrDigit(s.charAt(after));

            if (wordStart && wordEnd && hasArrayDimensions(s, after)) return c;

            idx = c + word.length();
        }
    }

    /**
     * True when a type token is followed by at least two bracket groups, in
     * either shape:
     *   Java:  char[][] name
     *   C/C++: char name[ROWS][COLS]
     */
    private static boolean hasArrayDimensions(String s, int from)
    {
        if (countBracketGroups(s, from) >= 2) return true;

        int nameEnd = skipIdentifier(s, from);

        return nameEnd > from && countBracketGroups(s, nameEnd) >= 2;
    }

    /** Skip whitespace then one identifier, or return {@code from} if there isn't one. */
    private static int skipIdentifier(String s, int from)
    {
        int i = from;
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;

        int start = i;
        while (i < s.length() && (Character.isLetterOrDigit(s.charAt(i)) || s.charAt(i) == '_')) i++;

        return i > start ? i : from;
    }

    /** Count the [] / [ROWS] groups following a type token. */
    private static int countBracketGroups(String s, int from)
    {
        int groups = 0;
        int i = from;

        while (i < s.length())
        {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) { i++; continue; }
            if (c != '[') break;

            // Skip over an optional dimension expression.
            int k = i + 1;
            while (k < s.length() && s.charAt(k) != ']' && s.charAt(k) != '{') k++;
            if (k >= s.length() || s.charAt(k) != ']') break;

            groups++;
            i = k + 1;
        }

        return groups;
    }

    /** True when a block holds only row-braces and separators, so it isn't code. */
    private static boolean looksLikeRowList(String block)
    {
        if (block.indexOf('{') < 0) return false;

        int i = 0;
        while (i < block.length())
        {
            char c = block.charAt(i);

            if (c == '{')
            {
                int close = matchBrace(block, i);
                if (close < 0) return false;
                i = close + 1;
            }
            else if (Character.isWhitespace(c) || c == ',')
            {
                i++;
            }
            else
            {
                return false; // Stray token, so this isn't a pure row list.
            }
        }

        return true;
    }

    private static ParsedLayer parseCharBlock(String block, String name, int index)
    {
        List<char[]> rowList = new ArrayList<>();
        for (String rowText : splitRows(block, "row")) { rowList.add(parseCharRow(rowText)); }
        if (rowList.isEmpty()) return null;

        int maxCols = 0;
        for (char[] row : rowList) { maxCols = Math.max(maxCols, row.length); }

        boolean padded = false;
        char[][] grid = new char[rowList.size()][maxCols];

        for (int r = 0; r < rowList.size(); r++)
        {
            char[] row = rowList.get(r);
            if (row.length != maxCols) padded = true;

            Arrays.fill(grid[r], EditorModel.EMPTY);
            System.arraycopy(row, 0, grid[r], 0, row.length);
        }

        ParsedLayer out = new ParsedLayer(layerName(name, index), grid, rowList.size(), maxCols);
        out.padded = padded;

        return out;
    }

    private static ParsedLayer parseIntBlock(String block, String name, int index)
    {
        List<int[]> rowList = new ArrayList<>();
        for (String rowText : splitRows(block, "int row")) { rowList.add(parseIntRow(rowText)); }
        if (rowList.isEmpty()) return null;

        int maxCols = 0;
        for (int[] row : rowList) { maxCols = Math.max(maxCols, row.length); }

        boolean padded = false;
        int[][] grid = new int[rowList.size()][maxCols]; // Pads with 0, which is empty.

        for (int r = 0; r < rowList.size(); r++)
        {
            int[] row = rowList.get(r);
            if (row.length != maxCols) padded = true;

            System.arraycopy(row, 0, grid[r], 0, row.length);
        }

        ParsedLayer out = new ParsedLayer(layerName(name, index), grid, rowList.size(), maxCols);
        out.padded = padded;

        return out;
    }

    /** Split an initialiser block into the raw text of each row-brace. */
    private static List<String> splitRows(String block, String what)
    {
        List<String> rows = new ArrayList<>();
        int i = 0;

        while (i < block.length())
        {
            int open = block.indexOf('{', i);
            if (open < 0) break;

            int close = matchBrace(block, open);
            if (close < 0) throw new IllegalArgumentException("Unbalanced braces inside a " + what + ".");

            rows.add(block.substring(open + 1, close));
            i = close + 1;
        }

        return rows;
    }

    /**
     * Parse a single row. Two styles are supported:
     *   quoted:  'a','b','c'  (commas optional, \\ and \' escapes honoured)
     *   compact: abc          (each non-space character is a cell)
     * A single quote anywhere in the row selects quoted mode.
     */
    private static char[] parseCharRow(String row)
    {
        return row.indexOf('\'') >= 0 ? parseQuotedRow(row) : parseCompactRow(row);
    }

    private static char[] parseQuotedRow(String row)
    {
        StringBuilder cells = new StringBuilder();
        int i = 0;

        while (i < row.length())
        {
            if (row.charAt(i) != '\'')
            {
                i++; // Skip commas and whitespace between cells.
                continue;
            }

            i++; // Opening quote.
            if (i >= row.length()) break;

            if (row.charAt(i) == '\\' && i + 1 < row.length())
            {
                char escape = row.charAt(i + 1);
                cells.append(switch (escape)
                {
                    case 'n' -> '\n';
                    case 't' -> '\t';
                    case '0' -> EditorModel.EMPTY; // Treat \0 as an empty cell.
                    default  -> escape;            // \' \\ and friends.
                });
                i += 2;
            }
            else
            {
                cells.append(row.charAt(i));
                i++;
            }

            if (i < row.length() && row.charAt(i) == '\'') i++; // Closing quote.
        }

        return cells.toString().toCharArray();
    }

    /**
     * Every non-whitespace, non-comma character is a cell. A literal space
     * can't be told apart from formatting here, so compact rows are assumed to
     * have no intentional empty cells.
     */
    private static char[] parseCompactRow(String row)
    {
        StringBuilder cells = new StringBuilder();

        for (int i = 0; i < row.length(); i++)
        {
            char c = row.charAt(i);
            if (c != ',' && !Character.isWhitespace(c)) cells.append(c);
        }

        return cells.toString().toCharArray();
    }

    /** Parse one int row: integers separated by commas or whitespace. */
    private static int[] parseIntRow(String row)
    {
        List<Integer> values = new ArrayList<>();
        int i = 0;

        while (i < row.length())
        {
            char c = row.charAt(i);
            if (c != '-' && !Character.isDigit(c))
            {
                i++;
                continue;
            }

            int start = i;
            i++;
            while (i < row.length() && Character.isDigit(row.charAt(i))) i++;

            try
            {
                values.add(Integer.parseInt(row.substring(start, i)));
            }
            catch (NumberFormatException ex)
            {
                throw new IllegalArgumentException("Bad number in int row: " + row.substring(start, i));
            }
        }

        int[] out = new int[values.size()];
        for (int k = 0; k < out.length; k++) { out[k] = values.get(k); }

        return out;
    }

    /** Given the index of an opening brace, return the matching close index. */
    private static int matchBrace(String s, int open)
    {
        int depth = 0;
        boolean inChar = false;

        for (int i = open; i < s.length(); i++)
        {
            char c = s.charAt(i);

            if (inChar)
            {
                if (c == '\\') { i++; continue; } // Skip the escaped character.
                if (c == '\'') inChar = false;
                continue;
            }

            if (c == '\'') { inChar = true; continue; }

            if (c == '{') depth++;
            else if (c == '}' && --depth == 0) return i;
        }

        return -1;
    }

    /**
     * Read the variable name sitting between a type token and its initialiser.
     * Two shapes are handled:
     *   Java:  char[][] NAME = {
     *   C/C++: char NAME[ROWS][COLS] = {
     */
    private static String extractNameBetween(String s, int typeStart, int brace)
    {
        int i = brace - 1;
        while (i > typeStart && (Character.isWhitespace(s.charAt(i)) || s.charAt(i) == '=')) i--;

        // Walk back over any trailing [..] dimension groups.
        while (i > typeStart && s.charAt(i) == ']')
        {
            int depth = 0;
            while (i > typeStart)
            {
                char c = s.charAt(i);
                if (c == ']') depth++;
                else if (c == '[' && --depth == 0) { i--; break; }
                i--;
            }
            while (i > typeStart && Character.isWhitespace(s.charAt(i))) i--;
        }

        int end = i + 1;
        while (i > typeStart && (Character.isLetterOrDigit(s.charAt(i)) || s.charAt(i) == '_')) i--;
        int start = i + 1;

        if (start < end)
        {
            String id = s.substring(start, end).replace("]", "").replace("[", "").trim();
            if (!id.isEmpty() && !id.equals(CHAR_TYPE) && !id.equals(INT_TYPE)) return id;
        }

        return null;
    }

    /** Remove line and block comments so the parser sees only code. */
    private static String stripComments(String s)
    {
        StringBuilder sb = new StringBuilder(s.length());
        int i = 0;
        boolean inChar = false;
        boolean inString = false;

        while (i < s.length())
        {
            char c = s.charAt(i);

            if (inChar || inString)
            {
                sb.append(c);

                if (c == '\\' && i + 1 < s.length())
                {
                    sb.append(s.charAt(i + 1));
                    i += 2;
                    continue;
                }

                if (inChar && c == '\'') inChar = false;
                if (inString && c == '"') inString = false;
                i++;
                continue;
            }

            if (c == '\'' || c == '"')
            {
                inChar = (c == '\'');
                inString = (c == '"');
                sb.append(c);
                i++;
                continue;
            }

            if (c == '/' && i + 1 < s.length() && s.charAt(i + 1) == '/')
            {
                while (i < s.length() && s.charAt(i) != '\n') i++;
                continue;
            }

            if (c == '/' && i + 1 < s.length() && s.charAt(i + 1) == '*')
            {
                i += 2;
                while (i + 1 < s.length() && !(s.charAt(i) == '*' && s.charAt(i + 1) == '/')) i++;
                i += 2;
                continue;
            }

            sb.append(c);
            i++;
        }

        return sb.toString();
    }

    private static String layerName(String parsedName, int index)
    {
        return (parsedName == null || parsedName.isEmpty()) ? "Imported " + (index + 1) : parsedName;
    }
}