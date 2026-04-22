package gg.moonrise.engine;

import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface Plugin {

    /**
     * Gets the plugin's data directory.
     * @return the directory
     */
    Path directory();

    /**
     * Invokes beans of the specified class, passing them to the provided consumer.
     * @param clazz the class of the beans to invoke
     * @param consumer the consumer to process each bean
     * @param onFailure the consumer to handle exceptions that occur during bean retrieval or processing
     * @param <T> the type of the beans to invoke
     */
    <T> void fetchBeans(Class<T> clazz, Consumer<T> consumer, BiConsumer<T, Exception> onFailure);

    /**
     * Invokes beans of the specified class, passing them to the provided consumer. Exceptions that occur during bean retrieval or processing are ignored.
     * @param clazz the class of the beans to invoke
     * @param consumer the consumer to process each bean
     * @param <T> the type of the beans to invoke
     */
    <T> void fetchBeans(Class<T> clazz, Consumer<T> consumer);

}
