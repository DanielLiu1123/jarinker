package jarinker.cli.util;

import java.io.PrintStream;

/**
 * @author Freeman
 * @since 2024/10/17
 */
public final class Printer {

    private static final PrintStream out = System.out;

    public static void print(String message) {
        out.println(message);
    }
}
