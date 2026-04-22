package gg.moonrise.engine.util;

/**
 * Represents the Callback interface.
 */

@FunctionalInterface
public interface Callback<T> {

    T apply();

}
