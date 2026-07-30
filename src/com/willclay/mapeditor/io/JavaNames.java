package com.willclay.mapeditor.io;

/** Turns free-text layer and tile names into valid Java identifiers and literals. */
public final class JavaNames
{
    private static final String FALLBACK_PREFIX = "layer_";

    private JavaNames() {}

    /** Make an identifier-safe name, prefixing it if it would start with a digit. */
    public static String toIdentifier(String s)
    {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < s.length(); i++)
        {
            char c = s.charAt(i);
            sb.append(Character.isLetterOrDigit(c) || c == '_' ? c : '_');
        }

        if (sb.isEmpty() || Character.isDigit(sb.charAt(0)))
        {
            sb.insert(0, FALLBACK_PREFIX);
        }

        return sb.toString();
    }

    /** The variable a layer's grid is declared as, honouring an optional prefix. */
    public static String gridVariable(String layerName, String classPrefix)
    {
        return toIdentifier(classPrefix.isEmpty() ? layerName : classPrefix + "_" + layerName);
    }

    /** Derive a class name from a layer name: "Props" becomes "Prop". */
    public static String classNameFor(String layerName)
    {
        String id = toIdentifier(layerName.trim());

        // Drop a prefix toIdentifier may have added; it isn't part of the name.
        if (id.startsWith(FALLBACK_PREFIX) && id.length() > FALLBACK_PREFIX.length())
        {
            id = id.substring(FALLBACK_PREFIX.length());
        }
        if (id.isEmpty()) id = "Entity";

        // Singularise a trailing plural, but leave words like "Grass" alone.
        if (id.length() > 1 && (id.endsWith("s") || id.endsWith("S")) && !id.endsWith("ss"))
        {
            id = id.substring(0, id.length() - 1);
        }

        return Character.toUpperCase(id.charAt(0)) + id.substring(1);
    }

    public static String lowerFirst(String s)
    {
        if (s.isEmpty()) return s;

        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    /** Escape a value for use inside a Java string literal. */
    public static String escapeString(String s)
    {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < s.length(); i++)
        {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') sb.append('\\');
            sb.append(c);
        }

        return sb.toString();
    }

    /** Escape a value for use inside a Java char literal. */
    public static String escapeChar(char c)
    {
        return (c == '\'' || c == '\\') ? "\\" + c : String.valueOf(c);
    }
}