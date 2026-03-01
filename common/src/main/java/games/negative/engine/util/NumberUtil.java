package games.negative.engine.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Optional;

/**
 * Represents a number utility class used to handle numbers, parse them, etc.
 */
public class NumberUtil {

    /*
     * Format a number to a fancy format.
     */
    private static final ThreadLocal<DecimalFormat> FANCY_FORMAT = ThreadLocal.withInitial(
            () -> new DecimalFormat("###,###,###,###,###.##")
    );

    private static final String SUFFIXES = "kMBTQqSsOND";

    /**
     * Parse a number to a fancy format.
     *
     * @param number Number to parse
     * @return Parsed number
     */
    public static String decimalFormat(int number) {
        return FANCY_FORMAT.get().format(number);
    }

    /**
     * Parse a number to a fancy format.
     *
     * @param number Number to parse
     * @return Parsed number
     */
    public static String decimalFormat(long number) {
        return FANCY_FORMAT.get().format(number);
    }

    /**
     * Parse a number to a fancy format.
     *
     * @param number Number to parse
     * @return Parsed number
     */
    public static String decimalFormat(double number) {
        return FANCY_FORMAT.get().format(number);
    }

    /**
     * Parse a number to a fancy format.
     *
     * @param number Number to parse
     * @return Parsed number
     */
    public static String decimalFormat(float number) {
        return FANCY_FORMAT.get().format(number);
    }

    /**
     * Parse a number to a fancy format.
     *
     * @param number Number to parse
     * @return Parsed number
     */
    public static String decimalFormat(short number) {
        return FANCY_FORMAT.get().format(number);
    }

    /**
     * Parse a number to a fancy format.
     *
     * @param number Number to parse
     * @return Parsed number
     */
    public static String decimalFormat(byte number) {
        return FANCY_FORMAT.get().format(number);
    }

    /**
     * Parse a number to a fancy format.
     * @param number Number to parse
     * @return Parsed number
     */
    public static String decimalFormat(BigDecimal number) {
        return FANCY_FORMAT.get().format(number);
    }

    /**
     * Parse a number to a fancy format.
     * @param number Number to parse
     * @return Parsed number
     */
    public static String decimalFormat(BigInteger number) {
        return FANCY_FORMAT.get().format(number);
    }

