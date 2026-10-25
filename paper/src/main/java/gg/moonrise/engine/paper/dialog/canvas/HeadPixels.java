package gg.moonrise.engine.paper.dialog.canvas;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Objects;

/**
 * The 8 x 8 face of a player skin, with the hat layer composited on top.
 */
public final class HeadPixels {

    private static final HeadPixels PLACEHOLDER = placeholder(0x8A8A8A, 0x5E5E5E);

    private final int[] rgb;

    private HeadPixels(int[] rgb) {
        this.rgb = rgb;
    }

    /**
     * Creates a face from 64 RGB values in row-major order.
     * @param rgb the colours
     * @return the face
     */
    public static HeadPixels of(int[] rgb) {
        Objects.requireNonNull(rgb, "rgb");
        if (rgb.length != 64) throw new IllegalArgumentException("A face needs exactly 64 pixels");

        int[] copy = new int[64];
        for (int i = 0; i < 64; i++) {
            copy[i] = rgb[i] & 0xFFFFFF;
        }
        return new HeadPixels(copy);
    }

    /**
     * Extracts the face from a skin texture.
     * @param skin a 64 x 64 or legacy 64 x 32 skin
     * @return the face
     */
    public static HeadPixels fromSkin(BufferedImage skin) {
        Objects.requireNonNull(skin, "skin");
        if (skin.getWidth() < 64 || skin.getHeight() < 16) {
            throw new IllegalArgumentException("Not a player skin: " + skin.getWidth() + "x" + skin.getHeight());
        }

        boolean hat = !legacyOpaqueHat(skin);
        int[] rgb = new int[64];
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int face = skin.getRGB(8 + x, 8 + y);
                rgb[y * 8 + x] = hat ? blend(face, skin.getRGB(40 + x, 8 + y)) : face & 0xFFFFFF;
            }
        }

        return new HeadPixels(rgb);
    }

    /**
     * Neutral face shown while a skin is still loading.
     * @return the placeholder
     */
    public static HeadPixels placeholder() {
        return PLACEHOLDER;
    }

    /**
     * Returns the colour of one pixel.
     * @param x the column, from 0 to 7
     * @param y the row, from 0 to 7
     * @return the RGB colour
     */
    public int rgb(int x, int y) {
        return rgb[y * 8 + x];
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof HeadPixels pixels && Arrays.equals(rgb, pixels.rgb);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(rgb);
    }

    private static HeadPixels placeholder(int fill, int border) {
        int[] rgb = new int[64];
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                boolean edge = x == 0 || y == 0 || x == 7 || y == 7;
                rgb[y * 8 + x] = edge ? border : fill;
            }
        }

        return new HeadPixels(rgb);
    }

    /**
     * Vanilla ignores the hat layer of a legacy 64 x 32 skin whose hat area is fully opaque.
     */
    private static boolean legacyOpaqueHat(BufferedImage skin) {
        if (skin.getHeight() != 32) return false;

        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                if ((skin.getRGB(40 + x, 8 + y) >>> 24) != 255) return false;
            }
        }
        return true;
    }

    private static int blend(int base, int overlay) {
        int alpha = overlay >>> 24;
        if (alpha == 0) return base & 0xFFFFFF;
        if (alpha == 255) return overlay & 0xFFFFFF;

        int red = mix(base >> 16 & 0xFF, overlay >> 16 & 0xFF, alpha);
        int green = mix(base >> 8 & 0xFF, overlay >> 8 & 0xFF, alpha);
        int blue = mix(base & 0xFF, overlay & 0xFF, alpha);
        return red << 16 | green << 8 | blue;
    }

    private static int mix(int base, int overlay, int alpha) {
        return (overlay * alpha + base * (255 - alpha)) / 255;
    }
}
