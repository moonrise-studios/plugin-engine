package games.negative.engine.command;

import games.negative.engine.util.Callback;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;

/**
 * Represents the CloudArgument interface.
 */

public interface CloudArgument<S, T> extends ArgumentParser<S, T> {

    /**
     * Gets the name of the argument.
     * @return The name of the argument, or null if not specified.
     */
    default String name() {
        return null;
    }

    /**
     * Gets the type of the argument.
     * @return The class type of the argument.
     */
    Class<T> getType();

    /**
     * Helper method to compute an ArgumentParseResult based on a value and a failure callback.
     * @param value The value to check for success.
     * @param onFailure The callback to invoke if the value is null.
     * @return An ArgumentParseResult representing success or failure.
     */
    default ArgumentParseResult<T> resultOrThrow(T value, Callback<Throwable> onFailure) {
        if (value != null) {
            return ArgumentParseResult.success(value);
        } else {
            Throwable throwable = onFailure.apply();
            return ArgumentParseResult.failure(throwable);
        }
    }
}