    /**
     * Parses an integer from a string.
     * @param input String input
     * @return Parsed integer
     */
    public Optional<Integer> parseInteger(String input) {
        try {
            return Optional.of(Integer.parseInt(input));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * Parses a long from a string.
     * @param input String input
     * @return Parsed long
     */
    public Optional<Long> parseLong(String input) {
        try {
            return Optional.of(Long.parseLong(input));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * Parses a double from a string.
     * @param input String input
     * @return Parsed double
     */
    public Optional<Double> parseDouble(String input) {
        try {
            return Optional.of(Double.parseDouble(input));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * Parses a float from a string.
     * @param input String input
     * @return Parsed float
     */
    public Optional<Float> parseFloat(String input) {
        try {
            return Optional.of(Float.parseFloat(input));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * Parses a short from a string.
     * @param input String input
     * @return Parsed short
     */
    public Optional<Short> parseShort(String input) {
        try {
            return Optional.of(Short.parseShort(input));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * Parses a byte from a string.
     * @param input String input
     * @return Parsed byte
     */
    public Optional<Byte> parseByte(String input) {
        try {
            return Optional.of(Byte.parseByte(input));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * This method will convert a number to a fancy version of
     * the provided number such as 1st, 2nd, 3rd, 4th, etc.
     *
     * @param number Number to convert
     * @return Fancy version of the number
     */
    public static String fancy(int number) {
        if (number % 100 >= 11 && number % 100 <= 13) {
            return decimalFormat(number) + "th";
        }

        return switch (number % 10) {
            case 1 -> decimalFormat(number) + "st";
            case 2 -> decimalFormat(number) + "nd";
            case 3 -> decimalFormat(number) + "rd";
            default -> decimalFormat(number) + "th";
        };
    }

    /**
     * This method will convert a number to a fancy version of
     * the provided number such as 1st, 2nd, 3rd, 4th, etc.
     *
     * @param number Number to convert
     * @return Fancy version of the number
     */
    public static String fancy(long number) {
        if (number % 100 >= 11 && number % 100 <= 13) {
            return decimalFormat(number) + "th";
        }

        return switch ((int) (number % 10)) {
            case 1 -> decimalFormat(number) + "st";
            case 2 -> decimalFormat(number) + "nd";
            case 3 -> decimalFormat(number) + "rd";
            default -> decimalFormat(number) + "th";
        };
    }

    /**
     * This method will convert a number to a fancy version of
     * the provided number such as 1st, 2nd, 3rd, 4th, etc.
     *
     * @param number Number to convert
     * @return Fancy version of the number
     */
    public static String fancy(double number) {
        if (number % 100 >= 11 && number % 100 <= 13) {
            return decimalFormat(number) + "th";
        }

        return switch ((int) (number % 10)) {
            case 1 -> decimalFormat(number) + "st";
            case 2 -> decimalFormat(number) + "nd";
            case 3 -> decimalFormat(number) + "rd";
            default -> decimalFormat(number) + "th";
        };
    }

    /**
     * This method will convert a number to a fancy version of
     * the provided number such as 1st, 2nd, 3rd, 4th, etc.
     *
     * @param number Number to convert
     * @return Fancy version of the number
     */
    public static String fancy(float number) {
        if (number % 100 >= 11 && number % 100 <= 13) {
            return number + "th";
        }

        return switch ((int) (number % 10)) {
            case 1 -> decimalFormat(number) + "st";
            case 2 -> decimalFormat(number) + "nd";
            case 3 -> decimalFormat(number) + "rd";
            default -> decimalFormat(number) + "th";
        };
    }

    /**
     * This method will convert a number to a fancy version of
     * the provided number such as 1st, 2nd, 3rd, 4th, etc.
     *
     * @param number Number to convert
     * @return Fancy version of the number
     */
    public static String fancy(short number) {
        if (number % 100 >= 11 && number % 100 <= 13) {
            return decimalFormat(number) + "th";
        }

        return switch (number % 10) {
            case 1 -> decimalFormat(number) + "st";
            case 2 -> decimalFormat(number) + "nd";
            case 3 -> decimalFormat(number) + "rd";
            default -> decimalFormat(number) + "th";
        };
    }

    /**
     * This method will convert a number to a fancy version of
     * the provided number such as 1st, 2nd, 3rd, 4th, etc.
     *
     * @param number Number to convert
     * @return Fancy version of the number
     */
    public static String fancy(byte number) {
        if (number % 100 >= 11 && number % 100 <= 13) {
            return decimalFormat(number) + "th";
        }

        return switch (number % 10) {
            case 1 -> decimalFormat(number) + "st";
            case 2 -> decimalFormat(number) + "nd";
            case 3 -> decimalFormat(number) + "rd";
            default -> decimalFormat(number) + "th";
        };
    }

    /**
     * Condenses a number into a shorter version using suffixes.
     * @param number Number to condense
     * @return Condensed number
     */
    public static String condense(int number) {
        return condense(number, null);
    }

    /**
     * Condenses a number into a shorter version using suffixes.
     * @param number Number to condense
     * @param set Set of suffixes to use
     * @return Condensed number
     */
    public static String condense(int number, final char[] set) {
        if (number < 1000) return String.valueOf(number); // Return the number itself if less than 1000.

        int exp = (int) (Math.log(number) / Math.log(1000));

        String suffixes = (set == null) ? SUFFIXES : new String(set);
        char suffix = suffixes.charAt(Math.min(exp - 1, suffixes.length() - 1));

        return String.format("%.1f%c", number / Math.pow(1000, exp), suffix);
    }

    /**
     * Condenses a number into a shorter version using suffixes.
     * @param number Number to condense
     * @return Condensed number
     */
    public static String condense(double number) {
        return condense(number, null);
    }

    /**
     * Condenses a number into a shorter version using suffixes.
     * @param number Number to condense
     * @param set Set of suffixes to use
     * @return Condensed number
     */
    public static String condense(double number, final char[] set) {
        if (number < 1000) return String.valueOf(number); // Return the number itself if less than 1000.

        int exp = (int) (Math.log(number) / Math.log(1000));

        String suffixes = (set == null) ? SUFFIXES : new String(set);
        char suffix = suffixes.charAt(Math.min(exp - 1, suffixes.length() - 1));

        return String.format("%.1f%c", number / Math.pow(1000, exp), suffix);
    }

    /**
     * Condenses a number into a shorter version using suffixes.
     * @param number Number to condense
     * @return Condensed number
     */
    public static String condense(long number) {
        return condense(number, null);
    }

    /**
     * Condenses a number into a shorter version using suffixes.
     * @param number Number to condense
     * @param set Set of suffixes to use
     * @return Condensed number
     */
    public static String condense(long number, final char[] set) {
        if (number < 1000) return String.valueOf(number); // Return the number itself if less than 1000.

        int exp = (int) (Math.log(number) / Math.log(1000));

        String suffixes = (set == null) ? SUFFIXES : new String(set);
        char suffix = suffixes.charAt(Math.min(exp - 1, suffixes.length() - 1));

        return String.format("%.1f%c", number / Math.pow(1000, exp), suffix);
    }

    /**
     * Condenses a number into a shorter version using suffixes.
     * @param number Number to condense
     * @return Condensed number
     */
    public static String condense(BigDecimal number) {
        return condense(number, null);
    }

    /**
     * Condenses a number into a shorter version using suffixes.
     * @param number Number to condense
     * @param set Set of suffixes to use
     * @return Condensed number
     */
    public static String condense(BigDecimal number, final char[] set) {
        BigDecimal thousand = BigDecimal.valueOf(1000);
        String condensed;
        if (number.compareTo(thousand) < 0) {
            condensed = number.stripTrailingZeros().toPlainString(); // Return the number itself if less than 1000.
        } else {
            int exp = (int) (Math.floor(Math.log10(number.doubleValue()) / 3));

            String suffixes = (set == null) ? SUFFIXES : new String(set);
            char suffix = suffixes.charAt(Math.min(exp - 1, suffixes.length() - 1));

            BigDecimal result = number.divide(thousand.pow(exp), 1, RoundingMode.HALF_UP);
            condensed = String.format("%.1f%c", result, suffix);
        }
        return condensed;
    }

    /**
     * Condenses a number into a shorter version using suffixes.
     * @param number Number to condense
     * @return Condensed number
     */
    public static String condense(BigInteger number) {
        return condense(number, null);
    }

    /**
     * Condenses a number into a shorter version using suffixes.
     * @param number Number to condense
     * @param set Set of suffixes to use
     * @return Condensed number
     */
    public static String condense(BigInteger number, final char[] set) {
        return condense(new BigDecimal(number), set);
    }

}
