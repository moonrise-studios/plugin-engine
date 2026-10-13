package gg.moonrise.engine.paper.dialog.canvas.theme;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * A named piece of theme artwork, such as a button or tab background.
 * <p>
 * Fixed assets are drawn at their natural size. Stretchable assets keep their left and right caps
 * and repeat the first column after the left cap, so one small image can back a button of any width.
 * Artwork is drawn at its source resolution; scaling is deliberately not supported so borders stay crisp.
 */
public final class ThemeAsset {

    /**
     * Largest artwork height the client font atlas accepts for a single glyph.
     */
    public static final int MAX_HEIGHT = 256;

    /**
     * How an asset is laid out horizontally.
     */
    public enum Kind {
        FIXED,
        STRETCH
    }

    private final Kind kind;
    private final BufferedImage image;
    private final int leftInset;
    private final int rightInset;
    private final Integer topOffset;

    private ThemeAsset(Kind kind, BufferedImage image, int leftInset, int rightInset, Integer topOffset) {
        this.kind = kind;
        this.image = image;
        this.leftInset = leftInset;
        this.rightInset = rightInset;
        this.topOffset = topOffset;
    }

    /**
     * Creates an asset drawn at its natural size.
     * @param image the artwork
     * @return the asset
     */
    public static ThemeAsset fixed(BufferedImage image) {
        return new ThemeAsset(Kind.FIXED, validate(image), 0, 0, null);
    }

    /**
     * Creates an asset drawn at its natural size from a PNG file.
     * @param file the PNG file
     * @return the asset
     */
    public static ThemeAsset fixed(Path file) {
        return fixed(read(file));
    }

    /**
     * Creates a horizontally stretchable asset.
     * @param image the artwork
     * @param leftInset width of the left cap in pixels
     * @param rightInset width of the right cap in pixels
     * @return the asset
     */
    public static ThemeAsset stretchable(BufferedImage image, int leftInset, int rightInset) {
        BufferedImage validated = validate(image);
        if (leftInset < 0 || rightInset < 0) {
            throw new IllegalArgumentException("Stretchable asset insets must not be negative");
        }
        if (leftInset > ThemeCompiler.MAX_GLYPH_WIDTH || rightInset > ThemeCompiler.MAX_GLYPH_WIDTH) {
            throw new IllegalArgumentException("Stretchable asset caps must be at most " + ThemeCompiler.MAX_GLYPH_WIDTH + " pixels wide");
        }
        if (leftInset + rightInset >= validated.getWidth()) {
            throw new IllegalArgumentException("Stretchable asset needs at least one repeatable column between its caps");
        }

        return new ThemeAsset(Kind.STRETCH, validated, leftInset, rightInset, null);
    }

    /**
     * Creates a horizontally stretchable asset from a PNG file.
     * @param file the PNG file
     * @param leftInset width of the left cap in pixels
     * @param rightInset width of the right cap in pixels
     * @return the asset
     */
    public static ThemeAsset stretchable(Path file, int leftInset, int rightInset) {
        return stretchable(read(file), leftInset, rightInset);
    }

    /**
     * Generates a flat, stretchable button background with a one pixel border.
     * @param fillArgb the fill colour as ARGB
     * @param borderArgb the border colour as ARGB
     * @param height the button height in pixels
     * @return the asset
     */
    public static ThemeAsset button(int fillArgb, int borderArgb, int height) {
        if (height < 3 || height > MAX_HEIGHT) {
            throw new IllegalArgumentException("Button height must be between 3 and " + MAX_HEIGHT);
        }

        BufferedImage image = new BufferedImage(5, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < 5; x++) {
                boolean corner = (x == 0 || x == 4) && (y == 0 || y == height - 1);
                boolean edge = x == 0 || x == 4 || y == 0 || y == height - 1;
                if (corner) continue;

                image.setRGB(x, y, edge ? borderArgb : fillArgb);
            }
        }

        return stretchable(image, 2, 2);
    }

    /**
     * Generates a stretchable one pixel thick line through the middle of a text line, used for dividers.
     * @param argb the line colour as ARGB
     * @return the asset
     */
    public static ThemeAsset line(int argb) {
        BufferedImage image = new BufferedImage(1, 9, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 4, argb);
        return stretchable(image, 0, 0);
    }

    /**
     * Returns a copy of this asset drawn at an explicit vertical offset.
     * <p>
     * The offset is measured from the top of the text line the asset is placed on; negative values
     * draw above it. When unset, stretchable assets are centred on the line and fixed assets start at its top,
     * or sit on the text baseline when shorter than seven pixels.
     * @param topOffset the offset in pixels
     * @return the adjusted asset
     */
    public ThemeAsset topOffset(int topOffset) {
        return new ThemeAsset(kind, image, leftInset, rightInset, topOffset);
    }

    public Kind kind() {
        return kind;
    }

    /**
     * Returns a copy of the artwork.
     * @return the artwork
     */
    public BufferedImage image() {
        return copy(image);
    }

    public int width() {
        return image.getWidth();
    }

    public int height() {
        return image.getHeight();
    }

    public int leftInset() {
        return leftInset;
    }

    public int rightInset() {
        return rightInset;
    }

    /**
     * Resolves the vertical offset relative to the top of a text line.
     * @return the offset in pixels
     */
    public int resolvedTopOffset() {
        if (topOffset != null) return topOffset;
        if (kind == Kind.STRETCH) return -Math.max(0, (height() - 8) / 2);

        return Math.max(0, 7 - height());
    }

    private static BufferedImage validate(BufferedImage image) {
        Objects.requireNonNull(image, "image");
        if (image.getWidth() < 1 || image.getHeight() < 1) {
            throw new IllegalArgumentException("Theme artwork must not be empty");
        }
        if (image.getHeight() > MAX_HEIGHT) {
            throw new IllegalArgumentException("Theme artwork must be at most " + MAX_HEIGHT + " pixels high");
        }

        return copy(image);
    }

    private static BufferedImage read(Path file) {
        Objects.requireNonNull(file, "file");
        try {
            BufferedImage image = ImageIO.read(file.toFile());
            if (image == null) throw new IllegalArgumentException("Not a readable image: " + file);

            return image;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read theme artwork " + file, e);
        }
    }

    private static BufferedImage copy(BufferedImage source) {
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                copy.setRGB(x, y, source.getRGB(x, y));
            }
        }

        return copy;
    }
}
