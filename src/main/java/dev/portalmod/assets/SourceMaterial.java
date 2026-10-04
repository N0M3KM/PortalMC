package dev.portalmod.assets;

import java.util.regex.Pattern;

/** Minimal read-only VMT interpretation. Opaque texture alpha may be a shader mask, not opacity. */
public record SourceMaterial(String texture, boolean translucent, boolean alphaTest, boolean eye) {
    public static SourceMaterial parse(String text) {
        String cleaned = text.replaceAll("(?m)//[^\\r\\n]*", "");
        String base = value(cleaned, "basetexture"), iris = value(cleaned, "iris");
        return new SourceMaterial(base == null ? iris : base, flag(cleaned, "translucent"), flag(cleaned, "alphatest"), base == null && iris != null);
    }
    private static boolean flag(String text, String key) { return "1".equals(value(text, key)); }
    private static String value(String text, String key) {
        var m = Pattern.compile("(?i)\\\"?\\$" + key + "\\\"?\\s+(?:\\\"([^\\\"]+)\\\"|([^\\s{}]+))").matcher(text);
        return m.find() ? (m.group(1) == null ? m.group(2) : m.group(1)) : null;
    }
    public int pixel(int argb) { return translucent || alphaTest ? argb : argb | 0xff000000; }
}
