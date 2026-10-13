package gg.moonrise.engine.paper.dialog.canvas.theme;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.kyori.adventure.key.Key;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Compiles a {@link DialogTheme} into a resource pack and the matching server-side glyph metadata.
 * <p>
 * Both outputs come from the same pass, so the font definitions in the pack and the advances used by
 * the layout code cannot drift apart. Glyph metrics follow the vanilla bitmap font provider: a glyph
 * advances by its rightmost opaque column plus one pixel, and its {@code ascent} may not exceed its height.
 */
public final class ThemeCompiler {

    /**
     * Largest glyph width that fits a single client font atlas page.
     */
    public static final int MAX_GLYPH_WIDTH = 256;

    private static final int SPACE_POWERS = 12;
    private static final int STRETCH_POWERS = 9;
    private static final int BASELINE = 7;
    private static final char FIRST_CHARACTER = '';
    private static final char LAST_CHARACTER = '';
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final DialogTheme theme;
    private final Map<String, byte[]> files = new TreeMap<>();
    private final JsonArray providers = new JsonArray();
    private final JsonObject spaceAdvances = new JsonObject();
    private final Map<Character, Integer> advances = new HashMap<>();
    private char next = FIRST_CHARACTER;

    private ThemeCompiler(DialogTheme theme) {
        this.theme = theme;
    }

    /**
     * Compiles a theme.
     * @param theme the theme
     * @return the compiled theme
     */
    public static CompiledTheme compile(DialogTheme theme) {
        return new ThemeCompiler(theme).run();
    }

    /**
     * Calculates the advance the client assigns to a single-glyph bitmap.
     * @param image the glyph image
     * @param height the configured glyph height
     * @return the advance in pixels
     */
    public static int bitmapAdvance(BufferedImage image, int height) {
        float scale = (float) height / image.getHeight();
        int actualWidth = 0;
        for (int x = image.getWidth() - 1; x >= 0 && actualWidth == 0; x--) {
            for (int y = 0; y < image.getHeight(); y++) {
                if ((image.getRGB(x, y) >>> 24) != 0) {
                    actualWidth = x + 1;
                    break;
                }
            }
        }

        return (int) (0.5 + actualWidth * scale) + 1;
    }

    private CompiledTheme run() {
        Key font = Key.key(theme.namespace(), "dialog/" + theme.id());

        char[] positiveSpaces = new char[SPACE_POWERS];
        char[] negativeSpaces = new char[SPACE_POWERS];
        for (int power = 0; power < SPACE_POWERS; power++) {
            positiveSpaces[power] = space(1 << power);
            negativeSpaces[power] = space(-(1 << power));
        }

        PanelGlyphs panel = compilePanel();

        Map<String, CompiledAsset> assets = new LinkedHashMap<>();
        theme.assets().forEach((name, asset) -> assets.put(name, compileAsset(name, asset)));

        ThemeGlyph[][] headPixels = compileHeadPixels();

        JsonObject spaceProvider = new JsonObject();
        spaceProvider.addProperty("type", "space");
        spaceProvider.add("advances", spaceAdvances);
        providers.add(spaceProvider);

        JsonObject fontJson = new JsonObject();
        fontJson.add("providers", providers);
        files.put("assets/" + theme.namespace() + "/font/dialog/" + theme.id() + ".json", json(fontJson));
        files.put(ResourcePackExport.PACK_META, json(packMeta()));

        String buildHash = buildHash();
        UUID packId = UUID.nameUUIDFromBytes(("moonrise-dialog-theme:" + theme.namespace() + ":" + theme.id() + ":" + buildHash)
                .getBytes(StandardCharsets.UTF_8));
        files.put("dialog-theme." + theme.namespace() + "." + theme.id() + ".json", json(manifest(font, buildHash, packId)));

        return new CompiledTheme(
                theme,
                font,
                assets,
                panel.top(),
                panel.middle(),
                panel.bottom(),
                headPixels,
                positiveSpaces,
                negativeSpaces,
                advances,
                new ResourcePackExport(files, packId),
                buildHash
        );
    }

