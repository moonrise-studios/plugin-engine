package gg.moonrise.engine.paper.dialog.canvas.theme;

/**
 * Resource pack format range written into the generated {@code pack.mcmeta}.
 * <p>
 * Both the legacy {@code pack_format}/{@code supported_formats} fields (clients before 1.21.9) and the
 * newer {@code min_format}/{@code max_format} fields are written, so one pack covers both generations.
 * @param minFormat the oldest supported resource pack format, inclusive
 * @param maxFormat the newest supported resource pack format, inclusive
 */
public record PackFormat(int minFormat, int maxFormat) {

    /**
     * Resource pack format of Minecraft 1.21.6, the first version with dialogs.
     */
    public static final int MINECRAFT_1_21_6 = 63;

    /**
     * Default range: 1.21.6 onwards.
     */
    public static final PackFormat DEFAULT = new PackFormat(MINECRAFT_1_21_6, 99);

    public PackFormat {
        if (minFormat < 1 || maxFormat < minFormat) {
            throw new IllegalArgumentException("Invalid resource pack format range " + minFormat + ".." + maxFormat);
        }
    }

    /**
     * Creates a format range.
     * @param minFormat the oldest supported format, inclusive
     * @param maxFormat the newest supported format, inclusive
     * @return the range
     */
    public static PackFormat range(int minFormat, int maxFormat) {
        return new PackFormat(minFormat, maxFormat);
    }
}
