package gg.moonrise.engine.paper.dialog.canvas.theme;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ThemeCompilerTest {

    @Test
    public void bitmapAdvanceMatchesVanillaRule() {
        assertEquals(49, ThemeCompiler.bitmapAdvance(opaque(48, 9), 9));

        BufferedImage trailingTransparent = new BufferedImage(10, 9, BufferedImage.TYPE_INT_ARGB);
        trailingTransparent.setRGB(5, 3, 0xFFFFFFFF);
        assertEquals(7, ThemeCompiler.bitmapAdvance(trailingTransparent, 9));

        assertEquals(1, ThemeCompiler.bitmapAdvance(new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB), 4));

        BufferedImage pixel = new BufferedImage(1, 8, BufferedImage.TYPE_INT_ARGB);
        pixel.setRGB(0, 0, 0xFFFFFFFF);
        assertEquals(3, ThemeCompiler.bitmapAdvance(pixel, 16));
    }

    @Test
    public void compilesDefaultThemeIntoPackAndMetadata() {
        CompiledTheme compiled = DialogTheme.builder("moonrise", "test").build().compiled();
        ResourcePackExport pack = compiled.pack();

        assertEquals("moonrise:dialog/test", compiled.font().asString());
        assertTrue(pack.files().containsKey("pack.mcmeta"));
        assertTrue(pack.files().containsKey("assets/moonrise/font/dialog/test.json"));
        assertTrue(pack.files().containsKey("assets/moonrise/textures/dialog/test/panel_top.png"));
        assertTrue(pack.files().containsKey("assets/moonrise/textures/dialog/test/head_row_7.png"));
        assertTrue(pack.files().containsKey("dialog-theme.moonrise.test.json"));

        for (String asset : List.of(DialogTheme.TAB, DialogTheme.TAB_SELECTED, DialogTheme.BUTTON,
                DialogTheme.BUTTON_SELECTED, DialogTheme.BACK, DialogTheme.DIVIDER)) {
            compiled.asset(asset);
        }
        assertThrows(IllegalArgumentException.class, () -> compiled.asset("missing"));
    }

    @Test
    public void fontDefinitionsRespectClientConstraints() {
        CompiledTheme compiled = DialogTheme.builder("moonrise", "test").build().compiled();
        JsonObject font = JsonParser.parseString(new String(
                compiled.pack().files().get("assets/moonrise/font/dialog/test.json"), StandardCharsets.UTF_8)).getAsJsonObject();

        int bitmaps = 0;
        for (JsonElement element : font.getAsJsonArray("providers")) {
            JsonObject provider = element.getAsJsonObject();
            if (!provider.get("type").getAsString().equals("bitmap")) continue;

            bitmaps++;
            int height = provider.get("height").getAsInt();
            int ascent = provider.get("ascent").getAsInt();
            assertTrue(ascent <= height, "ascent " + ascent + " exceeds height " + height);

            String file = provider.get("file").getAsString();
            String path = "assets/moonrise/textures/" + file.substring(file.indexOf(':') + 1);
            assertTrue(compiled.pack().files().containsKey(path), "missing texture " + path);
        }
        assertTrue(bitmaps > 0);

        JsonObject meta = JsonParser.parseString(new String(compiled.pack().files().get("pack.mcmeta"), StandardCharsets.UTF_8))
                .getAsJsonObject().getAsJsonObject("pack");
        assertEquals(PackFormat.MINECRAFT_1_21_6, meta.get("pack_format").getAsInt());
        assertEquals(PackFormat.MINECRAFT_1_21_6, meta.get("min_format").getAsInt());
        assertEquals(99, meta.get("max_format").getAsInt());
    }

    @Test
    public void compilationIsDeterministic() {
        CompiledTheme first = DialogTheme.builder("moonrise", "test").build().compiled();
        CompiledTheme second = DialogTheme.builder("moonrise", "test").build().compiled();
        CompiledTheme different = DialogTheme.builder("moonrise", "test").width(300).build().compiled();

        assertEquals(first.buildHash(), second.buildHash());
        assertEquals(first.packId(), second.packId());
        assertArrayEquals(first.pack().zip(), second.pack().zip());
        assertEquals(first.pack().sha1(), second.pack().sha1());
        assertNotEquals(first.packId(), different.packId());
    }

    @Test
    public void spacesEncodeAnyOffset() {
        CompiledTheme compiled = DialogTheme.builder("moonrise", "test").build().compiled();

        for (int offset = -1500; offset <= 1500; offset += 7) {
            int total = 0;
            for (char c : compiled.spaces(offset).toCharArray()) {
                total += compiled.advance(c).orElseThrow();
            }
            assertEquals(offset, total);
        }
        assertEquals("", compiled.spaces(0));
    }

    @Test
    public void stretchableAssetsCoverRequestedWidthWithoutGaps() {
        CompiledAsset button = DialogTheme.builder("moonrise", "test").build().compiled().asset(DialogTheme.BUTTON);

        for (int width = button.minimumWidth(); width < 400; width += 13) {
            List<PlacedGlyph> glyphs = button.layout(width);
            int expectedX = 0;
            for (PlacedGlyph glyph : glyphs) {
                assertEquals(expectedX, glyph.x());
                expectedX += glyph.glyph().width();
            }
            assertEquals(width, expectedX);
        }
    }

    @Test
    public void panelCoversEveryLine() {
        CompiledTheme compiled = DialogTheme.builder("moonrise", "test").maxLines(12).build().compiled();

        List<PlacedGlyph> panel = compiled.panel(5);
        assertEquals(5, panel.size());
        for (int line = 0; line < 5; line++) {
            assertEquals(7 - line * CompiledTheme.LINE_HEIGHT, panel.get(line).glyph().ascent());
        }
        assertThrows(IllegalArgumentException.class, () -> compiled.panel(13));
    }

    @Test
    public void widePanelsAreSplitIntoAtlasSizedChunks() {
        CompiledTheme compiled = DialogTheme.builder("moonrise", "wide").width(600).build().compiled();

        List<PlacedGlyph> top = compiled.panel(2).subList(0, 3);
        assertEquals(List.of(0, 256, 512), top.stream().map(PlacedGlyph::x).toList());
        assertEquals(88, top.get(2).glyph().width());
    }

    @Test
    public void rejectsAssetsDrawnAboveTheirAscentLimit() {
        DialogTheme theme = DialogTheme.builder("moonrise", "bad")
                .asset("icon", ThemeAsset.fixed(opaque(4, 4)).topOffset(0))
                .build();

        assertThrows(IllegalArgumentException.class, theme::compiled);
    }

    @Test
    public void rejectsAssetsWhoseTexturesCollide() {
        DialogTheme theme = DialogTheme.builder("moonrise", "clash")
                .asset("tab_left", ThemeAsset.fixed(opaque(4, 4)))
                .build();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, theme::compiled);
        assertTrue(error.getMessage().contains("moonrise:clash"), error.getMessage());
        assertTrue(error.getMessage().contains("assets/moonrise/textures/dialog/clash/asset_tab_left.png"), error.getMessage());
    }

    @Test
    public void exportsDirectoryAndZip() throws Exception {
        CompiledTheme compiled = DialogTheme.builder("moonrise", "test").build().compiled();
        Path directory = Files.createTempDirectory("dialog-theme");

        compiled.pack().writeDirectory(directory.resolve("pack"), false);
        Path zip = compiled.pack().writeZip(directory.resolve("pack.zip"));

        assertTrue(Files.exists(directory.resolve("pack/assets/moonrise/font/dialog/test.json")));
        assertTrue(Files.notExists(directory.resolve("pack/pack.mcmeta")));
        assertArrayEquals(compiled.pack().zip(), Files.readAllBytes(zip));
        assertEquals(compiled.packId(), compiled.pack().info(java.net.URI.create("https://example.com/pack.zip")).id());
    }

    private static BufferedImage opaque(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, 0xFFFFFFFF);
            }
        }

        return image;
    }

    @Test
    public void syncDirectoryWritesOnlyChangesAndRemovesStaleFiles(@TempDir Path directory) throws IOException {
        ResourcePackExport pack = DialogTheme.builder("moonrise", "sync").build().compiled().pack();

        ResourcePackExport.SyncResult first = pack.syncDirectory(directory, false);
        assertTrue(first.changed());
        assertFalse(Files.exists(directory.resolve(ResourcePackExport.PACK_META)));

        Path stale = directory.resolve("assets/moonrise/textures/dialog/sync/old.png");
        Files.write(stale, new byte[]{1});
        Path staleTheme = directory.resolve("assets/moonrise/textures/dialog/old");
        Files.createDirectories(staleTheme.resolve("icons"));
        Files.write(staleTheme.resolve("icons/star.png"), new byte[]{1});
        ResourcePackExport.SyncResult second = pack.syncDirectory(directory, false);

        assertEquals(new ResourcePackExport.SyncResult(0, 2), second);
        assertFalse(Files.exists(stale));
        assertFalse(Files.exists(staleTheme), "directories left empty are removed");
        assertTrue(Files.isDirectory(directory.resolve("assets/moonrise/textures/dialog/sync")));
        assertFalse(pack.syncDirectory(directory, false).changed());
    }
}
