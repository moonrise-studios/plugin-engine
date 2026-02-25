package games.negative.engine.util;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Utility class for formatting and parsing time durations in human-readable formats.
 */
public final class TimeUtil {

    private static final Pattern COLON_FORMAT_PATTERN = Pattern.compile("^\\d+(:\\d{2})*$");

    private TimeUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Formats a duration into a human-readable string.
     *
     * @param duration The duration to format
     * @param compact  Whether to use compact format (e.g., "1d" vs "1 day")
     * @return The formatted duration string, or "0s" if duration is zero
     * @throws NullPointerException if duration is null
     */
    public static String format(Duration duration, boolean compact) {
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        List<String> parts = new ArrayList<>(4);

        if (days > 0) {
            parts.add(formatUnit(days, "d", "day", "days", compact));
        }
        if (hours > 0) {
            parts.add(formatUnit(hours, "h", "hour", "hours", compact));
        }
        if (minutes > 0) {
            parts.add(formatUnit(minutes, "m", "minute", "minutes", compact));
        }
        if (seconds > 0) {
            parts.add(formatUnit(seconds, "s", "second", "seconds", compact));
        }

        if (parts.isEmpty()) {
            return compact ? "0s" : "0 seconds";
        }

        return String.join(" ", parts);
    }

    /**
     * Formats a duration into a human-readable string using full format.
     *
     * @param duration The duration to format
     * @return The formatted duration string
     * @throws NullPointerException if duration is null
     */
    public static String format(Duration duration) {
        return format(duration, false);
    }

