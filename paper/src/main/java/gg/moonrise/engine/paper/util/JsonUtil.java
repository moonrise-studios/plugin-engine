package gg.moonrise.engine.paper.util;


import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Utility class for JSON operations using Gson.
 */
@Slf4j
public final class JsonUtil {

    private JsonUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Loads a JSON object from a file.
     * @param file the file to load from
     * @param clazz the class of the object to load
     * @param gson the Gson instance to use for deserialization
     * @param <T> the type of the object to load
     * @return an Optional containing the loaded object, or empty if loading failed
     */
    public static <T> Optional<T> loadFromFile(File file, Class<T> clazz, Gson gson) {
        if (file == null || !file.exists() || !file.isFile()) {
            log.error("File {} does not exist or is not a valid file", file != null ? file.getAbsolutePath() : "null");
            return Optional.empty();
        }

        try (Reader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            T object = gson.fromJson(reader, clazz);
            return Optional.ofNullable(object);
        } catch (IOException e) {
            log.error("Failed to load json from file {}", file.getAbsolutePath(), e);
            return Optional.empty();
        }
    }

    /**
     * Loads a JSON object from a file using a Type.
     * @param file the file to load from
     * @param type the Type of the object to load
     * @param gson the Gson instance to use for deserialization
     * @param <T> the type of the object to load
     * @return an Optional containing the loaded object, or empty if loading failed
     */
    public static <T> Optional<T> loadTypeFromFile(File file, Type type, Gson gson) {
        if (file == null || !file.exists() || !file.isFile()) {
            log.error("File {} does not exist or is not a valid file", file != null ? file.getAbsolutePath() : "null");
            return Optional.empty();
        }

        try (Reader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            T object = gson.fromJson(reader, type);
            return Optional.ofNullable(object);
        } catch (IOException e) {
            log.error("Failed to load json from file {}", file.getAbsolutePath(), e);
            return Optional.empty();
        }
    }

    /**
     * Loads all JSON objects from a directory.
     * @param directory the directory to load from
     * @param clazz the class of the objects to load
     * @param gson the Gson instance to use for deserialization
     * @param <T> the type of the objects to load
     * @return a collection of loaded objects
     */
    public static <T> Collection<T> loadFromDirectory(File directory, Class<T> clazz, Gson gson) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            log.error("Directory {} does not exist or is not a valid directory", directory != null ? directory.getAbsolutePath() : "null");
            return Collections.emptyList();
        }

        List<T> objects = new ArrayList<>();

        File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
        if (files == null) return objects;

        for (File file : files) {
            loadFromFile(file, clazz, gson).ifPresent(objects::add);
        }

        return objects;
    }

    /**
     * Saves a JSON object to a file.
     * @param file the file to save to
     * @param object the object to save
     * @param gson the Gson instance to use for serialization
     * @param <T> the type of the object to save
     */
    public static <T> void saveToFile(File file, T object, Gson gson) {
        if (file == null) {
            log.error("File is null, cannot save object");
            return;
        }

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                log.error("Failed to save file {}", file.getAbsolutePath(), e);
                return;
            }
        }

        try (Writer writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            gson.toJson(object, writer);
        } catch (IOException e) {
            log.error("Failed to save to file {}", file.getAbsolutePath(), e);
        }
    }

    /**
     * Saves a JSON object to a file using a Type.
     * @param file the file to save to
     * @param type the Type of the object to save
     * @param gson the Gson instance to use for serialization
     */
    public static void saveTypeToFile(File file, Type type, Gson gson) {
        try (Writer writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            gson.toJson(type, writer);
        } catch (IOException e) {
            log.error("Failed to save to file {}", file.getAbsolutePath(), e);
        }
    }

}

