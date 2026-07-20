package com.cms.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * All interactive console input funnels through here so a stray letter typed
 * into a "number field" (or any other bad input) never crashes the app.
 */
public final class InputValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[\\w.-]+$");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private InputValidator() {
    }

    public static String readNonEmptyString(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = sc.nextLine().trim();
            if (!line.isEmpty()) {
                return line;
            }
            System.out.println("  Input cannot be empty. Please try again.");
        }
    }

    public static String readEmail(Scanner sc, String prompt) {
        while (true) {
            String value = readNonEmptyString(sc, prompt);
            if (EMAIL_PATTERN.matcher(value).matches()) {
                return value;
            }
            System.out.println("  That doesn't look like a valid email address. Please try again.");
        }
    }

    public static int readInt(Scanner sc, String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String line = sc.nextLine().trim();
            try {
                int value = Integer.parseInt(line);
                if (value < min || value > max) {
                    System.out.printf("  Please enter a number between %d and %d.%n", min, max);
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                System.out.println("  That's not a valid whole number. Please try again.");
            }
        }
    }

    public static LocalDate readDate(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt + " (yyyy-MM-dd): ");
            String line = sc.nextLine().trim();
            try {
                return LocalDate.parse(line, DATE_FMT);
            } catch (Exception e) {
                System.out.println("  Invalid date format. Please use yyyy-MM-dd (e.g. 2026-01-31).");
            }
        }
    }

    public static <T extends Enum<T>> T readEnum(Scanner sc, String prompt, Class<T> enumClass) {
        T[] constants = enumClass.getEnumConstants();
        while (true) {
            StringBuilder options = new StringBuilder();
            for (int i = 0; i < constants.length; i++) {
                options.append(i + 1).append(") ").append(constants[i].name());
                if (i < constants.length - 1) options.append("  ");
            }
            System.out.println("  " + options);
            int choice = readInt(sc, prompt, 1, constants.length);
            return constants[choice - 1];
        }
    }
}
