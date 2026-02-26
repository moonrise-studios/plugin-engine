package games.negative.engine.util;

import java.util.ArrayList;
import java.util.List;

public class IntList {

    /**
     * Parses a list of strings into a list of integers.
     * Strings can represent single integers or ranges (e.g., "5-10").
     *
     * @param strings The list of strings to parse.
     * @return A list of integers.
     */
    public static List<Integer> parse(List<String> strings) {
        List<Integer> list = new ArrayList<>();

        for (String string : strings) {
            if (string.contains("-")) {
                String[] range = string.split("-", 2);
                int start = Integer.parseInt(range[0].trim());
                int end = Integer.parseInt(range[1].trim());

                for (int i = start; i <= end; i++) {
                    list.add(i);
                }
            } else {
                list.add(Integer.parseInt(string.trim()));
            }
        }

        return list;
    }
}