    /**
     * Formats a duration into colon-separated time format.
     * <p>
     * Examples:
     * <ul>
     *   <li>5 seconds → "0:05"</li>
     *   <li>3 hours 5 minutes → "3:05:00"</li>
     *   <li>1 day 3 hours 5 minutes → "1:03:05:00"</li>
     *   <li>2 days 0 hours 0 minutes 30 seconds → "2:00:00:30"</li>
     * </ul>
     *
     * @param duration The duration to format
     * @return The formatted duration in colon-separated format
     * @throws NullPointerException if duration is null
     */
    public static String formatColonSeparated(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        if (days > 0) {
            return String.format("%d:%02d:%02d:%02d", days, hours, minutes, seconds);
        } else if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%d:%02d", minutes, seconds);
        } else {
            return String.format("0:%02d", seconds);
        }
    }

    /**
     * Formats a time in milliseconds into a human-readable string.
     *
     * @param millis  Time in milliseconds to format
     * @param compact Whether to use compact format
     * @return Formatted time string
     * @throws IllegalArgumentException if millis is negative
     * @deprecated Use {@link #format(Duration, boolean)} instead
     */
    @Deprecated
    public static String format(long millis, boolean compact) {
        return format(Duration.ofMillis(millis), compact);
    }

    /**
     * Formats a time in milliseconds into a human-readable string.
     *
     * @param millis Time in milliseconds to format
     * @return Formatted time string
     * @throws IllegalArgumentException if millis is negative
     * @deprecated Use {@link #format(Duration)} instead
     */
    @Deprecated
    public static String format(long millis) {
        return format(millis, false);
    }

    /**
     * Parses a duration string into a Duration object.
     * <p>
     * Supports two formats:
     * <ul>
     *   <li>Unit format: "1d2h30m" (units: w, d, h, m, s)</li>
     *   <li>Colon format: "1:03:05:00" (days:hours:minutes:seconds)</li>
     * </ul>
     *
     * @param input The duration string to parse
     * @return The parsed Duration object
     * @throws NullPointerException     if input is null
     * @throws IllegalArgumentException if input is empty or has invalid format
     */
    public static Duration parse(String input) {
        String trimmed = input.trim();

        // Check if it's colon-separated format
        if (trimmed.contains(":")) {
            return parseColonSeparated(trimmed);
        } else {
            return parseUnitFormat(trimmed);
        }
    }

    /**
     * Parses a colon-separated duration string (e.g., "1:03:05:00").
     * <p>
     * Format can be:
     * <ul>
     *   <li>M:SS (minutes:seconds)</li>
     *   <li>H:MM:SS (hours:minutes:seconds)</li>
     *   <li>D:HH:MM:SS (days:hours:minutes:seconds)</li>
     * </ul>
     *
     * @param input The colon-separated duration string
     * @return The parsed Duration object
     * @throws IllegalArgumentException if input has invalid format
     */
    public static Duration parseColonSeparated(String input) {
        String trimmed = input.trim();

        if (!COLON_FORMAT_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException(
                    "Invalid colon format: '" + input + "'. " +
                            "Expected format: D:HH:MM:SS, H:MM:SS, or M:SS"
            );
        }

        String[] parts = trimmed.split(":");

        if (parts.length < 2 || parts.length > 4) {
            throw new IllegalArgumentException(
                    "Invalid colon format: '" + input + "'. " +
                            "Must have 2-4 components (M:SS, H:MM:SS, or D:HH:MM:SS)"
            );
        }

        try {
            long days = 0;
            long hours = 0;
            long minutes;
            long seconds;

            if (parts.length == 4) {
                // D:HH:MM:SS
                days = Long.parseLong(parts[0]);
                hours = Long.parseLong(parts[1]);
                minutes = Long.parseLong(parts[2]);
                seconds = Long.parseLong(parts[3]);
            } else if (parts.length == 3) {
                // H:MM:SS
                hours = Long.parseLong(parts[0]);
                minutes = Long.parseLong(parts[1]);
                seconds = Long.parseLong(parts[2]);
            } else {
                // M:SS
                minutes = Long.parseLong(parts[0]);
                seconds = Long.parseLong(parts[1]);
            }

            // Validate ranges
            if (hours < 0) {
                throw new IllegalArgumentException(
                        "Hours cannot be negative, got: " + hours
                );
            }
            if (parts.length == 4 && hours > 23) {
                throw new IllegalArgumentException(
                        "Hours must be between 0 and 23 in D:HH:MM:SS format, got: " + hours
                );
            }
            if (minutes < 0 || minutes > 59) {
                throw new IllegalArgumentException(
                        "Minutes must be between 0 and 59, got: " + minutes
                );
            }
            if (seconds < 0 || seconds > 59) {
                throw new IllegalArgumentException(
                        "Seconds must be between 0 and 59, got: " + seconds
                );
            }
            if (days < 0) {
                throw new IllegalArgumentException(
                        "Days cannot be negative, got: " + days
                );
            }

            return Duration.ofDays(days)
                    .plusHours(hours)
                    .plusMinutes(minutes)
                    .plusSeconds(seconds);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Invalid number in colon format: '" + input + "'", e
            );
        }
    }

    /**
     * Parses a unit-based duration string (e.g., "1d2h30m").
     *
     * @param input The unit-based duration string
     * @return The parsed Duration object
     * @throws IllegalArgumentException if input has invalid format
     */
    private static Duration parseUnitFormat(String input) {
        StringBuilder numberBuffer = new StringBuilder();
        Duration duration = Duration.ZERO;

        for (char c : input.toCharArray()) {
            if (Character.isDigit(c)) {
                numberBuffer.append(c);
            } else if (Character.isWhitespace(c)) {
                continue; // Allow whitespace
            } else {
                if (numberBuffer.isEmpty()) {
                    throw new IllegalArgumentException(
                            "Invalid duration format: unit '" + c +
                                    "' without preceding number"
                    );
                }

                long value = Long.parseLong(numberBuffer.toString());
                duration = addUnit(duration, value, c);
                numberBuffer.setLength(0);
            }
        }

        if (!numberBuffer.isEmpty()) {
            throw new IllegalArgumentException(
                    "Invalid duration format: number without unit at end"
            );
        }

        return duration;
    }

    /**
     * Converts a Duration object into a compact string format (e.g., "1d2h30m").
     *
     * @param duration The Duration object to convert
     * @return A compact string representation of the duration
     * @throws NullPointerException if duration is null
     */
    public static String toCompactString(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        StringBuilder builder = new StringBuilder();

        if (days > 0) builder.append(days).append('d');
        if (hours > 0) builder.append(hours).append('h');
        if (minutes > 0) builder.append(minutes).append('m');
        if (seconds > 0) builder.append(seconds).append('s');

        return !builder.isEmpty() ? builder.toString() : "0s";
    }

    /**
     * Parses a duration string into milliseconds.
     *
     * @param input The duration string to parse
     * @return Duration in milliseconds
     * @throws NullPointerException     if input is null
     * @throws IllegalArgumentException if input is empty or has invalid format
     * @deprecated Use {@link #parse(String)} instead
     */
    @Deprecated
    public static long fromString(String input) {
        return parse(input).toMillis();
    }

    /**
     * Formats a single time unit.
     *
     * @param value      The numeric value
     * @param compact    The compact suffix (e.g., "d")
     * @param singular   The singular form (e.g., "day")
     * @param plural     The plural form (e.g., "days")
     * @param useCompact Whether to use compact format
     * @return The formatted unit string
     */
    private static String formatUnit(
            long value,
            String compact,
            String singular,
            String plural,
            boolean useCompact
    ) {
        if (useCompact) {
            return value + compact;
        }
        return value + " " + (value == 1 ? singular : plural);
    }

    /**
     * Adds a time unit to a duration.
     *
     * @param duration The current duration
     * @param value    The value to add
     * @param unit     The unit character
     * @return The updated duration
     * @throws IllegalArgumentException if unit is not recognized
     */
    private static Duration addUnit(Duration duration, long value, char unit) {
        return switch (unit) {
            case 's', 'S' -> duration.plusSeconds(value);
            case 'm', 'M' -> duration.plusMinutes(value);
            case 'h', 'H' -> duration.plusHours(value);
            case 'd', 'D' -> duration.plusDays(value);
            case 'w', 'W' -> duration.plusDays(value * 7);
            default -> throw new IllegalArgumentException(
                    "Invalid duration unit: '" + unit + "'. " +
                            "Supported units: s, m, h, d, w"
            );
        };
    }
}