    private PanelGlyphs compilePanel() {
        int lineHeight = CompiledTheme.LINE_HEIGHT;
        BufferedImage rendered = theme.panel().render(theme.width(), lineHeight * 3);
        List<Chunk> top = chunks("panel_top", rendered.getSubimage(0, 0, theme.width(), lineHeight));
        List<Chunk> middle = chunks("panel_middle", rendered.getSubimage(0, lineHeight, theme.width(), lineHeight));
        List<Chunk> bottom = chunks("panel_bottom", rendered.getSubimage(0, lineHeight * 2, theme.width(), lineHeight));

        List<PlacedGlyph> topGlyphs = placeChunks(top, 0);
        List<List<PlacedGlyph>> middleGlyphs = new ArrayList<>();
        for (int line = 1; line < theme.maxLines() - 1; line++) {
            middleGlyphs.add(placeChunks(middle, line * lineHeight));
        }
        List<List<PlacedGlyph>> bottomGlyphs = new ArrayList<>();
        for (int line = 1; line < theme.maxLines(); line++) {
            bottomGlyphs.add(placeChunks(bottom, line * lineHeight));
        }

        return new PanelGlyphs(topGlyphs, middleGlyphs, bottomGlyphs);
    }

    private CompiledAsset compileAsset(String name, ThemeAsset asset) {
        int topOffset = asset.resolvedTopOffset();
        int height = asset.height();
        if (BASELINE - topOffset > height) {
            throw new IllegalArgumentException("Dialog theme asset " + name + " is " + height
                    + " pixels high and cannot be drawn " + (-topOffset) + " pixels above its line; the client rejects an ascent above the glyph height");
        }

        BufferedImage image = asset.image();
        if (asset.kind() == ThemeAsset.Kind.FIXED) {
            List<Chunk> chunks = chunks("asset_" + name, image);
            return CompiledAsset.fixed(name, height, topOffset, placeChunks(chunks, topOffset), image.getWidth());
        }

        ThemeGlyph left = null;
        if (asset.leftInset() > 0) {
            String texture = texture("asset_" + name + "_left", image.getSubimage(0, 0, asset.leftInset(), height));
            left = glyph(texture, image.getSubimage(0, 0, asset.leftInset(), height), topOffset);
        }

        ThemeGlyph right = null;
        if (asset.rightInset() > 0) {
            BufferedImage cap = image.getSubimage(image.getWidth() - asset.rightInset(), 0, asset.rightInset(), height);
            right = glyph(texture("asset_" + name + "_right", cap), cap, topOffset);
        }

        ThemeGlyph[] middles = new ThemeGlyph[STRETCH_POWERS];
        for (int power = 0; power < STRETCH_POWERS; power++) {
            int width = 1 << power;
            BufferedImage piece = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < height; y++) {
                int argb = image.getRGB(asset.leftInset(), y);
                for (int x = 0; x < width; x++) {
                    piece.setRGB(x, y, argb);
                }
            }
            middles[power] = glyph(texture("asset_" + name + "_middle_" + width, piece), piece, topOffset);
        }

