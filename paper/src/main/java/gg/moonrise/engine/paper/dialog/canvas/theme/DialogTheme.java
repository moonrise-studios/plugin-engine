package gg.moonrise.engine.paper.dialog.canvas.theme;

import net.kyori.adventure.text.format.TextColor;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Visual definition of themed dialog menus: panel, named artwork, colours, and canvas size.
 * <p>
 * A theme is compiled once into a resource pack plus matching glyph metadata, see {@link #compiled()}.
 * Compilation is deterministic, so the same theme always produces the same pack and pack id.
 */
public final class DialogTheme {

    public static final String TAB = "tab";
    public static final String TAB_SELECTED = "tab_selected";
    public static final String BUTTON = "button";
    public static final String BUTTON_SELECTED = "button_selected";
    public static final String BACK = "back";
    public static final String DIVIDER = "divider";

    private static final Pattern RESOURCE_PART = Pattern.compile("[a-z0-9_.-]+");

    private final String namespace;
    private final String id;
    private final int width;
    private final int padding;
    private final int maxLines;
    private final ThemePanel panel;
    private final Map<String, ThemeAsset> assets;
    private final TextColor textColor;
    private final TextColor mutedColor;
    private final TextColor accentColor;
    private final TextColor selectedTextColor;
    private final boolean textShadow;
    private final PackFormat packFormat;
    private final String packDescription;

    private volatile CompiledTheme compiled;

    private DialogTheme(Builder builder) {
        this.namespace = builder.namespace;
        this.id = builder.id;
        this.width = builder.width;
        this.padding = builder.padding;
        this.maxLines = builder.maxLines;
        this.panel = builder.panel;
        this.assets = Collections.unmodifiableMap(new LinkedHashMap<>(builder.assets));
        this.textColor = builder.textColor;
        this.mutedColor = builder.mutedColor;
        this.accentColor = builder.accentColor;
        this.selectedTextColor = builder.selectedTextColor;
        this.textShadow = builder.textShadow;
        this.packFormat = builder.packFormat;
        this.packDescription = builder.packDescription;
    }

    /**
     * Starts a theme pre-populated with the default artwork and colours.
     * @param namespace the resource pack namespace, for example your plugin id
     * @param id the theme id, unique within the namespace
     * @return the builder
     */
    public static Builder builder(String namespace, String id) {
        return new Builder(namespace, id);
    }

    /**
     * Compiles this theme, caching the result.
     * @return the compiled theme
     */
    public CompiledTheme compiled() {
        CompiledTheme result = compiled;
        if (result != null) return result;

        synchronized (this) {
            if (compiled == null) compiled = ThemeCompiler.compile(this);
            return compiled;
        }
    }

    public String namespace() {
        return namespace;
    }

    public String id() {
        return id;
    }

    /**
     * Full canvas width in pixels, including the panel padding.
     * @return the width
     */
    public int width() {
        return width;
    }

    /**
     * Horizontal padding between the panel edge and content, in pixels.
     * @return the padding
     */
    public int padding() {
        return padding;
    }

    /**
     * Width available to content, in pixels.
     * @return the content width
     */
    public int contentWidth() {
        return width - padding * 2;
    }

    /**
     * Maximum number of text lines a screen may use, including panel padding lines.
     * @return the line limit
     */
    public int maxLines() {
        return maxLines;
    }

    public ThemePanel panel() {
        return panel;
    }

    public Map<String, ThemeAsset> assets() {
        return assets;
    }

    public TextColor textColor() {
        return textColor;
    }

    public TextColor mutedColor() {
        return mutedColor;
    }

    public TextColor accentColor() {
        return accentColor;
    }

    public TextColor selectedTextColor() {
        return selectedTextColor;
    }

    public boolean textShadow() {
        return textShadow;
    }

    public PackFormat packFormat() {
        return packFormat;
    }

    public String packDescription() {
        return packDescription;
    }

    /**
     * Fluent builder for {@link DialogTheme}.
     */
    public static final class Builder {

        private final String namespace;
        private final String id;
        private final Map<String, ThemeAsset> assets = new LinkedHashMap<>();
        private int width = 240;
        private int padding = 10;
        private int maxLines = 40;
        private ThemePanel panel = ThemePanel.generated(0xE0141420, 0xFFD4A537);
        private TextColor textColor = TextColor.color(0xE8E8F0);
        private TextColor mutedColor = TextColor.color(0xA0A0B4);
        private TextColor accentColor = TextColor.color(0xFFC94A);
        private TextColor selectedTextColor = TextColor.color(0xFFD866);
        private boolean textShadow = true;
        private PackFormat packFormat = PackFormat.DEFAULT;
        private String packDescription;

        private Builder(String namespace, String id) {
            this.namespace = requireResourcePart(namespace, "namespace");
            this.id = requireResourcePart(id, "id");
            this.packDescription = "Dialog theme " + namespace + ":" + id;

            assets.put(TAB, ThemeAsset.button(0xFF262636, 0xFF56566E, 13));
            assets.put(TAB_SELECTED, ThemeAsset.button(0xFF3A301C, 0xFFD4A537, 13));
            assets.put(BUTTON, ThemeAsset.button(0xFF262636, 0xFF56566E, 13));
            assets.put(BUTTON_SELECTED, ThemeAsset.button(0xFF3A301C, 0xFFD4A537, 13));
            assets.put(BACK, ThemeAsset.fixed(backArrow(0xFFD4A537)));
            assets.put(DIVIDER, ThemeAsset.line(0x80D4A537));
        }

        /**
         * Sets the full canvas width, including padding.
         * @param width the width in pixels, between 32 and 1000
         * @return this builder
         */
        public Builder width(int width) {
            if (width < 32 || width > 1000) throw new IllegalArgumentException("Theme width must be between 32 and 1000");

            this.width = width;
            return this;
        }

        public Builder padding(int padding) {
            if (padding < 0) throw new IllegalArgumentException("Theme padding must not be negative");

            this.padding = padding;
            return this;
        }

        public Builder maxLines(int maxLines) {
            if (maxLines < 3 || maxLines > 200) throw new IllegalArgumentException("Theme max lines must be between 3 and 200");

            this.maxLines = maxLines;
            return this;
        }

        public Builder panel(ThemePanel panel) {
            this.panel = Objects.requireNonNull(panel, "panel");
            return this;
        }

        /**
         * Adds or replaces a named asset.
         * @param name the asset name, see the constants on {@link DialogTheme}
         * @param asset the asset
         * @return this builder
         */
        public Builder asset(String name, ThemeAsset asset) {
            assets.put(requireResourcePart(name, "asset name"), Objects.requireNonNull(asset, "asset"));
            return this;
        }

        public Builder textColor(TextColor textColor) {
            this.textColor = Objects.requireNonNull(textColor, "textColor");
            return this;
        }

        public Builder mutedColor(TextColor mutedColor) {
            this.mutedColor = Objects.requireNonNull(mutedColor, "mutedColor");
            return this;
        }

        public Builder accentColor(TextColor accentColor) {
            this.accentColor = Objects.requireNonNull(accentColor, "accentColor");
            return this;
        }

        public Builder selectedTextColor(TextColor selectedTextColor) {
            this.selectedTextColor = Objects.requireNonNull(selectedTextColor, "selectedTextColor");
            return this;
        }

        public Builder textShadow(boolean textShadow) {
            this.textShadow = textShadow;
            return this;
        }

        public Builder packFormat(PackFormat packFormat) {
            this.packFormat = Objects.requireNonNull(packFormat, "packFormat");
            return this;
        }

        public Builder packDescription(String packDescription) {
            this.packDescription = Objects.requireNonNull(packDescription, "packDescription");
            return this;
        }

        public DialogTheme build() {
            if (padding * 2 >= width) throw new IllegalArgumentException("Theme padding leaves no room for content");

            return new DialogTheme(this);
        }

        private static BufferedImage backArrow(int argb) {
            String[] rows = {
                    "...#.....",
                    "..##.....",
                    ".########",
                    "#########",
                    ".########",
                    "..##.....",
                    "...#....."
            };
            BufferedImage image = new BufferedImage(rows[0].length(), rows.length, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < rows.length; y++) {
                for (int x = 0; x < rows[y].length(); x++) {
                    if (rows[y].charAt(x) == '#') image.setRGB(x, y, argb);
                }
            }

            return image;
        }

        private static String requireResourcePart(String value, String name) {
            Objects.requireNonNull(value, name);
            if (!RESOURCE_PART.matcher(value).matches()) {
                throw new IllegalArgumentException("Theme " + name + " must match [a-z0-9_.-]+: " + value);
            }

            return value;
        }
    }
}
