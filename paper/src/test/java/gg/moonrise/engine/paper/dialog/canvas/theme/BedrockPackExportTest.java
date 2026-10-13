package gg.moonrise.engine.paper.dialog.canvas.theme;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BedrockPackExportTest {

    @Test
    public void holdsTheFixedArtworkAtThemedPathsWithAManifest() {
        BedrockPackExport pack = BedrockPackExport.of(theme(0xFFFFD700));

        assertTrue(pack.files().containsKey("textures/dialog/moonrise/icons/icon_star.png"));
        assertTrue(pack.files().containsKey("textures/dialog/moonrise/icons/back.png"), "the default back arrow is fixed artwork too");
        assertFalse(pack.files().keySet().stream().anyMatch(path -> path.contains("/button")), "stretchable artwork has no Bedrock form use");
        assertEquals("textures/dialog/moonrise/icons/icon_star", pack.texturePath("icon_star"));
        assertNull(pack.texturePath("button"));

        JsonObject manifest = JsonParser.parseString(new String(pack.files().get("manifest.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        assertEquals(2, manifest.get("format_version").getAsInt());
        assertEquals("resources", manifest.getAsJsonArray("modules").get(0).getAsJsonObject().get("type").getAsString());
        assertEquals(3, manifest.getAsJsonObject("header").getAsJsonArray("version").size());
    }

    @Test
    public void isDeterministicAndVersionsNewArtwork() {
        BedrockPackExport gold = BedrockPackExport.of(theme(0xFFFFD700));

        assertArrayEquals(gold.zip(), BedrockPackExport.of(theme(0xFFFFD700)).zip());
        assertNotEquals(version(gold), version(BedrockPackExport.of(theme(0xFF00AAFF))));
        assertEquals(header(gold), header(BedrockPackExport.of(theme(0xFF00AAFF))), "the pack keeps its identity across versions");
    }

    @Test
    public void writesTheArchiveOnlyWhenItChanged(@TempDir Path directory) {
        BedrockPackExport pack = BedrockPackExport.of(theme(0xFFFFD700));
        Path file = directory.resolve("packs/icons.mcpack");

        assertTrue(pack.writeZip(file));
        assertFalse(pack.writeZip(file));
    }

    private static DialogTheme theme(int argb) {
        BufferedImage star = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        star.setRGB(3, 3, argb);
        return DialogTheme.builder("moonrise", "icons").asset("icon_star", ThemeAsset.fixed(star)).build();
    }

    private static String version(BedrockPackExport pack) {
        return manifest(pack).getAsJsonObject("header").get("version").toString();
    }

    private static String header(BedrockPackExport pack) {
        return manifest(pack).getAsJsonObject("header").get("uuid").getAsString();
    }

    private static JsonObject manifest(BedrockPackExport pack) {
        return JsonParser.parseString(new String(pack.files().get("manifest.json"), StandardCharsets.UTF_8)).getAsJsonObject();
    }
}