        return CompiledAsset.stretch(name, height, topOffset, left, right, middles);
    }

    private ThemeGlyph[][] compileHeadPixels() {
        String[] textures = new String[8];
        BufferedImage[] images = new BufferedImage[8];
        for (int row = 0; row < 8; row++) {
            BufferedImage image = new BufferedImage(1, 8, BufferedImage.TYPE_INT_ARGB);
            image.setRGB(0, row, 0xFFFFFFFF);
            images[row] = image;
            textures[row] = texture("head_row_" + row, image);
        }

        ThemeGlyph[][] glyphs = new ThemeGlyph[CompiledTheme.MAX_HEAD_SCALE][8];
        for (int scale = 1; scale <= CompiledTheme.MAX_HEAD_SCALE; scale++) {
            for (int row = 0; row < 8; row++) {
                glyphs[scale - 1][row] = bitmap(textures[row], images[row], 8 * scale, BASELINE);
            }
        }

        return glyphs;
    }

    private List<Chunk> chunks(String name, BufferedImage image) {
        List<Chunk> chunks = new ArrayList<>();
        for (int x = 0, index = 0; x < image.getWidth(); x += MAX_GLYPH_WIDTH, index++) {
            int width = Math.min(MAX_GLYPH_WIDTH, image.getWidth() - x);
            BufferedImage part = image.getSubimage(x, 0, width, image.getHeight());
            String suffix = image.getWidth() > MAX_GLYPH_WIDTH ? "_" + index : "";
            chunks.add(new Chunk(x, texture(name + suffix, part), part));
        }

        return chunks;
    }

    private List<PlacedGlyph> placeChunks(List<Chunk> chunks, int topOffset) {
        List<PlacedGlyph> glyphs = new ArrayList<>();
        for (Chunk chunk : chunks) {
            glyphs.add(new PlacedGlyph(chunk.x(), glyph(chunk.texture(), chunk.image(), topOffset)));
        }

        return glyphs;
    }

    private ThemeGlyph glyph(String texture, BufferedImage image, int topOffset) {
        return bitmap(texture, image, image.getHeight(), BASELINE - topOffset);
    }

    private ThemeGlyph bitmap(String texture, BufferedImage image, int height, int ascent) {
        if (ascent > height) {
            throw new IllegalStateException("Glyph ascent " + ascent + " exceeds its height " + height + " for " + texture);
        }

        char character = allocate();
        JsonObject provider = new JsonObject();
        provider.addProperty("type", "bitmap");
        provider.addProperty("file", texture);
        provider.addProperty("height", height);
        provider.addProperty("ascent", ascent);
        JsonArray chars = new JsonArray();
        chars.add(String.valueOf(character));
        provider.add("chars", chars);
        providers.add(provider);

        int advance = bitmapAdvance(image, height);
        int width = Math.round(image.getWidth() * ((float) height / image.getHeight()));
        advances.put(character, advance);
        return new ThemeGlyph(character, width, height, ascent, advance);
    }

    private char space(int advance) {
        char character = allocate();
        spaceAdvances.addProperty(String.valueOf(character), advance);
        advances.put(character, advance);
        return character;
    }

    private char allocate() {
        if (next > LAST_CHARACTER) {
            throw new IllegalStateException("Dialog theme " + theme.namespace() + ":" + theme.id() + " needs more glyphs than the private use area holds");
        }

        return next++;
    }

    private String texture(String name, BufferedImage image) {
        String path = "dialog/" + theme.id() + "/" + name + ".png";
        files.put("assets/" + theme.namespace() + "/textures/" + path, png(image));
        return theme.namespace() + ":" + path;
    }

    private JsonObject packMeta() {
        PackFormat format = theme.packFormat();
        JsonObject supported = new JsonObject();
        supported.addProperty("min_inclusive", format.minFormat());
        supported.addProperty("max_inclusive", format.maxFormat());

        JsonObject pack = new JsonObject();
        pack.addProperty("description", theme.packDescription());
        pack.addProperty("pack_format", format.minFormat());
        pack.add("supported_formats", supported);
        pack.addProperty("min_format", format.minFormat());
        pack.addProperty("max_format", format.maxFormat());

        JsonObject root = new JsonObject();
        root.add("pack", pack);
        return root;
    }

    private JsonObject manifest(Key font, String buildHash, UUID packId) {
        JsonObject manifest = new JsonObject();
        manifest.addProperty("theme", theme.namespace() + ":" + theme.id());
        manifest.addProperty("font", font.asString());
        manifest.addProperty("build", buildHash);
        manifest.addProperty("pack_id", packId.toString());
        manifest.addProperty("width", theme.width());
        manifest.addProperty("max_lines", theme.maxLines());
        manifest.addProperty("glyphs", next - FIRST_CHARACTER);
        return manifest;
    }

    private String buildHash() {
        ByteArrayOutputStream content = new ByteArrayOutputStream();
        files.forEach((path, bytes) -> {
            content.writeBytes(path.getBytes(StandardCharsets.UTF_8));
            content.write(0);
            content.writeBytes(bytes);
        });

        return HexFormat.of().formatHex(ResourcePackExport.sha1(content.toByteArray()));
    }

    private static byte[] json(JsonObject object) {
        return (GSON.toJson(object) + "\n").getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] png(BufferedImage image) {
        BufferedImage argb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                argb.setRGB(x, y, image.getRGB(x, y));
            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            if (!ImageIO.write(argb, "png", out)) throw new IllegalStateException("No PNG writer available");
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to encode dialog theme texture", e);
        }

        return out.toByteArray();
    }

    private record Chunk(int x, String texture, BufferedImage image) {
    }

    private record PanelGlyphs(List<PlacedGlyph> top, List<List<PlacedGlyph>> middle, List<List<PlacedGlyph>> bottom) {
    }
}
