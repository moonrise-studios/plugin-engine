package gg.moonrise.engine.paper.dialog.canvas.theme;

import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * Background panel artwork, described as a nine-slice image.
 * <p>
 * The corners are kept as-is and the edges and centre are tiled to the theme width and the number of
 * text lines a screen uses.
 */
public final class ThemePanel {

    private final BufferedImage image;
    private final int corner;

    private ThemePanel(BufferedImage image, int corner) {
        this.image = image;
        this.corner = corner;
    }

    /**
     * Creates a panel from nine-slice artwork.
     * @param image the artwork
     * @param corner the corner size in pixels, at most one text line (9 pixels)
     * @return the panel
     */
    public static ThemePanel nineSlice(BufferedImage image, int corner) {
        Objects.requireNonNull(image, "image");
        if (corner < 0 || corner > 9) {
            throw new IllegalArgumentException("Panel corner size must be between 0 and 9 pixels");
        }
        if (image.getWidth() < corner * 2 + 1 || image.getHeight() < corner * 2 + 1) {
            throw new IllegalArgumentException("Panel artwork must be larger than both of its corners");
        }

        return new ThemePanel(ThemeAsset.fixed(image).image(), corner);
    }

    /**
     * Generates a flat panel with a one pixel border and clipped corners.
     * @param fillArgb the fill colour as ARGB
     * @param borderArgb the border colour as ARGB
     * @return the panel
     */
    public static ThemePanel generated(int fillArgb, int borderArgb) {
        BufferedImage image = new BufferedImage(5, 5, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                boolean corner = (x == 0 || x == 4) && (y == 0 || y == 4);
                boolean edge = x == 0 || x == 4 || y == 0 || y == 4;
                if (corner) continue;

                image.setRGB(x, y, edge ? borderArgb : fillArgb);
            }
        }

        return new ThemePanel(image, 2);
    }

    public int corner() {
        return corner;
    }

    /**
     * Renders the panel at a target size.
     * @param width the target width in pixels
     * @param height the target height in pixels
     * @return the rendered panel
     */
    public BufferedImage render(int width, int height) {
        if (width < corner * 2 + 1 || height < corner * 2 + 1) {
            throw new IllegalArgumentException("Panel target size is smaller than its corners");
        }

        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            int sourceY = sourceCoordinate(y, height, image.getHeight());
            for (int x = 0; x < width; x++) {
                target.setRGB(x, y, image.getRGB(sourceCoordinate(x, width, image.getWidth()), sourceY));
            }
        }

        return target;
    }

    private int sourceCoordinate(int target, int targetSize, int sourceSize) {
        if (target < corner) return target;
        if (target >= targetSize - corner) return sourceSize - (targetSize - target);

        int middle = sourceSize - corner * 2;
        return corner + (target - corner) % middle;
    }
}
