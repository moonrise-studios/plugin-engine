package gg.moonrise.engine.paper.dialog.canvas.theme;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

/**
 * A Bedrock resource pack holding a theme's fixed artwork, such as icons, for Bedrock form buttons.
 * <p>
 * Bedrock forms cannot draw the Java font artwork, but each form button can show a texture from a
 * Bedrock pack. Install the {@link #zip() .mcpack} where Geyser loads packs, usually its {@code packs}
 * folder, then let the menu name the textures with {@link #texturePath(String)}; see
 * {@link gg.moonrise.engine.paper.dialog.canvas.DialogMenu#bedrockImage}. Every texture sits at
 * {@code textures/dialog/<namespace>/<theme id>/<asset>.png}, apart from textures other packs convert
 * from Java ones. The form screen shows images at 32 x 32, so 32 pixel artwork stays crisp. The build is deterministic; its version changes
 * whenever the artwork does, so clients that cached an older copy download it again.
 */
public final class BedrockPackExport {

    private static final int[] MIN_ENGINE_VERSION = {1, 20, 0};

    private final DialogTheme theme;
    private final SortedMap<String, byte[]> files;

    private BedrockPackExport(DialogTheme theme, SortedMap<String, byte[]> files) {
        this.theme = theme;
        this.files = Collections.unmodifiableSortedMap(files);
    }

    /**
     * Builds the pack from a theme's fixed assets.
     * @param theme the theme
     * @return the pack
     */
    public static BedrockPackExport of(DialogTheme theme) {
        Objects.requireNonNull(theme, "theme");
        SortedMap<String, byte[]> textures = new TreeMap<>();
        for (Map.Entry<String, ThemeAsset> asset : theme.assets().entrySet()) {
            if (asset.getValue().kind() != ThemeAsset.Kind.FIXED) continue;

            textures.put(texturePath(theme, asset.getKey()) + ".png", ThemeCompiler.png(asset.getValue().image()));
        }

        byte[] digest = digest(textures);
        SortedMap<String, byte[]> files = new TreeMap<>(textures);
        files.put("manifest.json", manifest(theme, digest));
        return new BedrockPackExport(theme, files);
    }

    /**
     * The path a Bedrock form image uses for one of a theme's assets.
     * @param theme the theme
     * @param asset the asset name
     * @return the path, without the {@code .png} extension
     */
    public static String texturePath(DialogTheme theme, String asset) {
        return "textures/dialog/" + theme.namespace() + "/" + theme.id() + "/" + asset;
    }

    /**
     * The path a Bedrock form image uses for one of this pack's assets.
     * @param asset the asset name
     * @return the path, or {@code null} when the pack has no such texture
     */
    public String texturePath(String asset) {
        String path = texturePath(theme, asset);
        return files.containsKey(path + ".png") ? path : null;
    }

    /**
     * Every file of the pack keyed by its path inside it.
     * @return the files, sorted by path
     */
    public SortedMap<String, byte[]> files() {
        return files;
    }

    /**
     * The pack as a deterministic {@code .mcpack} archive.
     * @return the archive bytes
     */
    public byte[] zip() {
        return ResourcePackExport.zip(files);
    }

    /**
     * Writes the {@code .mcpack} unless the file already holds exactly this pack.
     * @param file the target file
     * @return whether the file was written
     */
    public boolean writeZip(Path file) {
        Objects.requireNonNull(file, "file");
        byte[] zip = zip();
        try {
            if (Files.isRegularFile(file) && Arrays.equals(Files.readAllBytes(file), zip)) return false;

            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.write(file, zip);
            return true;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write Bedrock pack to " + file, e);
        }
    }

    private static byte[] manifest(DialogTheme theme, byte[] digest) {
        String seed = "moonrise-dialog-bedrock:" + theme.namespace() + ":" + theme.id();
        JsonArray version = version(digest);

        JsonObject header = new JsonObject();
        header.addProperty("name", theme.packDescription());
        header.addProperty("description", "Dialog artwork " + theme.namespace() + ":" + theme.id());
        header.addProperty("uuid", UUID.nameUUIDFromBytes((seed + ":header").getBytes(StandardCharsets.UTF_8)).toString());
        header.add("version", version);
        JsonArray minEngine = new JsonArray();
        for (int part : MIN_ENGINE_VERSION) minEngine.add(part);
        header.add("min_engine_version", minEngine);

        JsonObject module = new JsonObject();
        module.addProperty("type", "resources");
        module.addProperty("uuid", UUID.nameUUIDFromBytes((seed + ":resources").getBytes(StandardCharsets.UTF_8)).toString());
        module.add("version", version.deepCopy());
        JsonArray modules = new JsonArray();
        modules.add(module);

        JsonObject manifest = new JsonObject();
        manifest.addProperty("format_version", 2);
        manifest.add("header", header);
        manifest.add("modules", modules);
        return new GsonBuilder().setPrettyPrinting().create().toJson(manifest).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * {@code [1, a, b]} from the artwork's digest, so new artwork gets a new version.
     */
    private static JsonArray version(byte[] digest) {
        JsonArray version = new JsonArray();
        version.add(1);
        version.add(((digest[0] & 0xFF) << 8 | (digest[1] & 0xFF)) & 0x7FFF);
        version.add(((digest[2] & 0xFF) << 8 | (digest[3] & 0xFF)) & 0x7FFF);
        return version;
    }

    private static byte[] digest(SortedMap<String, byte[]> textures) {
        ByteArrayOutputStream all = new ByteArrayOutputStream();
        for (Map.Entry<String, byte[]> entry : textures.entrySet()) {
            all.writeBytes(entry.getKey().getBytes(StandardCharsets.UTF_8));
            all.writeBytes(entry.getValue());
        }

        return ResourcePackExport.sha1(all.toByteArray());
    }

    /**
     * Hex SHA-1 of the archive, handy for logs.
     * @return the hash
     */
    public String sha1() {
        return HexFormat.of().formatHex(ResourcePackExport.sha1(zip()));
    }
}
