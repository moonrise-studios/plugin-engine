package games.negative.engine.util;

import java.util.Optional;

/**
 * A utility class that wraps a boolean value and provides functional-style operations
 * for conditional execution and value mapping based on the boolean state.
 *
 * <p>This class is similar to {@link Optional} but specifically designed for boolean values,
 * offering convenient methods for executing actions conditionally and mapping values
 * based on the true/false state.</p>
 *
 * <p>The class uses a singleton pattern for the two possible instances ({@code true} and {@code false})
 * to ensure memory efficiency and reference equality for instances with the same boolean value.</p>
 *
 * <h3>Usage Examples:</h3>
 * <pre>{@code
 * OptionalBool condition = OptionalBool.of(someCondition);
 *
 * // Conditional execution
 * condition.ifTrue(() -> System.out.println("Condition is true"));
 * condition.ifTrueOrElse(
 *     () -> performTrueAction(),
 *     () -> performFalseAction()
 * );
 *
 * // Value mapping
 * Optional<String> result = condition.mapIfTrue(() -> "Success");
 * String value = condition.mapIfTrueOrElse(
 *     () -> "True value",
 *     () -> "False value"
 * );
 * }</pre>
 */
public class OptionalBool {

    private static final OptionalBool TRUE = new OptionalBool(true);
    private static final OptionalBool FALSE = new OptionalBool(false);

    private final boolean value;

    /**
     * Private constructor to enforce singleton pattern.
     *
     * @param value the boolean value to wrap
     */
    private OptionalBool(boolean value) {
        this.value = value;
    }

    /**
     * Creates an {@code OptionalBool} instance for the given boolean value.
     *
     * <p>This method returns one of two singleton instances to ensure memory efficiency
     * and allow reference equality comparisons.</p>
     *
     * @param value the boolean value to wrap
     * @return {@code OptionalBool.TRUE} if value is {@code true},
     *         {@code OptionalBool.FALSE} if value is {@code false}
     */
    public static OptionalBool of(boolean value) {
        return value ? TRUE : FALSE;
    }

    /**
     * Checks if the wrapped boolean value is {@code true}.
     *
     * @return {@code true} if the wrapped value is {@code true}, {@code false} otherwise
     */
    public boolean isTrue() {
        return value;
    }

    /**
     * Checks if the wrapped boolean value is {@code false}.
     *
     * @return {@code true} if the wrapped value is {@code false}, {@code false} otherwise
     */
    public boolean isFalse() {
        return !value;
    }

    /**
     * Executes the given action if the wrapped boolean value is {@code true}.
     *
     * <p>If the value is {@code false}, no action is performed.</p>
     *
     * @param action the action to execute if the value is {@code true}
     * @throws NullPointerException if action is null and the value is {@code true}
     */
    public void ifTrue(Runnable action) {
        if (value) action.run();
    }

    /**
     * Executes the given action if the wrapped boolean value is {@code false}.
     *
     * <p>If the value is {@code true}, no action is performed.</p>
     *
     * @param action the action to execute if the value is {@code false}
     * @throws NullPointerException if action is null and the value is {@code false}
     */
    public void ifFalse(Runnable action) {
        if (!value) action.run();
    }

    /**
     * Executes one of two actions based on the wrapped boolean value.
     *
     * <p>If the value is {@code true}, the {@code trueAction} is executed.
     * If the value is {@code false}, the {@code falseAction} is executed.</p>
     *
     * @param trueAction the action to execute if the value is {@code true}
     * @param falseAction the action to execute if the value is {@code false}
     * @throws NullPointerException if the corresponding action is null
     */
    public void ifTrueOrElse(Runnable trueAction, Runnable falseAction) {
        if (value) {
            trueAction.run();
        } else {
            falseAction.run();
        }
    }

    /**
     * Maps the wrapped boolean to a value using the provided callback if the value is {@code true}.
     *
     * <p>If the wrapped value is {@code true}, the callback is executed and its result
     * is wrapped in an {@link Optional}. If the wrapped value is {@code false},
     * an empty {@code Optional} is returned.</p>
     *
     * @param <T> the type of the value to be returned
     * @param callback the callback to execute if the value is {@code true}
     * @return an {@code Optional} containing the callback result if value is {@code true},
     *         or an empty {@code Optional} if value is {@code false}
     * @throws NullPointerException if callback is null and the value is {@code true}
     */
    public <T> Optional<T> mapIfTrue(Callback<T> callback) {
        if (isFalse()) return Optional.empty();

        return Optional.of(callback.apply());
    }

    /**
     * Maps the wrapped boolean to a value using the provided callback if the value is {@code false}.
     *
     * <p>If the wrapped value is {@code false}, the callback is executed and its result
     * is wrapped in an {@link Optional}. If the wrapped value is {@code true},
     * an empty {@code Optional} is returned.</p>
     *
     * @param <T> the type of the value to be returned
     * @param callback the callback to execute if the value is {@code false}
     * @return an {@code Optional} containing the callback result if value is {@code false},
     *         or an empty {@code Optional} if value is {@code true}
     * @throws NullPointerException if callback is null and the value is {@code false}
     */
    public <T> Optional<T> mapIfFalse(Callback<T> callback) {
        if (isTrue()) return Optional.empty();

        return Optional.of(callback.apply());
    }

    /**
     * Maps the wrapped boolean to a value using one of two callbacks based on the boolean state.
     *
     * <p>If the wrapped value is {@code true}, the {@code trueCallback} is executed and its result returned.
     * If the wrapped value is {@code false}, the {@code falseCallback} is executed and its result returned.</p>
     *
     * @param <T> the type of the value to be returned
     * @param trueCallback the callback to execute if the value is {@code true}
     * @param falseCallback the callback to execute if the value is {@code false}
     * @return the result of the appropriate callback
     * @throws NullPointerException if the corresponding callback is null
     */
    public <T> T mapIfTrueOrElse(Callback<T> trueCallback, Callback<T> falseCallback) {
        if (isTrue()) {
            return trueCallback.apply();
        } else {
            return falseCallback.apply();
        }
    }

    /**
     * Indicates whether some other object is "equal to" this {@code OptionalBool}.
     *
     * <p>Two {@code OptionalBool} instances are considered equal if they wrap the same boolean value.</p>
     *
     * @param obj the reference object with which to compare
     * @return {@code true} if this object is the same as the obj argument; {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof OptionalBool && ((OptionalBool) obj).value == value;
    }

    /**
     * Returns a hash code value for this {@code OptionalBool}.
     *
     * <p>The hash code is based on the wrapped boolean value.</p>
     *
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        return Boolean.hashCode(value);
    }

    /**
     * Returns a string representation of this {@code OptionalBool}.
     *
     * <p>The string representation consists of the class name followed by
     * the wrapped boolean value in square brackets.</p>
     *
     * @return a string representation of this {@code OptionalBool}
     */
    @Override
    public String toString() {
        return "OptionalBool[" + value + "]";
    }
}

