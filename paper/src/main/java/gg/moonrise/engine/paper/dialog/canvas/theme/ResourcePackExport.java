package gg.moonrise.engine.paper.dialog.canvas.theme;

import net.kyori.adventure.resource.ResourcePackInfo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * The resource pack generated for a compiled theme.
 * <p>
 * Generation and delivery are separate: export the pack as a directory to merge into an existing pack
 * workflow, or as a ZIP to host yourself, then send it with {@link #info(URI)}.
 */
public final class ResourcePackExport {

    /**
     * Path of the pack metadata file inside the pack.
     */
    public static final String PACK_META = "pack.mcmeta";

    private static final LocalDateTime ZIP_TIMESTAMP = LocalDateTime.of(1980, 1, 1, 0, 0);

    private final SortedMap<String, byte[]> files;
    private final UUID packId;
    private volatile byte[] zip;

    public ResourcePackExport(Map<String, byte[]> files, UUID packId) {
        this.files = Collections.unmodifiableSortedMap(new TreeMap<>(Objects.requireNonNull(files, "files")));
        this.packId = Objects.requireNonNull(packId, "packId");
    }

    /**
     * Returns every generated file keyed by its path inside the pack.
     * @return the files, sorted by path
     */
    public SortedMap<String, byte[]> files() {
        return files;
    }

    /**
     * Stable pack id; changes only when the generated content changes.
     * @return the pack id
     */
    public UUID packId() {
        return packId;
    }

    /**
     * Writes the pack into a directory, including {@code pack.mcmeta}.
     * @param directory the target directory
     */
    public void writeDirectory(Path directory) {
        writeDirectory(directory, true);
    }

    /**
     * Writes the pack into a directory.
     * @param directory the target directory
     * @param includePackMeta whether to write {@code pack.mcmeta}; pass {@code false} when merging into a pack that has its own
     */
    public void writeDirectory(Path directory, boolean includePackMeta) {
        Objects.requireNonNull(directory, "directory");
        try {
            for (Map.Entry<String, byte[]> entry : files.entrySet()) {
                if (!includePackMeta && entry.getKey().equals(PACK_META)) continue;

                Path target = directory.resolve(entry.getKey());
                Files.createDirectories(target.getParent());
                Files.write(target, entry.getValue());
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write dialog theme pack to " + directory, e);
        }
    }

    /**
     * Makes a directory hold exactly this pack: writes files that are missing or different and deletes
     * every other file in it. Use it for a directory dedicated to this theme inside another plugin's
     * pack sources, such as Nexo's {@code pack/external_packs/<name>}, and regenerate that pack when
     * {@link SyncResult#changed()} is true.
     * @param directory the dedicated target directory
     * @param includePackMeta whether to write {@code pack.mcmeta}
     * @return what changed
     */
    public SyncResult syncDirectory(Path directory, boolean includePackMeta) {
        Objects.requireNonNull(directory, "directory");
        int written = 0;
        int deleted = 0;
        try {
            Files.createDirectories(directory);
            Set<Path> expected = new HashSet<>();
            for (Map.Entry<String, byte[]> entry : files.entrySet()) {
                if (!includePackMeta && entry.getKey().equals(PACK_META)) continue;

                Path target = directory.resolve(entry.getKey()).normalize();
                expected.add(target);
                if (Files.isRegularFile(target) && Arrays.equals(Files.readAllBytes(target), entry.getValue())) continue;

                Files.createDirectories(target.getParent());
                Files.write(target, entry.getValue());
                written++;
            }

            List<Path> existing;
            try (Stream<Path> walk = Files.walk(directory)) {
                existing = walk.filter(Files::isRegularFile).map(Path::normalize).toList();
            }
            for (Path file : existing) {
                if (expected.contains(file)) continue;

                Files.delete(file);
                deleted++;
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to sync dialog theme pack to " + directory, e);
        }

        return new SyncResult(written, deleted);
    }

    /**
     * Builds the pack as a deterministic ZIP archive.
     * @return the archive bytes
     */
    public byte[] zip() {
        byte[] result = zip;
        if (result != null) return result.clone();

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream out = new ZipOutputStream(bytes)) {
            for (Map.Entry<String, byte[]> entry : files.entrySet()) {
                ZipEntry zipEntry = new ZipEntry(entry.getKey());
                zipEntry.setTimeLocal(ZIP_TIMESTAMP);
                out.putNextEntry(zipEntry);
                out.write(entry.getValue());
                out.closeEntry();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to build dialog theme pack archive", e);
        }

        zip = bytes.toByteArray();
        return zip.clone();
    }

    /**
     * SHA-1 of {@link #zip()}, as clients expect when downloading a pack.
     * @return the lowercase hex hash
     */
    public String sha1() {
        return HexFormat.of().formatHex(sha1(zip()));
    }

    /**
     * Writes {@link #zip()} to a file.
     * @param file the target file
     * @return the file
     */
    public Path writeZip(Path file) {
        Objects.requireNonNull(file, "file");
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.write(file, zip());
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write dialog theme pack archive to " + file, e);
        }
    }

    /**
     * Creates the info needed to send the hosted ZIP to players.
     * @param uri where the ZIP from {@link #zip()} is hosted
     * @return the pack info, using {@link #packId()} so pack-loaded checks recognise it
     */
    public ResourcePackInfo info(URI uri) {
        return ResourcePackInfo.resourcePackInfo(packId, Objects.requireNonNull(uri, "uri"), sha1());
    }

    public static byte[] sha1(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-1").digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 is not available", e);
        }
    }

    /**
     * Outcome of {@link #syncDirectory}.
     * @param written files created or replaced
     * @param deleted stale files removed
     */
    public record SyncResult(int written, int deleted) {

        /**
         * Returns whether the directory changed, so the pack built from it needs regenerating.
         * @return whether anything was written or deleted
         */
        public boolean changed() {
            return written > 0 || deleted > 0;
        }
    }
}
