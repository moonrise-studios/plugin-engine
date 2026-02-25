package games.negative.engine.util;

/**
 * Represents the Callback interface.
 */

@FunctionalInterface
public interface Callback<T> {

    T apply();

}
